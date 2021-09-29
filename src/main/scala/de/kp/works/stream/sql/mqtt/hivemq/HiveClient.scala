package de.kp.works.stream.sql.mqtt.hivemq
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

import com.hivemq.client.internal.mqtt.MqttRxClientBuilder
import com.hivemq.client.internal.mqtt.message.auth.MqttSimpleAuthBuilder
import com.hivemq.client.internal.mqtt.message.auth.mqtt3.Mqtt3SimpleAuthViewBuilder
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3RxClientViewBuilder
import com.hivemq.client.mqtt.MqttClientSslConfig
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import de.kp.works.stream.sql.Logging
import de.kp.works.stream.ssl.SslOptions
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.nio.charset.Charset
import java.security.Security
import java.util.UUID

object HiveClient {

  def build(options:HiveOptions):HiveClient = {

    val (mqttHost, mqttPort)  = options.getHostAndPort
    val (mqttUser, mqttPass) = options.getUserAndPass

    val mqttKeepAlive = options.getKeepAlive

    val mqttQoS = Some(options.getQoS)
    val mqttVersion = Some(options.getVersion)

    val mqttSsl =
      if (!options.isSsl)
        None

      else
        Some(options.getSslOptions)

    new HiveClient(
      mqttHost,
      mqttPort,
      mqttUser,
      mqttPass,
      mqttKeepAlive,
      mqttSsl,
      mqttQoS,
      mqttVersion)
  }

}

class HiveClient(
  mqttHost: String,
  mqttPort: Int,
  mqttUser: String,
  mqttPass: String,
  mqttKeepAlive:Int,
  mqttSsl: Option[SslOptions],
  mqttQoS: Option[Int] = None,
  mqttVersion: Option[Int] = None) extends Logging {

  private val UTF8 = Charset.forName("UTF-8")

  private var expose:HiveExpose = _
  private var connected:Boolean = false

  private var mqtt3Client: Option[Mqtt3AsyncClient] = None
  private var mqtt5Client: Option[Mqtt5AsyncClient] = None

  def setExpose(hiveExpose:HiveExpose):Unit = {
    expose = hiveExpose
  }

  /** MQTT v3.1 & v3.1.1 SUPPORT **/

  private val onMqtt3Connect = new java.util.function.BiConsumer[Mqtt3ConnAck, Throwable] {

    def accept(connAck: Mqtt3ConnAck, throwable: Throwable):Unit = {

      /* Error handling */
      if (throwable != null) {
        /*
         * In case of an error, the respective message is log,
         * but streaming is continued
         */
        log.error(throwable.getLocalizedMessage)

      } else {

        log.info("Connecting to HiveMQ Broker successfull")
        connected = true

      }
    }

  }

  private def connectToMqtt3(): Unit = {

    /***** BUILD CLIENT *****/

    val identifier = UUID.randomUUID().toString

    val builder = new Mqtt3RxClientViewBuilder()
      .identifier(identifier)
      .serverHost(mqttHost)
      .serverPort(mqttPort)

    /* Transport layer security */

    val sslConfig = getMqttSslConfig
    if (sslConfig != null) builder.sslConfig(sslConfig)

    /* Application layer security */

    val simpleAuth = new Mqtt3SimpleAuthViewBuilder.Default()
      .username(mqttUser)
      .password(mqttPass.getBytes(UTF8))
      .build()

    builder.simpleAuth(simpleAuth)

    /***** CONNECT *****/

    mqtt3Client = Some(builder.buildAsync())

    mqtt3Client.get
      .connectWith()
      .keepAlive(mqttKeepAlive)
      .send()
      .whenComplete(onMqtt3Connect)

  }

  /**
   * Set up callback for MqttClient. This needs to happen before
   * connecting or subscribing, otherwise messages may be lost
   */
  private val mqtt3Callback = new java.util.function.Consumer[Mqtt3Publish] {

    def accept(publish: Mqtt3Publish):Unit = {

      val topic = publish.getTopic.toString
      val payload = publish.getPayloadAsBytes

      if (payload != null) {

        val qos = publish.getQos.ordinal()

        /* NOT SUPPORTED */
        val duplicate = false

        val retained = publish.isRetain
        expose.messageArrived(topic, payload, qos, duplicate, retained)

      }

    }

  }

  private def listenToMqtt3(topics:Array[String]): Unit = {

    if (!connected || mqtt3Client.isEmpty) {
      throw new Exception("No connection to HiveMQ broker established")
    }
    /*
     * The HiveMQ client supports a single topic; the publish
     * interface, however, accept a list of topics to increase
     * use flexibility
     */
    val mqttTopic = getMqttTopic(topics)

    /***** SUBSCRIBE *****/

    /*
     * Define the subscription callback, and, log the
     * respective results
     */
    val onSubscription = new java.util.function.BiConsumer[Mqtt3SubAck, Throwable] {

      def accept(connAck: Mqtt3SubAck, throwable: Throwable):Unit = {

        /* Error handling */
        if (throwable != null) {
          log.error("Subscription failed: " + throwable.getLocalizedMessage)

        } else {
          log.debug("Subscription successful")

        }

      }

    }

    mqtt3Client.get
      .subscribeWith()
      .topicFilter(mqttTopic)
      .callback(mqtt3Callback)
      .send()
      .whenComplete(onSubscription)

  }

  /** MQTT v5 SUPPORT **/

  private val onMqtt5Connect = new java.util.function.BiConsumer[Mqtt5ConnAck, Throwable] {

    def accept(connAck: Mqtt5ConnAck, throwable: Throwable):Unit = {

      /* Error handling */
      if (throwable != null) {
        /*
         * In case of an error, the respective message is log,
         * but streaming is continued
         */
        log.error(throwable.getLocalizedMessage)

      } else {

        log.info("Connecting to HiveMQ Broker successfull")
        connected = true

      }
    }

  }

  private def connectToMqtt5(): Unit = {

    /***** BUILD CLIENT *****/

    val identifier = UUID.randomUUID().toString

    val builder = new MqttRxClientBuilder()
      .identifier(identifier)
      .serverHost(mqttHost)
      .serverPort(mqttPort)

    /* Transport layer security */

    val sslConfig = getMqttSslConfig
    if (sslConfig != null) builder.sslConfig(sslConfig)

    /* Application layer security */

    val simpleAuth = new MqttSimpleAuthBuilder.Default()
      .username(mqttUser)
      .password(mqttPass.getBytes(UTF8))
      .build()

    builder.simpleAuth(simpleAuth)

    /***** CONNECT *****/

    mqtt5Client = Some(builder.buildAsync())
    mqtt5Client.get
      .connectWith()
      .keepAlive(mqttKeepAlive)
      .send()
      .whenComplete(onMqtt5Connect)

  }

  private val mqtt5Callback = new java.util.function.Consumer[Mqtt5Publish] {

    def accept(publish: Mqtt5Publish):Unit = {

      val topic = publish.getTopic.toString
      val payload = publish.getPayloadAsBytes
      if (payload != null) {

        val qos = publish.getQos.ordinal()

        /* NOT SUPPORTED */
        val duplicate = false

        val retained = publish.isRetain
        expose.messageArrived(topic, payload, qos, duplicate, retained)

      }

    }

  }

  private def listenToMqtt5(topics:Array[String]):Unit = {

    if (!connected || mqtt3Client.isEmpty) {
      throw new Exception("No connection to HiveMQ broker established")
    }
    /*
     * The HiveMQ client supports a single topic; the publish
     * interface, however, accept a list of topics to increase
     * use flexibility
     */
    val mqttTopic = getMqttTopic(topics)

    /***** SUBSCRIBE *****/

    /*
     * Define the subscription callback, and, log the
     * respective results
     */
    val onSubscription = new java.util.function.BiConsumer[Mqtt5SubAck, Throwable] {

      def accept(connAck: Mqtt5SubAck, throwable: Throwable):Unit = {

        /* Error handling */
        if (throwable != null) {
          log.error("Subscription failed: " + throwable.getLocalizedMessage)

        } else {
          log.debug("Subscription successful")

        }

      }

    }

    mqtt5Client.get
      .subscribeWith()
      .topicFilter(mqttTopic)
      .qos(getMqttQoS)
      .callback(mqtt5Callback)
      .send()
      .whenComplete(onSubscription)

  }

  /** HELPER METHODS **/

  /*
   * This method evaluates the provided topics and
   * joins them into a single topic for connecting
   * and subscribing to the HiveMQ clients
   */
  private def getMqttTopic(topics:Array[String]): String = {

    if (topics.isEmpty)
      throw new Exception("The topics must not be empty.")

    if (topics.length == 1)
      topics(0)

    else {

      /*
       * We expect a set of topics that differ at
       * the lowest level only
       */
      var levels = 0
      var head = ""

      topics.foreach(topic => {

        val tokens = topic.split("\\/").toList

        /* Validate levels */
        val length = tokens.length
        if (levels == 0) levels = length

        if (levels != length)
          throw new Exception("Supported MQTT topics must have the same levels.")

        /* Validate head */
        val init = tokens.init.mkString
        if (head.isEmpty) head = init

        if (head != init)
          throw new Exception("Supported MQTT topics must have the same head.")

      })
      /*
       * Merge MQTT topics with the same # of levels
       * into a single topic by replacing the lowest
       * level with a placeholder '#'
       */
      val mqttTopic = {

        val tokens = topics(0).split("\\/").init ++ Array("#")
        tokens.mkString("/")

      }

      mqttTopic

    }

  }

  private def getMqttQoS: MqttQos = {

    val qos = mqttQoS.getOrElse(1)
    qos match {
      case 0 =>
        /*
         * QoS for at most once delivery according to the
         * capabilities of the underlying network.
         */
        MqttQos.AT_MOST_ONCE
      case 1 =>
        /*
         * QoS for ensuring at least once delivery.
         */
        MqttQos.AT_LEAST_ONCE
      case 2 =>
        /*
         * QoS for ensuring exactly once delivery.
         */
        MqttQos.EXACTLY_ONCE
      case _ => throw new RuntimeException(s"Quality of Service '$qos' is not supported.")
    }

  }

  /* Transport layer security */
  private def getMqttSslConfig: MqttClientSslConfig = {

    if (mqttSsl.isDefined) {

      Security.addProvider(new BouncyCastleProvider())

      val sslOptions = mqttSsl.get
      val builder = MqttClientSslConfig.builder()

      /* CipherSuites */
      val cipherSuites = sslOptions.getCipherSuites
      if (cipherSuites != null)
        builder.cipherSuites(cipherSuites)

      /* Hostname verifier */
      builder.hostnameVerifier(sslOptions.getHostnameVerifier)

      /* Key manager factory */
      val keyManagerFactory = sslOptions.getKeyManagerFactory
      if (keyManagerFactory != null)
        builder.keyManagerFactory(keyManagerFactory)

      /* Trust manager factory */
      val trustManagerFactory = sslOptions.getTrustManagerFactory
      if (trustManagerFactory != null)
        builder.trustManagerFactory(trustManagerFactory)


      builder.build()

    } else null

  }

  /** PUBLIC METHODS **/

  def connect(): Unit = {
    /*
     * HiveMQ supports MQTT version 5 as well as version 3;
     * default is version 3
     */
    val version = mqttVersion.getOrElse(3)
    if (version == 3)
      connectToMqtt3()

    else
      connectToMqtt5()

  }

  def isConnected:Boolean = connected

  def listen(topics:Array[String]):Unit = {
    /*
     * HiveMQ supports MQTT version 5 as well
     * as version 3; default is version 3
     */
    val version = mqttVersion.getOrElse(3)
    if (version == 3)
      listenToMqtt3(topics)

    else
      listenToMqtt5(topics)

  }

  def disconnect():Unit = {
    /*
     * HiveMQ supports MQTT version 5 as well as version 3;
     * default is version 3
     */
    val version = mqttVersion.getOrElse(3)
    if (version == 3) {
      mqtt3Client.get.disconnect()

    } else
      mqtt5Client.get.disconnect()

  }
}
