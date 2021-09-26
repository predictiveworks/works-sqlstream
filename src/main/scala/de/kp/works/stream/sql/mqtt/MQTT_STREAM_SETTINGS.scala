package de.kp.works.stream.sql.mqtt
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

object MQTT_STREAM_SETTINGS {

  val FORMAT_DITTO = "de.kp.works.stream.sql.mqtt.ditto.DittoSourceProvider"
  val FORMAT_PAHO  = "de.kp.works.stream.sql.mqtt.paho.PahoSourceProvider"

  val AUTO_RECONNECT = "auto.reconnect"
  val BROKER_URL     = "broker.url"
  val CLEAN_SESSION  = "clean.session"
  val CLIENT_ID      = "client.id"
  val KEEP_ALIVE     = "keep.alive"
  val MAX_INFLIGHT   = "max.inflight"
  val PASSWORD       = "password"
  val PERSISTENCE    = "persistence"
  val QOS            = "qos"
  val TIMEOUT        = "timeout"
  val TOPICS         = "topics"
  val USERNAME       = "username"
  val VERSION        = "version"

}
