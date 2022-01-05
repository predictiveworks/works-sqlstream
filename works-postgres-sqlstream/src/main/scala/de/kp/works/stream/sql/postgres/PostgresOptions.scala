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

import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.sources.v2.DataSourceOptions

import scala.collection.JavaConverters._

class PostgresOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getBatchSize:Int =
    settings.getOrElse(POSTGRES_STREAM_SETTINGS.BATCH_SIZE, "1000").toInt

  /**
   * The connection timeout is specified in seconds
   * and defaults to 10
   */
  def getConnectionTimeout:Int =
    settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_TIMEOUT, "10").toInt

  def getDatabaseUrl:String = {

    val host = settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_HOST,
      throw new Exception(s"No Postgres host specified."))

    if (host.isEmpty)
      throw new Exception(s"No Postgres host specified.")

    val port = settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_PORT,
      POSTGRES_STREAM_SETTINGS.DEFAULT_POSTGRES_PORT).toInt

    /*
     * Providing a database name is optional
     */
    val database = settings.get(POSTGRES_STREAM_SETTINGS.POSTGRES_DATABASE)
    if (database.isDefined && database.get.nonEmpty) {
      s"$host:$port/${database.get}"
    }
    else {
      s"$host:$port/"
    }

  }

  def getJdbcDriver:String =
    settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_JDBC_DRIVER,
      POSTGRES_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME)

  def getMaxRetries:Int =
    settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_MAX_RETRIES, "3").toInt

  def getPrimaryKey:Option[String] =
    settings.get(POSTGRES_STREAM_SETTINGS.POSTGRES_PRIMARY_KEY)

  def getTable:String =
    settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_TABLE,
      throw new Exception(s"No Postgres table specified."))

  /* User authentication */

  def getUserAndPass:(String, String) = {

    val username =
      settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_USER,
        throw new Exception("No Postgres user name specified."))

    val password =
      settings.getOrElse(POSTGRES_STREAM_SETTINGS.POSTGRES_PASSWORD,
        throw new Exception("No Postgres user password specified."))

    (username, password)

  }

}
