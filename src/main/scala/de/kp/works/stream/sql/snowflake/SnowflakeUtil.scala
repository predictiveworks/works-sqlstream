package de.kp.works.stream.sql.snowflake
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
import org.apache.spark.sql.types._

import java.sql.{Connection, Statement}
import java.util.Properties

object SnowflakeUtil extends JdbcUtil {
  /**
   * Compute the Snowflake SQL schema string for the
   * given Spark SQL Schema.
   */
  def buildSqlSchema(schema: StructType, options:SnowflakeOptions): String = {

    var sqlSchema = schema.fields.map { field => {

      val fname = ensureQuoted(field.name)
      /*
       * Retrieve corresponding Snowflake data type
       * giving a Spark SQL data type
       */
      val ftype = field.dataType match {
        /*
         * Primitive data types
         */
        case BinaryType     =>
          if (field.metadata.contains("maxlength")) {
            s"BINARY(${field.metadata.getLong("maxlength")})"
          } else {
            "BINARY"
          }
        case BooleanType    => "BOOLEAN"
        case ByteType       => "INTEGER" // Snowflake does not support the BYTE type.
        case DateType       => "DATE"
        case t: DecimalType => s"DECIMAL(${t.precision},${t.scale})"
        case DoubleType     => "DOUBLE"
        case FloatType      => "FLOAT"
        case IntegerType    => "INTEGER"
        case LongType       => "INTEGER"
        case ShortType      => "INTEGER"
        case StringType     =>
          if (field.metadata.contains("maxlength")) {
            s"VARCHAR(${field.metadata.getLong("maxlength")})"
          } else {
            "STRING"
          }
        case TimestampType  => "TIMESTAMP"
        /*
         * Complex data types
         */
        case _: ArrayType   => "VARIANT"
        case _: MapType     => "VARIANT"
        case _: StructType  => "VARIANT"
        case _ =>
          throw new IllegalArgumentException(
            s"Don't know how to save $field of type ${field.name} to Snowflake")
      }

      val nullable = if (field.nullable) "" else "NOT NULL"
      s"""$fname $ftype $nullable"""

    }}.mkString(", ")

    if (options.getPrimaryKey.isEmpty) return sqlSchema
    val primaryKey = ensureQuoted(options.getPrimaryKey.get)

    sqlSchema = sqlSchema + s", PRIMARY KEY($primaryKey)"
    sqlSchema

  }
  /**
   * Create a `table` in a Snowflake database with respect
   * to the provided (DataFrame) schema, if the table does
   * not exist
   */
  def createTableIfNotExist(conn: Connection, schema: StructType, options: SnowflakeOptions): Boolean = {

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
   * Generate CREATE TABLE statement for Snowflake
   */
  def createTableSql(schema: StructType, options: SnowflakeOptions): String = {

    val table = options.getTable
    /*
     * Build the Snowflake compliant SQL schema
     * from the provided schema
     */
    val sqlSchema = buildSqlSchema(schema, options)
    s"CREATE TABLE IF NOT EXISTS $table ($sqlSchema)"

  }
  /**
   * The INSERT SQL statement is built from the provided
   * schema specification as the Snowflake stream writer
   * ensures that table schema and provided schema are
   * identical
   */
  def createInsertSql(schema:StructType, options:SnowflakeOptions): String = {

    val table = options.getTable

    val columns = schema.fields
      .map(field =>
        ensureQuoted(field.name))
      .mkString(",")

    val values = schema.fields.map(_ => "?").mkString(",")
    val insertSql = s"INSERT INTO $table ($columns) VALUES($values)"

    insertSql

  }

  override def getDriverClassName(jdbcDriverName:String): String = {
    jdbcDriverName match {
      case "net.snowflake.client.jdbc.SnowflakeDriver" =>
        classForName(jdbcDriverName).getName
      case _ =>
        classForName(SNOWFLAKE_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME).getName

    }
  }

  def getConnection(options:SnowflakeOptions): Connection = {
    /*
     * Build Snowflake JDBC driver and extract connection
     * url from provided configuration
     */
    val driver = getDriver(options.getJdbcDriver)
    val url = s"jdbc:snowflake://${options.getDatabaseUrl}"

    val jdbcProperties = new Properties()

    /* Mandatory parameters */

    jdbcProperties.put("account", options.getAccount)
    jdbcProperties.put("db",      options.getDatabase)
    jdbcProperties.put("schema",  options.getSchema)
    jdbcProperties.put("user",    options.getUser)

    /* Always set CLIENT_SESSION_KEEP_ALIVE */

    jdbcProperties.put("client_session_keep_alive", "true")

    /* Force DECIMAL for NUMBER (SNOW-33227) */
    jdbcProperties.put("JDBC_TREAT_DECIMAL_AS_INT", "false")
    /*
     * IMPORTANT: Set client_info is very important
     *
     * SFSessionProperty.CLIENT_INFO.getPropertyKey
     */
    jdbcProperties.put("snowflakeClientInfo",
      options.getClientInfoString)

    /* User authentication */

    val authToken  = options.getToken
    val password   = options.getPassword
    val privateKey = options.getPrivateKey

    privateKey match {
      case Some(value) =>
        jdbcProperties.put("privateKey", value)
      case None =>
        authToken match {
          case Some(value) =>
            jdbcProperties.put("token", value)
          case None => jdbcProperties.put("password", password.get)
        }
    }

    jdbcProperties.put("authenticator", options.getAuthenticator)
    jdbcProperties.put("ssl",           options.getSsl)

    /* Optional parameters */

    val role = options.getRole
    if (role.isDefined) {
      jdbcProperties.put("role", role.get)
    }

    val warehouse = options.getWarehouse
    if (warehouse.isDefined) {
      jdbcProperties.put("warehouse", warehouse.get)
    }

    /**
     * CURRENT IMPLEMENTATION RESTRICTIONS:
     *
     * (1) No timeout format is supported
     *
     * (2) No query result format is supported; therefore, there is
     * no (additional) need to setup the query format with an extra
     * JDBC statement
     *
     * "alter session set JDBC_QUERY_RESULT_FORMAT = '$resultFormat'"
     *
     * (3) No JDBC proxy is supported
     */

     driver.connect(url, jdbcProperties)

  }

  /** HELPER METHODS **/

  /**
   * This method check whether a name is quoted
   */
  private def isQuoted(name: String): Boolean = {
    name.startsWith("\"") && name.endsWith("\"")
  }
  /**
   * This method wraps a name with double quotes
   */
  private def ensureQuoted(name: String): String = {
    if (isQuoted(name)) name
    else {
      /*
       * If the input identifier is legal, uppercase
       * before wrapping it with double quotes
       */
      if (name.matches("[_a-zA-Z]([_0-9a-zA-Z])*")) {
        "\"" + name.toUpperCase + "\""

      } else {
        "\"" + name + "\""
      }

    }

  }

}
