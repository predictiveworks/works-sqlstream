package de.kp.works.stream.sql.mqtt.ditto
/*
 * Copyright (c) 2019 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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

object DITTO_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.mqtt.ditto.DittoSourceProvider"

  val DITTO_ENDPOINT = "ditto.endpoint"

  /** THING **/

  /* namespace:name */
  val DITTO_THING_ID = "ditto.thing.id"

  val DITTO_THING_NAME = "ditto.thing.name"
  val DITTO_THING_NAMESPACE = "ditto.thing.namespace"

  val DITTO_FEATURE_ID = "ditto.feature.id"

  /** BASIC AUTHENTICATION **/

  val DITTO_USER = "ditto.user"
  val DITTO_PASS = "ditto.pass"

  /** OAUTH2 **/

  val DITTO_OAUTH_CLIENT_ID     = "ditto.oauth.client.id"
  val DITTO_OAUTH_CLIENT_SECRET = "ditto.oauth.client.secret"

  val DITTO_OAUTH_SCOPES = "ditto.oauth.scopes"
  val DITTO_OAUTH_TOKEN_ENDPOINT = "ditto.oauth.token.endpoint"

  /** TRUST STORE **/

  val DITTO_TRUSTSTORE_LOCATION = "ditto.truststore.location"
  val DITTO_TRUSTSTORE_PASSWORD = "ditto.truststore.password"

  /** PROXY **/

  val DITTO_PROXY_HOST = "ditto.proxy.host"
  val DITTO_PROXY_PORT = "ditto.proxy.port"

  /** HANDLER **/

  val DITTO_THING_CHANGES = "ditto.thing.changes"
  val DITTO_THING_CHANGES_HANDLER = "DITTO_THING_CHANGES"

  val DITTO_FEATURES_CHANGES = "ditto.features.changes"
  val DITTO_FEATURES_CHANGES_HANDLER = "DITTO_FEATURES_CHANGES"

  val DITTO_FEATURE_CHANGES = "ditto.feature.changes"
  val DITTO_FEATURE_CHANGES_HANDLER = "DITTO_FEATURE_CHANGES"

  val DITTO_LIVE_MESSAGES = "ditto.live.messages"
  val DITTO_LIVE_MESSAGES_HANDLER = "DITTO_LIVE_MESSAGES"

  val PERSISTENCE = "persistence"
  /*
   * The schema type controls the output schema
   * assigned to the incoming MQTT stream
   */
  val SCHEMA_TYPE = "schema.type"

}
