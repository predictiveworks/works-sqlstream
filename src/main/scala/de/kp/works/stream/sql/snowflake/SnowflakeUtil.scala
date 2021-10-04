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

import java.sql.Connection
import java.util.Properties

object SnowflakeUtil extends JdbcUtil {

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
}
