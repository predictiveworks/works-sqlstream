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

import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.sources.v2.DataSourceOptions

import scala.collection.JavaConverters._

class HanaOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getBatchSize:Int =
    settings.getOrElse(HANA_STREAM_SETTINGS.BATCH_SIZE, "1000").toInt

  /**
   * The connection timeout is specified in seconds
   * and defaults to 10
   */
  def getConnectionTimeout:Int =
    settings.getOrElse(HANA_STREAM_SETTINGS.HANA_TIMEOUT, "10").toInt

  /**
   * The database url is a combination of host, port
   * and database name, and has a HANA specific url
   * representation
   */
  def getDatabaseUrl:String = {

    val host = settings.getOrElse(HANA_STREAM_SETTINGS.HANA_HOST,
      throw new Exception(s"No SAP HANA host specified."))

    if (host.isEmpty)
      throw new Exception(s"No SAP HANA host specified.")

    val port = settings.getOrElse(HANA_STREAM_SETTINGS.HANA_PORT,
      throw new Exception(s"No SAP HANA port specified.")).toInt

    val database = settings.getOrElse(HANA_STREAM_SETTINGS.HANA_DATABASE,
      throw new Exception(s"No SAP HANA database specified."))

    if (database.isEmpty)
      throw new Exception(s"No SAP HANA database specified.")
    /*
     * NOTE: The `?` is a SAP HANA specification of the
     * database name parameter within the JDBC url
     */
    val url = s"$host:$port/?$database"
    url

  }

  def getJdbcDriver:String =
    settings.getOrElse(HANA_STREAM_SETTINGS.HANA_JDBC_DRIVER,
      HANA_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME)

  def getMaxRetries:Int =
    settings.getOrElse(HANA_STREAM_SETTINGS.HANA_MAX_RETRIES, "3").toInt

  def getPartition:Option[String] =
    settings.get(HANA_STREAM_SETTINGS.HANA_PARTITION)

  def getPrimaryKey:Option[String] =
    settings.get(HANA_STREAM_SETTINGS.HANA_PRIMARY_KEY)

  /**
   * This method builds the SAP HANA table name from
   * the provided configuration. It thereby distinguishes
   * between uses case with and without namespace
   */
  def getTable:String = {

    val table = settings.getOrElse(HANA_STREAM_SETTINGS.HANA_TABLE,
      throw new Exception(s"No SAP HANA table specified."))

    val namespace = settings.get(HANA_STREAM_SETTINGS.HANA_NAMESPACE)
    namespace match {
      case Some(value) => s""""$value"."$table""""
      case None => table
    }

  }

  /* User authentication */

  def getUserAndPass:(Option[String], Option[String]) = {

    val username =
      settings.get(HANA_STREAM_SETTINGS.HANA_USER)

    val password =
      settings.get(HANA_STREAM_SETTINGS.HANA_PASSWORD)

    (username, password)

  }

}
