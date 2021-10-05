package de.kp.works.stream.sql.exasol
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

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import java.sql.{Connection, PreparedStatement}
import scala.util.Try

object ExasolUtil {
  /**
   * Compute the SAP HANA SQL schema string for the
   * given Spark SQL Schema.
   */
  def buildSqlSchema(schema: StructType, options:ExasolOptions): String = {

    val sqlSchema = schema.fields.map { field => {

      val fname = field.name
      val ftype = field.dataType match {
        case BinaryType      => "CLOB"
        case BooleanType     => "BOOLEAN"
        case ByteType        => "TINYINT"
        case DateType        => "DATE"
        case dt: DecimalType =>
          val precision = math.min(math.min(dt.precision, DecimalType.MAX_PRECISION), 36)
          val scale = math.min(math.min(dt.scale, DecimalType.MAX_SCALE), 36)
          s"DECIMAL($precision,$scale)"
        case DoubleType      => "DOUBLE"
        case FloatType       => "FLOAT"
        case IntegerType     => "INTEGER"
        case LongType        => "BIGINT"
        case ShortType       => "SMALLINT"
        case StringType      => "CLOB"
        case TimestampType   => "TIMESTAMP"
        case _ => throw new IllegalArgumentException(s"Don't know how to save $field to JDBC")
      }

      val nullable = if (field.nullable) "" else "NOT NULL"
      s"""$fname $ftype $nullable"""

    }}.mkString(", ")
    sqlSchema

  }

  /**
   * The INSERT SQL statement is built from the provided
   * schema specification as the Exasol stream writer
   * ensures that table schema and provided schema are
   * identical
   */
  def createInsertSql(schema:StructType, options:ExasolOptions): String = {

    val table = options.getTable

    val columns = schema.fields
      .map(field => s"""${field.name}""")
      .mkString(",")

    val values = schema.fields.map(_ => "?").mkString(",")
    val insertSql = s"""INSERT INTO $table ($columns) VALUES($values)"""

    insertSql

  }

  def getConnection(options: ExasolOptions): Connection = ???

  /**
   * This method inserts a stream (column) value into the
   * provided prepared statement, controlled by the Spark
   * data type
   */
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
       * Exasol does not support complex data types like ARRAY
       */
      case _ =>
        throw new Exception(s"Data type `${dataType.simpleString}` is not supported.")
    }

  }

  def tableExists(conn:Connection, options:ExasolOptions):Boolean = {

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
