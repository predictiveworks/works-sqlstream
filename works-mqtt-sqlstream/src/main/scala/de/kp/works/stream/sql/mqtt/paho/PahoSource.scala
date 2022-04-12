package de.kp.works.stream.sql.mqtt.paho

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

import de.kp.works.stream.sql.mqtt.{MqttEvent, MqttSchema, MqttUtil}
import de.kp.works.stream.sql.{LongOffset, WorksSource}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType
import org.eclipse.paho.client.mqttv3._

class PahoSource(options: PahoOptions) extends WorksSource(options) {

  private var client:MqttClient = _
  buildMqttClient()

  override def readSchema(): StructType =
    MqttSchema.getSchema(options.getSchemaType)

  /**
   * This method stops this streaming source
   */
  override def stop(): Unit = synchronized {

    client.disconnect()
    persistence.close()

    client.close()

  }

  private def buildMqttClient(): Unit = {

    val brokerUrl = options.getBrokerUrl
    val clientId  = options.getClientId

    client = new MqttClient(brokerUrl, clientId)
    val schemaType = options.getSchemaType

    val callback = new MqttCallbackExtended() {

      override def messageArrived(topic_ : String, message: MqttMessage): Unit = synchronized {

        val mqttEvent = new MqttEvent(topic_, message)

        val rows = MqttUtil.toRows(mqttEvent, schemaType)

        if (rows.isDefined) {
          rows.get.foreach(row => {

            val offset = currentOffset.offset + 1L

            events.put(offset, row)
            store.store[Row](offset, row)

            currentOffset = LongOffset(offset)

          })
        }

        log.trace(s"Message arrived, $topic_ $mqttEvent")

      }

      override def deliveryComplete(token: IMqttDeliveryToken): Unit = {
        /* Do nothing */
      }

      override def connectionLost(cause: Throwable): Unit = {
        log.warn("Connection to mqtt server lost.", cause)
      }

      override def connectComplete(reconnect: Boolean, serverURI: String): Unit = {
        log.info(s"Connect complete $serverURI. Is it a reconnect?: $reconnect")
      }
    }
    /* Register callback */
    client.setCallback(callback)

    /* Assign Mqtt options */
    val mqttOptions = options.getMqttOptions
    client.connect(mqttOptions)

    /* Subscribe to Mqtt topics */
    val qos    = options.getQos
    val topics = options.getTopics

    val quality = topics.map(_ => qos)
    client.subscribe(topics, quality)

  }
}
