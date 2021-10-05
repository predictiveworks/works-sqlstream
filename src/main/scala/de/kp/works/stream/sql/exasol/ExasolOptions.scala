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

import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.sources.v2.DataSourceOptions

import scala.collection.JavaConverters._

class ExasolOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getBatchSize:Int =
    settings.getOrElse(EXASOL_STREAM_SETTINGS.BATCH_SIZE, "1000").toInt

  /**
   * The connection timeout is specified in seconds
   * and defaults to 10
   */
  def getConnectionTimeout:Int =
    settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_TIMEOUT, "10").toInt

  def getDatabaseUrl:String = {

    val host = settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_HOST,
      throw new Exception(s"No Exasol host specified."))

    if (host.isEmpty)
      throw new Exception(s"No Exasol host specified.")

    val port = settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_PORT,
      throw new Exception(s"No Exasol port specified.")).toInt

    val url = s"$host:$port"
    url

  }

  def getJdbcDriver:String =
    settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_JDBC_DRIVER,
      EXASOL_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME)

  def getMaxRetries:Int =
    settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_MAX_RETRIES, "3").toInt

  def getPrimaryKey:Option[String] =
    settings.get(EXASOL_STREAM_SETTINGS.EXASOL_PRIMARY_KEY)

  def getTable:String =
    settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_TABLE,
      throw new Exception(s"No Exasol table specified."))

  /* User authentication */

  def getUserAndPass:(String, String) = {

    val username =
      settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_USER,
        throw new Exception("No Exasol user name specified."))

    val password =
      settings.getOrElse(EXASOL_STREAM_SETTINGS.EXASOL_PASSWORD,
        throw new Exception("No Exasol user password specified."))

    (username, password)

  }

}
