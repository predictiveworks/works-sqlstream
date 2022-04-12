package de.kp.works.stream.sql.mqtt.hivemq

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
import org.eclipse.paho.client.mqttv3.MqttMessage

class HiveSource(options: HiveOptions) extends WorksSource(options) {

  private var client:HiveClient = _
  buildHiveClient()

  override def readSchema(): StructType =
    MqttSchema.getSchema(options.getSchemaType)

  override def stop(): Unit = synchronized {
    client.disconnect()
  }

  private def buildHiveClient():Unit = {

    client = HiveClient.build(options)
    val schemaType = options.getSchemaType

    val expose = new HiveExpose() {

      override def messageArrived(
        topic:String,
        payload: Array[Byte],
        qos: Int,
        duplicate: Boolean,
        retained: Boolean): Unit =  synchronized {
        /*
         * Harmonize with Paho [MqttMessage]
         */
        val message = new MqttMessage()

        /* Random message identifier */
        val id = math.abs(scala.util.Random.nextInt)
        message.setId(id)

        message.setQos(qos)
        message.setPayload(payload)

        message.setRetained(retained)

        val mqttEvent = new MqttEvent(topic, message)

        val rows = MqttUtil.toRows(mqttEvent, schemaType)

        if (rows.isDefined) {
          rows.get.foreach(row => {

            val offset = currentOffset.offset + 1L

            events.put(offset, row)
            store.store[Row](offset, row)

            currentOffset = LongOffset(offset)

          })
        }

        log.trace(s"Message arrived, $topic $mqttEvent")

      }

    }

    client.setExpose(expose)
    client.connect()

    if (client.isConnected) client.listen(options.getTopics)

  }

}
