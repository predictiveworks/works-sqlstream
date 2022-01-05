package de.kp.works.stream.sql.postgres

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


object POSTGRES_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.postgres.PostgresSinkProvider"
  /**
   * The driver name of the Jdbc driver used to
   * connect and access Postgres
   */
  val DEFAULT_JDBC_DRIVER_NAME = "org.postgresql.Driver"
  /**
   * The maximum batch size of the internal cache
   * before writing to Postgres
   */
  val BATCH_SIZE = "batch.size"
  val POSTGRES_JDBC_DRIVER = "postgres.jdbc.driver"
  /**
   * The maximum number of retries to write
   * to a Postgres instance
   */
  val POSTGRES_MAX_RETRIES = "postgres.max.retries"
  val POSTGRES_TABLE       = "postgres.table"
  val POSTGRES_TIMEOUT     = "postgres.timeout"

}
