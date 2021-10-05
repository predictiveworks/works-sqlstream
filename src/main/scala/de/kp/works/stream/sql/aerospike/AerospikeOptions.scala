package de.kp.works.stream.sql.aerospike
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

import com.aerospike.client.policy.AuthMode
import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.sources.v2.DataSourceOptions

import scala.collection.JavaConverters._

class AerospikeOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getAuthMode:AuthMode = {
    val value = settings
      .getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_AUTH_MODE, "INTERNAL")

    AuthMode.valueOf(value.toUpperCase)
  }

  def getBatchSize:Int =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.BATCH_SIZE, "1000").toInt

  def getExpiration:Int =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_EXPIRATION, "0").toInt

  def getHost:String =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_HOST,
      throw new Exception("No Aerospike database host specified."))

  def getMaxRetries:Int =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_MAX_RETRIES, "3").toInt

  def getNamespace:String =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_NAMESPACE,
      throw new Exception("No Aerospike namespace specified."))

  def getPort:Int =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_PORT,
      throw new Exception("No Aerospike database port specified.")).toInt

  def getSetname:String =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_SET,
      throw new Exception("No Aerospike set name specified."))

  /**
   * The timeout of an Aerospike database connection
   * in milliseconds. Default is 1000.
   */
  def getTimeout:Int =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_TIMEOUT, "1000").toInt

  def getTlsMode:String =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_TLS_MODE, "false")

  def getTlsName:String =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_TLS_NAME, null)

  /* User authentication */

  def getUserAndPass:(String, String) = {

    val username =
      settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_USER,
        throw new Exception("No Aerospike user name specified."))

    val password =
      settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_PASSWORD,
        throw new Exception("No Aerospike user password specified."))

    (username, password)

  }

  def getWriteMode:String =
    settings.getOrElse(AEROSPIKE_STREAM_SETTINGS.AEROSPIKE_WRITE, "Append")

}
