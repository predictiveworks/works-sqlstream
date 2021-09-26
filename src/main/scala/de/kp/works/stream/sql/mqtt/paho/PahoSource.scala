package de.kp.works.stream.sql.mqtt.paho
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

import de.kp.works.stream.sql.Logging
import de.kp.works.stream.sql.mqtt.{LocalEventStore, LongOffset, MqttEvent}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.reader.InputPartition
import org.apache.spark.sql.sources.v2.reader.streaming.{MicroBatchReader, Offset}
import org.apache.spark.sql.types.StructType
import org.eclipse.paho.client.mqttv3._

import java.util
import java.util.Optional
import javax.annotation.concurrent.GuardedBy
import scala.collection.concurrent.TrieMap

class PahoSource(options: PahoOptions)
  extends MicroBatchReader with Logging {

  private val events = new TrieMap[Long, MqttEvent]

  private val persistence = options.getPersistence
  private val store = new LocalEventStore(persistence)

  @GuardedBy("this")
  private var currentOffset: LongOffset = LongOffset(-1L)

  @GuardedBy("this")
  private var lastOffsetCommitted: LongOffset = LongOffset(-1L)

  private var client:MqttClient = _
  buildMqttClient()

  private def getCurrentOffset = currentOffset

  override def commit(offset: Offset): Unit = ???

  override def deserializeOffset(s: String): Offset = ???

  override def getStartOffset: Offset = ???

  override def getEndOffset: Offset = ???

  override def planInputPartitions(): util.List[InputPartition[InternalRow]] = ???

  override def readSchema(): StructType = ???

  override def setOffsetRange(optional: Optional[Offset], optional1: Optional[Offset]): Unit = ???

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

    client = new MqttClient(brokerUrl, clientId, persistence)
    val callback = new MqttCallbackExtended() {

      override def messageArrived(topic_ : String, message: MqttMessage): Unit = synchronized {

        val mqttEvent = new MqttEvent(topic_, message)

        val offset = currentOffset.offset + 1L
        events.put(offset, mqttEvent)

        store.store(offset, mqttEvent)

        currentOffset = LongOffset(offset)
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
