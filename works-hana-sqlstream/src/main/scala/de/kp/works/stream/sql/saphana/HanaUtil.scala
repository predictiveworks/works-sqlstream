package de.kp.works.stream.sql.saphana

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

import java.sql.{Connection, PreparedStatement, Statement}
import java.util.Properties
import scala.util.Try

object HanaUtil extends JdbcUtil {
  /**
   * Compute the SAP HANA SQL schema string for the
   * given Spark SQL Schema.
   */
  def buildSqlSchema(schema: StructType, options:HanaOptions): String = {

    val sqlSchema = schema.fields.map { field => {

      val fname = field.name
      val ftype = field.dataType match {
        case BinaryType     =>
          if (field.metadata.contains("maxlength")) {
            s"VARBINARY(${field.metadata.getLong("maxlength")})"
          } else {
            "VARBINARY(8192)"
          }
        case BooleanType    => "BOOLEAN"
        case ByteType       => "TINYINT" // SAP HANA does not support the BYTE type.
        case t: DecimalType => s"DECIMAL(${t.precision},${t.scale})"
        case DateType       => "DATE"
        case DoubleType     => "DOUBLE"
        case FloatType      => "REAL"
        case IntegerType    => "INTEGER"
        case LongType       => "BIGINT"
        case ShortType      => "SMALLINT"
        case StringType     =>
          if (field.metadata.contains("maxlength")) {
            s"VARCHAR(${field.metadata.getLong("maxlength")})"
          } else {
            "TEXT"
          }
        case TimestampType  => "TIMESTAMP"
        case _ => throw new IllegalArgumentException(s"Don't know how to save $field to JDBC")
      }

      val nullable = if (field.nullable) "" else "NOT NULL"
      s""""$fname" $ftype $nullable"""

    }}.mkString(", ")
    sqlSchema

  }

  /**
   * The INSERT SQL statement is built from the provided
   * schema specification as the SAP HANA stream writer
   * ensures that table schema and provided schema are
   * identical
   */
  def createInsertSql(schema:StructType, options:HanaOptions): String = {

    val table = options.getTable

    val columns = schema.fields
      .map(field => s""""${field.name}"""")
      .mkString(",")

    val values = schema.fields.map(_ => "?").mkString(",")
    val insertSql = s"""INSERT INTO $table ($columns) VALUES($values)"""

    insertSql

  }

  def createTableIfNotExist(conn: Connection, schema: StructType, options: HanaOptions): Boolean = {
    /*
     * Explicitly check whether the configured SAP HANA
     * database table exists, as SAP HANA does not support
     * `if not exists` query clause
     */
    val exists = tableExists(conn, options)
    if (exists) return true
    /*
     * Build the SAP HANA compliant CREATE TABLE statement
     */
    val createSql = createTableSql(schema, options)
    /*
     * Finally create the configured database table
     */
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
   * Generate CREATE TABLE statement for SAP HANA
   */
  def createTableSql(schema: StructType, options: HanaOptions): String = {

    val table = options.getTable
    val sqlSchema = buildSqlSchema(schema, options)

    val keyName = options.getPrimaryKey
    val partitionType = options.getPartition

    /* Primary key support */

    var primaryKey = ""
    if (keyName.isDefined && keyName.get.nonEmpty)
      primaryKey = ", PRIMARY KEY (\"" + keyName + "\")"

    /* Partition support */

    var partition = ""
    if (partitionType.isDefined) {
      /*
       * Evaluate provided partition type: The number of partitions is determined
       * by the database at runtime according to its configuration.
       *
       * It is recommended by SAP to use this function in scripts.
       */
      if (partitionType.get == HANA_STREAM_SETTINGS.HASH_PARTITION) {
        if (keyName.isDefined && keyName.get.nonEmpty) {
          partition = " PARTITION BY HASH(\"" + keyName + "\") PARTITIONS GET_NUM_SERVERS()"
        }
        else
          throw new Exception("Hash partition is not supported when no primary key is specified.")
      }
      else if (partitionType.get == HANA_STREAM_SETTINGS.ROUND_ROBIN_PARTITION) {
        partition = " PARTITION BY ROUNDROBIN PARTITIONS GET_NUM_SERVERS()"
      }
    }

    s"""CREATE ROW TABLE $table ($sqlSchema$primaryKey)$partition"""

  }

  def getConnection(options:HanaOptions): Connection = {

    val driver = getDriver(options.getJdbcDriver)
    val url = s"jdbc:sap://${options.getDatabaseUrl}"

    val jdbcProperties = new Properties()

    /* User authentication */

    val (user, pass) = options.getUserAndPass
    if (user.isDefined && pass.isDefined) {

      jdbcProperties.put("user", user.get)
      jdbcProperties.put("password", pass.get)

    }

    driver.connect(url, jdbcProperties)

  }

  override def getDriverClassName(jdbcDriverName:String): String = {
    jdbcDriverName match {
      case "com.sap.db.jdbc.Driver" =>
        classForName(jdbcDriverName).getName
      case _ =>
        classForName(HANA_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME).getName

    }
  }

  def insertValue(conn:Connection, stmt:PreparedStatement, row:Row, pos:Int, dataType:DataType):Unit = {

    dataType match {
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
       * SAP HANA does not support complex data types like ARRAY
       */
      case _ =>
        throw new Exception(s"Data type `${dataType.simpleString}` is not supported.")
    }

  }

  def tableExists(conn:Connection, options:HanaOptions):Boolean = {

    val table = options.getTable
    val sql = s"""SELECT * FROM $table WHERE 1 = 0"""

    Try {

      val statement = conn.prepareStatement(sql)
      try {
        statement.setQueryTimeout(0)
        statement.executeQuery()

      } finally {
        statement.close()
      }

    }.isSuccess
  }

}
