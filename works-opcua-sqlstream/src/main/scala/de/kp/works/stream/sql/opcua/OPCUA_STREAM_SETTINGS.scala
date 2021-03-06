package de.kp.works.stream.sql.opcua

/**
 * Copyright (c) 2020 - 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

object OPCUA_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.opcua.OpcuaSourceProvider"

  val OPCUA_RETRY_WAIT      = "opcua.retry.wait"
  val OPCUA_SECURITY_FOLDER = "opcua.security.folder"
  val OPCUA_SECURITY_POLICY = "opcua.security.policy"
  val OPCUA_STARTUP_TOPICS  = "opcua.startup.topics"
  /**
   * User credentials
   */
  val OPCUA_USER_NAME = "opcua.user.name"
  val OPCUA_USER_PASS = "opcua.user.pass"

  val PERSISTENCE = "persistence"
  /**
   * The schema type controls the output schema
   * assigned to the incoming Opcua stream
   */
  val SCHEMA_TYPE = "schema.type"

}
