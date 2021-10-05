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

import de.kp.works.stream.sql.jdbc.JdbcUtil
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import java.sql.{Connection, JDBCType, PreparedStatement, Statement}
import java.util.Properties

object RedshiftUtil extends JdbcUtil {

  /** JDBC SUPPORT * */

  /**
   * Compute the Redshift SQL schema string for the given Spark SQL Schema.
   */
  def buildSqlSchema(schema: StructType, options:RedshiftOptions): String = {

    var sqlSchema = schema.fields.map { field => {

      var fname = field.name
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

      fname = fname.replace("\"", "\\\"")
      s"""$fname $ftype $nullable $encoding""".trim

    }}.mkString(", ")

    if (options.getPrimaryKey.isEmpty) return sqlSchema
    val primaryKey = options.getPrimaryKey.get
      .replace("\"", "\\\"")

    sqlSchema = sqlSchema + s""", PRIMARY KEY($primaryKey)"""
    sqlSchema

  }

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
   * The INSERT SQL statement is built from the provided
   * schema specification as the Redshift stream writer
   * ensures that table schema and provided schema are
   * identical
   */
  def createInsertSql(schema:StructType, options:RedshiftOptions): String = {

    val table = options.getTable

    val columns = schema.fields
      .map(field =>
        field.name.replace("\"", "\\\""))
      .mkString(",")

    val values = schema.fields.map(_ => "?").mkString(",")
    val insertSql = s"""INSERT INTO $table ($columns) VALUES($values)"""

    insertSql

  }
  /**
   * Generate CREATE TABLE statement for Redshift
   */
  def createTableSql(schema: StructType, options: RedshiftOptions): String = {
    /*
     * STEP #1: Build the Redshift compliant SQL schema
     * from the provided schema
     */
    val sqlSchema = buildSqlSchema(schema, options)
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

    s"""CREATE TABLE IF NOT EXISTS $table ($sqlSchema) $distStyle $distKey $sortKey"""

  }

  def getConnection(options: RedshiftOptions): Connection = {

    val driver = getDriver(options.getJdbcDriver)
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

  override def getDriverClassName(jdbcDriverName: String): String = {
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

  def insertValue(conn:Connection, stmt:PreparedStatement, row:Row, pos:Int, dataType:DataType):Unit = {

    dataType match {
      /*
       * Primitive data types
       */
      case BinaryType =>
        stmt.setBytes(pos + 1, row.getAs[Array[Byte]](pos))
      case BooleanType =>
        stmt.setBoolean(pos + 1, row.getBoolean(pos))
      case ByteType =>
        stmt.setByte(pos + 1, row.getByte(pos))
      case DateType =>
        stmt.setDate(pos + 1, row.getAs[java.sql.Date](pos))
      case _: DecimalType =>
        stmt.setBigDecimal(pos + 1, row.getDecimal(pos))
      case DoubleType =>
        stmt.setDouble(pos + 1, row.getDouble(pos))
      case FloatType =>
        stmt.setFloat(pos + 1, row.getFloat(pos))
      case IntegerType =>
        stmt.setInt(pos + 1, row.getInt(pos))
      case LongType =>
        stmt.setLong(pos + 1, row.getLong(pos))
      case ShortType =>
        stmt.setInt(pos + 1, row.getInt(pos))
      case StringType =>
        stmt.setString(pos + 1, row.getString(pos))
      case TimestampType =>
        stmt.setTimestamp(pos + 1, row.getAs[java.sql.Timestamp](pos))
      /*
       * Complex data types
       */
      case ArrayType(ct, _) =>
        val values = row.getSeq[AnyRef](pos).toArray
        val typeName = ct match {
          case BooleanType =>
            JDBCType.BOOLEAN.getName
          case ByteType =>
            JDBCType.TINYINT.getName
          case DateType =>
            JDBCType.DATE.getName
          case _:DecimalType =>
            JDBCType.DECIMAL.getName
          case DoubleType =>
            JDBCType.DOUBLE.getName
          case FloatType =>
            JDBCType.FLOAT.getName
          case IntegerType =>
            JDBCType.INTEGER.getName
          case LongType =>
            JDBCType.BIGINT.getName
          case StringType =>
            JDBCType.VARCHAR.getName
          case TimestampType =>
            JDBCType.TIMESTAMP.getName
          case _ =>
            throw new Exception(s"Component type `${ct.simpleString}` is not supported.")
        }
        val array = conn.createArrayOf(typeName, values)
        stmt.setArray(pos + 1, array)

      case _ =>
        throw new Exception(s"Data type `${dataType.simpleString}` is not supported.")
    }

  }
}
