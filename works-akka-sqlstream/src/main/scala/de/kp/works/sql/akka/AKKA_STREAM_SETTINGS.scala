package de.kp.works.sql.akka

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

object AKKA_STREAM_SETTINGS {

  val FORMAT_AKKA = "de.kp.works.stream.sql.akka.AkkaSourceProvider"

  val AKKA_MAX_RETRIES   = "akka.max.retries"
  val AKKA_PUBLISHER_URL = "akka.publisher.url"
  val AKKA_TIME_RANGE    = "akka.time.range"

  val PERSISTENCE = "persistence"
  /**
   * The schema type controls the output schema
   * assigned to the incoming Akka stream
   */
  val SCHEMA_TYPE = "schema.type"

}
