package de.kp.works.stream.sql.pubsub

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

object PUBSUB_STREAM_SETTINGS {

  val FORMAT_AKKA = "de.kp.works.stream.sql.pubsub.PubSubSourceProvider"

  val PERSISTENCE = "persistence"

  val PUBSUB_APP_NAME          = "pubsub.app.name"
  val PUBSUB_AUTO_ACK          = "pubsub.auto.ack"
  val PUBSUB_BACKOFF_INIT      = "pubsub.backoff.init"
  val PUBSUB_BACKOFF_MAX       = "pubsub.backoff.max"
  val PUBSUB_MESSAGE_MAX       = "pubsub.message.max"
  val PUBSUB_PROJECT_NAME      = "pubsub.project.name"
  val PUBSUB_SUBSCRIPTION_NAME = "pubsub.subscription.name"
  val PUBSUB_TOPIC_NAME        = "pubsub.topic.name"
  /**
   * The schema type controls the output schema
   * assigned to the incoming PUB/SUB stream
   */
  val SCHEMA_TYPE = "schema.type"

}
