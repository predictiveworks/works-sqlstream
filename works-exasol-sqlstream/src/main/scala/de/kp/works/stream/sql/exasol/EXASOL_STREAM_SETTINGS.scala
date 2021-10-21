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

object EXASOL_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.exasol.ExasolSinkProvider"
  /**
   * The driver name of the Jdbc driver used to
   * connect and access Exasol
   */
  val DEFAULT_JDBC_DRIVER_NAME = "com.exasol.jdbc.EXADriver"

  /**
   * The maximum batch size of the internal cache
   * before writing to Exasol
   */
  val BATCH_SIZE = "batch.size"

  val EXASOL_HOST        = "exasol.host"
  val EXASOL_JDBC_DRIVER = "exasol.jdbc.driver"
  /**
   * The maximum number of retries to write
   * to an Exasol instance
   */
  val EXASOL_MAX_RETRIES = "exasol.max.retries"
  val EXASOL_PASSWORD    = "exasol.password"
  val EXASOL_PRIMARY_KEY = "exasol.primary.key"
  val EXASOL_PORT        = "exasol.port"
  val EXASOL_TABLE       = "exasol.table"
  val EXASOL_TIMEOUT     = "exasol.timeout"
  val EXASOL_USER        = "exasol.username"

}
