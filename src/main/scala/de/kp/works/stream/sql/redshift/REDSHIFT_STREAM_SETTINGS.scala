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

object REDSHIFT_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.redshift.RedshiftSinkProvider"
  /**
   * The driver name of the Jdbc driver used to
   * connect and access Amazon Redshift
   */
  val DEFAULT_JDBC_DRIVER_NAME = "com.amazon.redshift.jdbc42.Driver"
  /**
   * The maximum batch size of the internal cache
   * before writing to Redshift
   */
  val BATCH_SIZE   = "batch.size"
  val REDSHIFT_DATABASE    = "redshift.database"
  val REDSHIFT_DIST_KEY    = "redshift.dist.key"
  val REDSHIFT_DIST_STYLE  = "redshift.dist.style"
  val REDSHIFT_HOST        = "redshift.host"
  val REDSHIFT_JDBC_DRIVER = "redshift.jdbc.driver"
  /**
   * The maximum number of retries to write
   * to a Redshift instance
   */
  val REDSHIFT_MAX_RETRIES = "redshift.max.retries"
  val REDSHIFT_PASSWORD    = "redshift.password"
  val REDSHIFT_PORT        = "redshift.port"
  val REDSHIFT_PRIMARY_KEY = "redshift.primary.key"
  val REDSHIFT_SORT_KEY    = "redshift.sort.key"
  val REDSHIFT_TABLE       = "redshift.table"
  val REDSHIFT_TIMEOUT     = "redshift.timeout"
  val REDSHIFT_USER        = "redshift.username"

}
