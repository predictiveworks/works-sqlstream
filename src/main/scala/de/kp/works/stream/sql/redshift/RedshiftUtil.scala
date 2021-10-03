package de.kp.works.stream.sql.redshift
/*
 * Copyright (c) 2020 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author Stefan Krusche, Dr. Krusche & Partner PartG
 *
 */

import org.apache.spark.sql.execution.datasources.jdbc.DriverRegistry
import org.apache.spark.sql.types._

import java.sql.{Connection, Driver, DriverManager, ResultSet, ResultSetMetaData, SQLException, Statement}
import java.util.Properties
import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
import scala.collection.mutable

object RedshiftUtil {

  def getDriverClassName(jdbcDriverName: String): String = {
    jdbcDriverName match {
      case "com.amazon.redshift.jdbc42.Driver" =>
        classForName(jdbcDriverName).getName
      case "com.amazon.redshift.jdbc41.Driver" =>
        classForName(jdbcDriverName).getName
      case "com.amazon.redshift.jdbc4.Driver" =>
        classForName(jdbcDriverName).getName
      case _ =>
        classForName(REDSHIFT_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME).getName

    }
  }

  def classForName(className: String): Class[_] = {
    val classLoader =
      Option(Thread.currentThread().getContextClassLoader).getOrElse(this.getClass.getClassLoader)

    Class.forName(className, true, classLoader)
  }

  /** JDBC SUPPORT * */

  def createTableIfNotExist(conn: Connection, schema: StructType, options: RedshiftOptions): Boolean = {

    val createSql = createTableSql(schema, options)

    var stmt: Statement = null
    var success: Boolean = false

    try {

      conn.setAutoCommit(false)

      stmt = conn.createStatement()
      stmt.execute(createSql)

      conn.commit()
      success = true

    } catch {
      case _: Throwable => /* Do nothing */

    } finally {

      if (stmt != null)
        try {
          stmt.close()

        } catch {
          case _: Throwable => /* Do nothing */
        }
    }

    success

  }

  /**
   * Generate CREATE TABLE statement for Redshift
   */
  def createTableSql(schema: StructType, options: RedshiftOptions): String = {
    /*
     * STEP #1: Build the Redshift compliant SQL schema
     * from the provided schema
     */
    val sqlSchema = buildSqlSchema(schema)
    /*
     * STEP #2: Add default and user specific parameters
     */
    val distStyle = s"DISTSTYLE ${options.getDistStyle}"

    val distKey = options.getDistKey match {
      case Some(key) => s"DISTKEY ($key)"
      case None => ""
    }

    val sortKey = options.getSortKey.getOrElse("")
    val table = options.getTable

    s"CREATE TABLE IF NOT EXISTS $table ($sqlSchema) $distStyle $distKey $sortKey"

  }

  def getConnection(options: RedshiftOptions): Connection = {

    val driverClassName = getDriverClassName(options.getJdbcDriver)
    DriverRegistry.register(driverClassName)

    val driverWrapperClass: Class[_] =
      classForName("org.apache.spark.sql.execution.datasources.jdbc.DriverWrapper")

    def getWrapped(d: Driver): Driver = {

      require(driverWrapperClass.isAssignableFrom(d.getClass))
      driverWrapperClass.getDeclaredMethod("wrapped").invoke(d).asInstanceOf[Driver]

    }

    /*
     * Note that we purposely don't call #DriverManager.getConnection() here:
     * we want to ensure that an explicitly-specified user-provided driver
     * class can take precedence over the default class, but
     *
     *                 #DriverManager.getConnection()
     *
     * might return a according to a different precedence. At the same time,
     * we don't want to create a driver-per-connection, so we use the DriverManager's
     * driver instances to handle that singleton logic for us.
     */
    val driver: Driver = DriverManager.getDrivers.asScala
      .collectFirst {
        case d if driverWrapperClass.isAssignableFrom(d.getClass)
          && getWrapped(d).getClass.getCanonicalName == driverClassName => d
        case d if d.getClass.getCanonicalName == driverClassName => d
      }.getOrElse {
      throw new IllegalArgumentException(
        s"Did not find registered Redshift driver with class $driverClassName")
    }

    val url = s"jdbc:redshift://${options.getDatabaseUrl}"

    /* User authentication */

    val (user, pass) = options.getUserAndPass

    val authProps = new Properties()
    if (user.isDefined && pass.isDefined) {

      authProps.setProperty("user", user.get)
      authProps.setProperty("password", pass.get)

    }

    driver.connect(url, authProps)

  }

  /**
   * This method checks whether the provided Redshift
   * table exists and if, returns the column metadata
   * representation to merge with the Spark SQL schema
   */
  def getColumnTypes(conn:Connection, options:RedshiftOptions): Seq[(Int, StructField)] = {

    var stmt: Statement = null
    val columnTypes = mutable.ArrayBuffer.empty[(Int, StructField)]

    try {

      stmt = conn.createStatement()
      /*
       * Run a query against the database table that returns 0 records,
       * but returns valid ResultSetMetadata that can be used to optimize
       * write requests to the Redshift database
       */
      val rs: ResultSet = stmt.executeQuery(s"SELECT * FROM ${options.getTable} WHERE 1 = 0")
      val rsMetadata = rs.getMetaData

      val columnCount = rsMetadata.getColumnCount
      (0 until columnCount).foreach(i => {
        /*
         * Extract JDBC representation
         */
        val columnName = rsMetadata.getColumnLabel(i + 1)

        val columnType = rsMetadata.getColumnType(i + 1)
        val precision  = rsMetadata.getPrecision(i + 1)

        val scale = rsMetadata.getScale(i + 1)
        val isSigned = rsMetadata.isSigned(i + 1)

        val nullable = rsMetadata.isNullable(i + 1) != ResultSetMetaData.columnNoNulls
        /*
         * Transform into Spark SQL data type
         */
        val dataType = getDataType(columnType, precision, scale, isSigned)
        val field = StructField(columnName, dataType, nullable)

        columnTypes += (columnType, field)

      })

    } catch {
      case _: Throwable => /* Do nothing */


    } finally {

      if (stmt != null)
        try {
          stmt.close()

        } catch {
          case _: Throwable => /* Do nothing */
        }

    }

    columnTypes

  }

  /**
   * Compute the Redshift SQL schema string for the given Spark SQL Schema.
   */
  def buildSqlSchema(schema: StructType): String = {

    val sb = new StringBuilder()
    schema.fields.foreach { field => {

      val fname = field.name
      val ftype = field.dataType match {
        /*
         * The current implementation supports primitive data types
         */
        case BooleanType => "BOOLEAN"
        case ByteType => "SMALLINT" // Redshift does not support the BYTE type.
        case DateType => "DATE"
        case t: DecimalType => s"DECIMAL(${t.precision},${t.scale})"
        case DoubleType => "DOUBLE PRECISION"
        case FloatType => "REAL"
        case IntegerType => "INTEGER"
        case LongType => "BIGINT"
        case ShortType => "INTEGER"
        case StringType =>
          if (field.metadata.contains("maxlength")) {
            s"VARCHAR(${field.metadata.getLong("maxlength")})"
          } else {
            "TEXT"
          }
        case TimestampType => "TIMESTAMP"
        case _ => throw new IllegalArgumentException(s"Don't know how to save $field to JDBC")
      }

      val nullable = if (field.nullable) "" else "NOT NULL"
      val encoding = if (field.metadata.contains("encoding")) {
        s"ENCODE ${field.metadata.getString("encoding")}"
      } else {
        ""
      }

      sb.append(s""", "${fname.replace("\"", "\\\"")}" $ftype $nullable $encoding""".trim)

    }
    }

    if (sb.length < 2) "" else sb.substring(2)

  }

  /**
   * This method maps a JDBC type to a Spark SQL [DataType].
   *
   * Note, this method is not restricted to Redshift JDBC
   * data types, which specify a subset
   */
  def getDataType(sqlType: Int, precision: Int, scale: Int, signed: Boolean): DataType = {

    val dataType = sqlType match {
      case java.sql.Types.ARRAY => null
      case java.sql.Types.BIGINT => if (signed) {
        LongType
      } else {
        DecimalType(20, 0)
      }
      case java.sql.Types.BINARY => BinaryType
      case java.sql.Types.BIT => BooleanType
      case java.sql.Types.BLOB => BinaryType
      case java.sql.Types.BOOLEAN => BooleanType
      case java.sql.Types.CHAR => StringType
      case java.sql.Types.CLOB => StringType
      case java.sql.Types.DATALINK => null
      case java.sql.Types.DATE => DateType
      case java.sql.Types.DECIMAL
        if precision != 0 || scale != 0 => DecimalType(precision, scale)
      case java.sql.Types.DECIMAL => DecimalType(10, 0)
      case java.sql.Types.DISTINCT => null
      case java.sql.Types.DOUBLE => DoubleType
      case java.sql.Types.FLOAT => FloatType
      case java.sql.Types.INTEGER => if (signed) {
        IntegerType
      } else {
        LongType
      }
      case java.sql.Types.JAVA_OBJECT => null
      case java.sql.Types.LONGNVARCHAR => StringType
      case java.sql.Types.LONGVARBINARY => BinaryType
      case java.sql.Types.LONGVARCHAR => StringType
      case java.sql.Types.NCHAR => StringType
      case java.sql.Types.NCLOB => StringType
      case java.sql.Types.NULL => null
      case java.sql.Types.NUMERIC
        if precision != 0 || scale != 0 => DecimalType(precision, scale)
      case java.sql.Types.NUMERIC => DecimalType(10, 0)
      case java.sql.Types.NVARCHAR => StringType
      case java.sql.Types.OTHER => null
      case java.sql.Types.REAL => DoubleType
      case java.sql.Types.REF => StringType
      case java.sql.Types.ROWID => LongType
      case java.sql.Types.SMALLINT => IntegerType
      case java.sql.Types.SQLXML => StringType
      case java.sql.Types.STRUCT => StringType
      case java.sql.Types.TIME => TimestampType
      case java.sql.Types.TIMESTAMP => TimestampType
      case java.sql.Types.TINYINT => IntegerType
      case java.sql.Types.VARBINARY => BinaryType
      case java.sql.Types.VARCHAR => StringType
      case _ => null
    }

    if (dataType == null)
      throw new SQLException("Unsupported type " + sqlType)

    dataType

  }
}
