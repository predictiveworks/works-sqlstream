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
import de.kp.works.stream.sql.mqtt.MQTT_STREAM_SETTINGS
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.eclipse.paho.client.mqttv3.persist.{MemoryPersistence, MqttDefaultFilePersistence}
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttClientPersistence, MqttConnectOptions}

import scala.collection.JavaConverters._

class PahoOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getBrokerUrl:String = {

    if (!settings.contains(MQTT_STREAM_SETTINGS.BROKER_URL))
      throw new Exception(s"[PahoOptions] No `broker.url` provided. Please specify in options.")

    settings(MQTT_STREAM_SETTINGS.BROKER_URL)

  }

  def getClientId:String = {

    if (!settings.contains(MQTT_STREAM_SETTINGS.CLIENT_ID)) {
      log.warn("[PahoOptions] A random value is picked up for the `client.id`. Recovering from failure is not supported.")
      MqttClient.generateClientId()

    } else
      settings(MQTT_STREAM_SETTINGS.CLIENT_ID)

  }

  def getMqttOptions:MqttConnectOptions = {

    val mqttConnectOptions = new MqttConnectOptions

    // TODO

    mqttConnectOptions

  }

  def getPersistence:MqttClientPersistence = {

    settings.get(MQTT_STREAM_SETTINGS.PERSISTENCE) match {
      case Some("file") =>
        new MqttDefaultFilePersistence()
      case Some("memory") =>
        new MemoryPersistence()

      case None => new MqttDefaultFilePersistence()
    }

  }

  def getQos:Int = {
    settings.getOrElse(MQTT_STREAM_SETTINGS.QOS, "1").toInt
  }

  def getTopics:Array[String] = {

    if (!settings.contains(MQTT_STREAM_SETTINGS.TOPICS)) {
      throw new Exception(s"[PahoOptions] No `topics` provided. Please specify in options.")
    }
    /*
     * Comma-separated list of topics
     */
    settings(MQTT_STREAM_SETTINGS.TOPICS)
      .split(",").map(_.trim)

  }

}
