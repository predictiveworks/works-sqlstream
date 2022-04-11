package de.kp.works.stream.sql.mqtt

/**
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

import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.Charset

class MqttEvent(val topic:String, message:MqttMessage) {
  /*
   * The message identifier
   */
  val id: Int = message.getId
  /*
   * The timestamp in milli seconds the message arrived
   */
  val timestamp: Long = System.currentTimeMillis()
  /*
   * The quality of service of the message
   */
  val qos: Int = message.getQos
  /*
   * Indicates whether or not this message might be a
   * duplicate of one which has already been received.
   */
  val duplicate: Boolean = message.isDuplicate
  /*
   * Indicates whether or not this message should be/was
   * retained by the server.
   */
  val retained: Boolean = message.isRetained
  /*
   * The payload of this message
   */
  val payload: Array[Byte] = message.getPayload

  override def toString: String = {
    s"""MQTTEvent.
       |EventId: ${this.id}
       |Timestamp: ${this.timestamp}
       |Topic: ${this.topic}
       |QoS: ${this.qos}
       |isDuplicate: ${this.duplicate}
       |isRetained: ${this.retained}
       |Payload as string: ${new String(this.payload, Charset.defaultCharset())}
     """.stripMargin
  }

}
