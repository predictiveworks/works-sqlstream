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

object SNOWFLAKE_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.snowflake.SnowflakeSinkProvider"
  /**
   * The driver name of the Jdbc driver used to
   * connect and access Snowflake
   */
  val DEFAULT_JDBC_DRIVER_NAME = "net.snowflake.client.jdbc.SnowflakeDriver"
  /**
   * The maximum batch size of the internal cache
   * before writing to Snowflake
   */
  val BATCH_SIZE = "batch.size"
  val JDBC_DRIVER = "jdbc.driver"

  val SNOWFLAKE_ACCOUNT       = "snowflake.account"
  /**
   * Specifies the authenticator to use for verifying
   * user login credentials. You can set this to one
   * of the following values:
   *
   * - `snowflake`        - use the internal Snowflake authenticator (default)
   *
   * - `externalbrowser`  - use your web browser to authenticate with Okta, ADFS,
   *                        or any other SAML 2.0-compliant identity provider (IdP)
   *                        that has been defined for your Snowflake account.
   *
   * - `oauth`            - to authenticate using OAuth. When OAuth is specified
   *                        as the authenticator, you must also set the token parameter
   *                        to specify the OAuth token.
   *
   * - snowflake_jwt      - to authenticate using key pair authentication.
   *
   * - username_password_mfa - to authenticate with MFA token caching.

   *
   */
  val SNOWFLAKE_AUTHENTICATOR = "snowflake.authenticator"
  val SNOWFLAKE_DATABASE      = "snowflake.database"
  val SNOWFLAKE_OAUTH_TOKEN   = "snowflake.oauth.token"
  val SNOWFLAKE_PASSWORD      = "snowflake.password"
  /**
   * The file name where to find the PEM private key
   */
  val SNOWFLAKE_PRIVATE_KEY   = "snowflake.private.key"
  /**
   * The maximum number of retries to write
   * to a Snowflake instance
   */
  val SNOWFLAKE_MAX_RETRIES   = "snowflake.max.retries"
  val SNOWFLAKE_ROLE          = "snowflake.role"
  val SNOWFLAKE_SCHEMA        = "snowflake.schema"
  /**
   * Snowflake SSL on/off - "on" by default
   */
  val SNOWFLAKE_SSL           = "snowflake.ssl"
  val SNOWFLAKE_TIMEOUT       = "snowflake.timeout"
  val SNOWFLAKE_USER          = "snowflake.user"
  val SNOWFLAKE_WAREHOUSE     = "snowflake.warehouse"

}
