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

import de.kp.works.stream.sql.Logging
import de.kp.works.stream.ssl.SslUtil
import net.snowflake.client.jdbc.SnowflakeDriver
import org.apache.spark.{SPARK_VERSION,SparkEnv}
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.{PrivateKey, Security}
import scala.collection.JavaConverters._

class SnowflakeOptions(options: DataSourceOptions) extends Logging {

  private val SNOWFLAKE_WRITER_VERSION = "snowflake.stream.writer.version"
  /**
   * The version of the [SnowflakeStreamWriter]
   */
  private val VERSION = "0.4.0"

  private val settings:Map[String,String] = options.asMap.asScala.toMap
  /**
   * Client info related variables
   */
  private lazy val javaVersion =
    System.getProperty("java.version", "UNKNOWN")

  private lazy val jdbcVersion =
    SnowflakeDriver.implementVersion

  private lazy val scalaVersion =
    util.Properties.versionNumberString

  private lazy val sparkAppName = if (SparkEnv.get != null) {
    SparkEnv.get.conf.get("spark.app.name", "")
  } else {
    ""
  }

  def getBatchSize:Int =
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.BATCH_SIZE, "1000").toInt

  def getJdbcDriver:String =
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.JDBC_DRIVER,
      SNOWFLAKE_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME)

  def getAccount:String =
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_ACCOUNT,
      throw new Exception(s"No Snowflake account name specified."))

  def getAuthenticator:String =
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_AUTHENTICATOR,
      "snowflake")

  def getClientInfoString: String = {
    val snowflakeClientInfo =
      s""" {
         | "spark.version" : "${esc(SPARK_VERSION)}",
         | "$SNOWFLAKE_WRITER_VERSION" : "${esc(VERSION)}",
         | "spark.app.name" : "${esc(sparkAppName)}",
         | "scala.version" : "${esc(scalaVersion)}",
         | "java.version" : "${esc(javaVersion)}",
         | "snowflakedb.jdbc.version" : "${esc(jdbcVersion)}"
         |}""".stripMargin

    snowflakeClientInfo

  }

  def getDatabaseUrl:String = {
    /*
     * The connection url to a Snowflake cloud instance
     * is [account].snowflakecomputing.com
     */
    val account = settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_ACCOUNT,
      throw new Exception(s"No Snowflake account name specified."))

    s"$account.snowflakecomputing.com"

  }

  def getDatabase:String =
    /* The name of the Snowflake database */
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_DATABASE,
      throw new Exception(s"No Snowflake database name specified."))

  def getPassword:Option[String] =
  /*
   * The user password or the private key password
   * in case of an encrypted private key
   */
    settings.get(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_PASSWORD)

  def getPrivateKey:Option[PrivateKey] = {

    val fileName = settings.get(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_PRIVATE_KEY)
    if (fileName.isEmpty) return None

    try {

      Security.addProvider(new BouncyCastleProvider())

      val password = getPassword
      val privateKey = SslUtil.getPrivateKeyFromPEM(fileName.get, password)

      Some(privateKey)

    } catch {
      case _:Throwable =>
        throw new Exception(s"Private key is invalid.")
    }

  }

  def getRole:Option[String] =
    /* The name of the Snowflake user role */
    settings.get(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_ROLE)

  def getSchema:String =
    /* The name of the Snowflake schema */
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_SCHEMA,
      "public")

  def getSsl:String =
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_SSL,
      "on")

  def getToken:Option[String] =
  /* The OAuth token */
    settings.get(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_OAUTH_TOKEN)

  def getUser:String =
    /* The name of the Snowflake user */
    settings.getOrElse(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_USER,
      throw new Exception(s"No Snowflake user name specified."))

  def getWarehouse:Option[String] =
    /* The name of the Snowflake warehouse */
    settings.get(SNOWFLAKE_STREAM_SETTINGS.SNOWFLAKE_WAREHOUSE)

  /** CLIENT INFO RELATED **/

  private def esc(s: String): String = {
    s.replace("\"", "").replace("\\", "")
  }

}
