package de.kp.works.stream.sql.postgres

/*
 * Copyright (c) 2020 - 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

import java.sql.{Connection, PreparedStatement, Statement}
import java.util.Properties

object PostgresUtil extends JdbcUtil {

  def getBasicTypeName(dataType:DataType):Option[String] = {

    dataType match {
      case BinaryType      => Some("BYTEA")
      case BooleanType     => Some("BOOLEAN")
      case ByteType        => Some("SMALLINT")
      case DateType        => Some("DATE")
      case dt: DecimalType => Some(s"NUMERIC(${dt.precision},${dt.scale})")
      case DoubleType      => Some("DOUBLE PRECISION")
      case FloatType       => Some("REAL")
      case IntegerType     => Some("INTEGER")
      case LongType        => Some("BIGINT")
      case ShortType       => Some("SMALLINT")
      case StringType      => Some("TEXT")
      case TimestampType   => Some("TIMESTAMP")
      case _ => None
    }

  }
  /**
   * Compute the Postgres SQL schema string for the
   * given Spark SQL Schema.
   */
  def buildSqlSchema(schema: StructType, options:PostgresOptions): String = {

    var sqlSchema = schema.fields.map { field => {

      val fname = field.name
      val ftype = field.dataType match {
        case ArrayType(dt, _) =>
          val btype = getBasicTypeName(dt)
          if (btype.isEmpty)
            throw new IllegalArgumentException(s"Don't know how to save $field to JDBC")

          else
            s"${btype.get}[]"
        case dt =>
          val btype = getBasicTypeName(dt)
          if (btype.isEmpty)
            throw new IllegalArgumentException(s"Don't know how to save $field to JDBC")

          else
            s"${btype.get}"
      }

      val nullable = if (field.nullable) "" else "NOT NULL"
      s"""$fname $ftype $nullable"""

    }}.mkString(", ")

    if (options.getPrimaryKey.isEmpty) return sqlSchema
    val primaryKey = options.getPrimaryKey.get

    sqlSchema = sqlSchema + s""", PRIMARY KEY($primaryKey)"""
    sqlSchema

  }

  def createTableIfNotExist(conn: Connection, schema: StructType, options: PostgresOptions): Boolean = {

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
   * Generate CREATE TABLE statement for Postgres
   */
  def createTableSql(schema: StructType, options: PostgresOptions): String = {

    val table = options.getTable

    val sqlSchema = buildSqlSchema(schema, options)
    s"""CREATE TABLE IF NOT EXISTS $table ($sqlSchema)"""

  }

  /**
   * The UPSERT SQL statement is built from the provided
   * schema specification as the Postgres stream writer
   * ensures that table schema and provided schema are
   * identical
   */
  def createUpsertSql(schema:StructType, options:PostgresOptions): String = ???

  def getConnection(options: PostgresOptions): Connection = {

    val driver = getDriver(options.getJdbcDriver)

    val url = s"jdbc:postgresql://${options.getDatabaseUrl}"
    val jdbcProperties = new Properties()
    /*
     * User authentication
     */
    val (user, pass) = options.getUserAndPass

    jdbcProperties.setProperty("user", user)
    jdbcProperties.setProperty("password", pass)

    driver.connect(url, jdbcProperties)

  }

  override def getDriverClassName(jdbcDriverName: String): String = {

    jdbcDriverName match {
      case "org.postgresql.Driver" =>
        classForName(jdbcDriverName).getName
      case _ =>
        classForName(POSTGRES_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME).getName

    }

  }

  /**
   * This method inserts a stream (column) value into the
   * provided prepared statement, controlled by the Spark
   * data type
   */
  def upsertValue(conn:Connection, stmt:PreparedStatement, row:Row, pos:Int, dataType:DataType):Unit = ???

}
