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

import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.sources.v2.DataSourceOptions

import scala.collection.JavaConverters._

class RedshiftOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getBatchSize:Int =
    settings.getOrElse(REDSHIFT_STREAM_SETTINGS.BATCH_SIZE, "1000").toInt

  /**
   * The connection timeout is specified in seconds
   * and defaults to 10
   */
  def getConnectionTimeout:Int =
    settings.getOrElse(REDSHIFT_STREAM_SETTINGS.REDSHIFT_TIMEOUT, "10").toInt

  /**
   * The database url is a combination of host, port
   * and database name
   */
  def getDatabaseUrl:String = {

    val host = settings.getOrElse(REDSHIFT_STREAM_SETTINGS.REDSHIFT_HOST,
      throw new Exception(s"No Redshift host specified."))

    if (host.isEmpty)
      throw new Exception(s"No Redshift host specified.")

    val port = settings.getOrElse(REDSHIFT_STREAM_SETTINGS.REDSHIFT_PORT,
      throw new Exception(s"No Redshift port specified.")).toInt

    val database = settings.getOrElse(REDSHIFT_STREAM_SETTINGS.REDSHIFT_DATABASE,
      throw new Exception(s"No Redshift database specified."))

    if (database.isEmpty)
      throw new Exception(s"No Redshift database specified.")

    val url = s"$host:$port/$database"
    url

  }

  def getJdbcDriver:String =
    settings.getOrElse(REDSHIFT_STREAM_SETTINGS.REDSHIFT_JDBC_DRIVER,
      REDSHIFT_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME)

  def getMaxRetries:Int =
    settings.getOrElse(REDSHIFT_STREAM_SETTINGS.REDSHIFT_MAX_RETRIES, "3").toInt

  /* User authentication */

  def getUserAndPass:(Option[String], Option[String]) = {

    val username =
      settings.get(REDSHIFT_STREAM_SETTINGS.REDSHIFT_USER)

    val password =
      settings.get(REDSHIFT_STREAM_SETTINGS.REDSHIFT_PASSWORD)

    (username, password)

  }

}
