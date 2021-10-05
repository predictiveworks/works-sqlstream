package de.kp.works.stream.sql.jdbc
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

import java.sql._
import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
import scala.collection.mutable

trait JdbcUtil {

  def classForName(className: String): Class[_] = {
    val classLoader =
      Option(Thread.currentThread().getContextClassLoader).getOrElse(this.getClass.getClassLoader)

    Class.forName(className, true, classLoader)
  }

  def getDriverClassName(jdbcDriverName:String): String

  def getDriver(jdbcDriverName:String):Driver = {

    val driverClassName = getDriverClassName(jdbcDriverName)
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

    driver

  }

  /**
   * This method checks whether the provided JDBC database
   * table exists and if, returns the column metadata
   * representation to merge with the Spark SQL schema
   */
  def getColumnTypes(conn:Connection, table:String): Seq[(Int, StructField)] = {

    var stmt: Statement = null
    val columnTypes = mutable.ArrayBuffer.empty[(Int, StructField)]

    try {

      stmt = conn.createStatement()
      /*
       * Run a query against the database table that returns 0 records,
       * but returns valid ResultSetMetadata that can be used to optimize
       * write requests to the Redshift database
       */
      val rs: ResultSet = stmt.executeQuery(s"""SELECT * FROM $table WHERE 1 = 0""")
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

        columnTypes += ((columnType, field))

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
   * This method maps a JDBC type to a Spark SQL [DataType].
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
      case java.sql.Types.DECIMAL => DecimalType(20, 0)
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
      case java.sql.Types.NUMERIC => DecimalType(20, 0)
      case java.sql.Types.NVARCHAR => StringType
      case java.sql.Types.OTHER => null
      case java.sql.Types.REAL => FloatType
      case java.sql.Types.REF => StringType
      case java.sql.Types.ROWID => LongType
      case java.sql.Types.SMALLINT => ShortType
      case java.sql.Types.SQLXML => StringType
      case java.sql.Types.STRUCT => StringType
      case java.sql.Types.TIME => TimestampType
      case java.sql.Types.TIMESTAMP => TimestampType
      case java.sql.Types.TINYINT => ShortType
      case java.sql.Types.VARBINARY => BinaryType
      case java.sql.Types.VARCHAR => StringType
      case _ => null
    }

    if (dataType == null)
      throw new SQLException("Unsupported type " + sqlType)

    dataType

  }

}
