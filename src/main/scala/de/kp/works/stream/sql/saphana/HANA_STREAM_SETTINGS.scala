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

object HANA_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.saphana.HanaSinkProvider"
  /**
   * The driver name of the Jdbc driver used to
   * connect and access SAP HANA
   */
  val DEFAULT_JDBC_DRIVER_NAME = "com.sap.db.jdbc.Driver"
  /**
   * The maximum batch size of the internal cache
   * before writing to SAP HANA
   */
  val BATCH_SIZE = "batch.size"
  val JDBC_DRIVER = "jdbc.driver"
}
