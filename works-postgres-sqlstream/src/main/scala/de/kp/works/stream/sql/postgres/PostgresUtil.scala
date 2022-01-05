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
import org.apache.spark.sql.types.{DataType, StructType}

import java.sql.{Connection, PreparedStatement}

object PostgresUtil extends JdbcUtil {

  override def getDriverClassName(jdbcDriverName: String): String = ???

  def createTableIfNotExist(conn: Connection, schema: StructType, options: PostgresOptions): Boolean = ???
  /**
   * The UPSERT SQL statement is built from the provided
   * schema specification as the Postgres stream writer
   * ensures that table schema and provided schema are
   * identical
   */
  def createUpsertSql(schema:StructType, options:PostgresOptions): String = ???

  def getConnection(options: PostgresOptions): Connection = ???
  /**
   * This method inserts a stream (column) value into the
   * provided prepared statement, controlled by the Spark
   * data type
   */
  def upsertValue(conn:Connection, stmt:PreparedStatement, row:Row, pos:Int, dataType:DataType):Unit = ???

}
