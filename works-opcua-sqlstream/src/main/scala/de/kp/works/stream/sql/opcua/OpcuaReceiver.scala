package de.kp.works.stream.sql.opcua

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

import de.kp.works.stream.sql.Logging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.{OpcUaClientConfig, OpcUaClientConfigBuilder}
import org.eclipse.milo.opcua.sdk.client.api.{ServiceFaultListener, UaClient}
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.{UaSubscription, UaSubscriptionManager}
import org.eclipse.milo.opcua.stack.core.UaException
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.types.builtin.{DateTime, LocalizedText, StatusCode}
import org.eclipse.milo.opcua.stack.core.types.structured.{EndpointDescription, ServiceFault}
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil

import java.security.Security
import java.util.Optional
import java.util.concurrent.{CompletableFuture, Future}
import java.util.function.{BiConsumer, Consumer, Function}
import scala.annotation.tailrec
import scala.collection.JavaConversions.asScalaBuffer

class OpcuaReceiver(options:OpcuaOptions) extends Logging {

  private var opcUaClient:OpcUaClient = _
  private var subscription:UaSubscription = _

  private val opcuaClientInfo = options.getClientInfo

  private val clientId = opcuaClientInfo.clientId
  private val uri = "opc/" + clientId

  private var opcuaHandler:OpcuaHandler = _

  private val identityProvider = options.getIdentityProvider
  private val subscribeOnStartup = options.getTopics

  private val opcUaSecurity = new OpcuaSecurity(options)
  /*
   * Initialize subscription listener to monitor
   * subscription status
   */
  private val subscriptionListener = new UaSubscriptionManager.SubscriptionListener() {

    override def onKeepAlive(subscription: UaSubscription, publishTime: DateTime): Unit = {
      /* Do nothing */
    }

    override def onStatusChanged(subscription: UaSubscription, status: StatusCode): Unit = {
      log.info("Status changed: " + status.toString)
    }

    override def onPublishFailure(exception: UaException): Unit = {
      log.warn("Publish failure: " + exception.getMessage)
    }

    override def onNotificationDataLost(subscription: UaSubscription): Unit = {
      log.warn("Notification data lost: " + subscription.getSubscriptionId)
    }

    override def onSubscriptionTransferFailed(subscription: UaSubscription, statusCode: StatusCode): Unit = {
      log.warn("Subscription transfer failed: " + statusCode.toString)
      /*
       * Re-create subscription
       */
      createSubscription()
    }

  }
  /**
   * This method is used to retrieve data from the
   * UA server and send to output channels
   */
  def setOpcuaHandler(handler:OpcuaHandler):OpcuaReceiver = {
    opcuaHandler = handler
    this
  }

  /**
   * This is the main method to connect to the OPC-UA
   * server and subscribe.
   */
  def start():Boolean = {

    var started = true
    try {

      val connected = connect()
      if (connected) {
        subscribeOnStartup.foreach(pattern => {
          /*
           * node/ns=2;s=ExampleDP_Float.ExampleDP_Arg1
           * node/ns=2;s=ExampleDP_Text.ExampleDP_Text1
           * path/Objects/Test/+/+
           */
          val topic = OpcuaTransform.parse(uri + "/" + pattern)
          if (topic.isValid) {

            val subscriber = new OpcuaSubscriber(opcUaClient, options, subscription, opcuaHandler)
            try {
              val subscribed = subscriber.subscribeTopic(clientId, topic).get()
              if (!subscribed) started = false

            } catch {
              case t:Throwable =>
                log.error("Starting OPC-UA connection failed with: " + t.getLocalizedMessage)
                started = false
            }
          }
        })

      } else started = false

    } catch {
      case t:Throwable =>
        log.error("Starting OPC-UA connection failed with: " + t.getLocalizedMessage)
        started = false
    }

    started

  }

  def shutdown():Future[Boolean] = {
    disconnect()
  }

  private def connect():Boolean = {

    var connected = true
    try {
      /*
       * STEP #1: Create OPC-UA client
       */
      val created = createClientAsync()
      if (!created) connected = false
      else {
        /*
         * STEP #2: Connect to OPC-UA server
         */
        val result = connectClientAsync().get()
        if (!result) connected = false
        else {
          /*
           * STEP #3: Add fault & subscription listener
           */
          opcUaClient.addFaultListener(new ServiceFaultListener() {
            override def onServiceFault(serviceFault: ServiceFault): Unit = {
              log.warn("[OPC-UA FAULT] " + serviceFault.toString)
            }
          })

          opcUaClient.getSubscriptionManager
            .addSubscriptionListener(subscriptionListener)
          /*
           * STEP #4: Create subscription
           */
          createSubscription()

        }
      }

    } catch {
      case _:Throwable =>
        connected = false
    }

    connected
  }

  private def disconnect():Future[Boolean] = {

    val future = new CompletableFuture[Boolean]()
    opcUaClient
      .disconnect()
      .thenAccept(new Consumer[OpcUaClient] {
        override def accept(c: OpcUaClient): Unit = {
          future.complete(true)
        }
      })

    future

  }

  private def createClientAsync():Boolean = createClientWithRetry()

  /**
   * This method creates an OPC-UA client with a retry mechanism
   * in case an error occurred; the current implementation retries
   * after 5000 ms with a maximum of 10 retries
   */
  private def createClientWithRetry():Boolean = {

    var created = true
    try {
      opcUaClient = createClient()

    } catch {
      case _:UaException =>
        Thread.sleep(options.getRetryWait)
        created = createClientWithRetry()

      case _:Exception =>
        created = false

    }

    created
  }

  private def createClient():OpcUaClient = {

    Security.addProvider(new BouncyCastleProvider())

    val selectEndpoint = new Function[java.util.List[EndpointDescription], Optional[EndpointDescription]] {
      override def apply(endpoints: java.util.List[EndpointDescription]): Optional[EndpointDescription] = {
        Optional.of[EndpointDescription](
          endpoints
            /*
             * Restricts endpoints to those that either have
             * no security policy implemented or a policy that
             * refers to the configured policy.
             */
            .filter(e => endpointFilter(e))
            .map(e => endpointUpdater(e))
            .head)
      }
    }

    val buildConfig = new Function[OpcUaClientConfigBuilder, OpcUaClientConfig] {
      override def apply(configBuilder: OpcUaClientConfigBuilder): OpcUaClientConfig = {

        configBuilder
          .setApplicationName(LocalizedText.english(OpcuaUtils.APPLICATION_NAME))
          .setApplicationUri(OpcuaUtils.APPLICATION_URI)
          .setIdentityProvider(identityProvider)
          .setKeyPair(opcUaSecurity.getClientKeyPair)
          .setCertificate(opcUaSecurity.getClientCertificate)
          .setConnectTimeout(UInteger.valueOf(opcuaClientInfo.connectTimeout))
          .setRequestTimeout(UInteger.valueOf(opcuaClientInfo.requestTimeout))
          .setKeepAliveFailuresAllowed(UInteger.valueOf(opcuaClientInfo.keepAliveFailuresAllowed))
          .build()

      }
    }

    OpcUaClient.create(opcuaClientInfo.endpointUrl, selectEndpoint, buildConfig)

  }

  private def createSubscription(): Unit = {

    opcUaClient
      .getSubscriptionManager
      .createSubscription(opcuaClientInfo.subscriptionSamplingInterval)
      .whenCompleteAsync(new BiConsumer[UaSubscription, Throwable] {
        override def accept(s: UaSubscription, e: Throwable): Unit = {
          if (e == null) {
            subscription = s
            try resubscribe()
            catch {
              case ex: Exception =>
                log.error("Unable to re-subscribe. Fail with: " + ex.getLocalizedMessage)
            }
          }
          else {
            log.error("Unable to create a subscription. Fail with: " + e.getLocalizedMessage)
          }
        }
      })

  }

  private def resubscribe():Unit = {

    val topics = OpcuaRegistry.getTopics
    if (topics.nonEmpty) {
      /*
       * Delete topics from registry
       */
      topics.foreach(topic => OpcuaRegistry.delTopic(topic))
      /*
       * Note: the subscriber adds the topics to
       * the registry again
       */
      val subscriber = new OpcuaSubscriber(opcUaClient, options, subscription, opcuaHandler)
      subscriber.subscribeTopics(topics)

    }
  }

  /**
   * __create_client__
   *
   * Method restricts endpoints to those that either
   * have no security policy implemented or a policy
   * that refers to the configured policy.
   */
  private def endpointFilter(e:EndpointDescription):Boolean = {
    val securityPolicy = options.getSecurityPolicy
    securityPolicy == null || securityPolicy.getUri.equals(e.getSecurityPolicyUri)
  }
  /**
   * __create_client__
   */
  private def endpointUpdater(e:EndpointDescription ):EndpointDescription = {

    if (!opcuaClientInfo.updateEndpointUrl) return e
    /*
     * "opc.tcp://desktop-9o6hthf:4890"; // WinCC UA NUC
     * "opc.tcp://desktop-pc4fa6r:4890"; // WinCC UA Laptop
     * "opc.tcp://centos1.predictiveworks.local:4840"; // WinCC OA
     * "opc.tcp://ubuntu1:62541/discovery" // Ignition
     */
    val parts1 = opcuaClientInfo.endpointUrl.split("://")
    if (parts1.length != 2) {
      log.warn("Provided endpoint url :" + opcuaClientInfo.endpointUrl + " cannot be split.")
      return e
    }
    val parts2 = parts1(1).split(":")
    if (parts2.length == 1) {
      EndpointUtil.updateUrl(e, parts2(0))
    }
    else if (parts2.length == 2) {
      EndpointUtil.updateUrl(e, parts2(0), Integer.parseInt(parts2(1)))
    }
    else {
      log.warn("Provided endpoint url :" + opcuaClientInfo.endpointUrl + " cannot be split.")
      e
    }
  }

  private def connectClientAsync():Future[Boolean] = {
    val future = new CompletableFuture[Boolean]()
    connectClientWithRetry(future)
    future
  }

  private def connectClientWithRetry(future:CompletableFuture[Boolean]):Unit = {

    if (opcUaClient == null) {
      future.complete(false)

    } else {
      opcUaClient
        .connect()
        .whenComplete(new BiConsumer[UaClient, Throwable] {
          override def accept(c: UaClient, t: Throwable): Unit = {
            if (t == null) {
              future.complete(true)
            }
            else {
              try {
                Thread.sleep(options.getRetryWait)
                connectClientWithRetry(future)

              } catch {
                case _:Throwable =>
                  future.complete(false)
              }

            }
          }
        })

    }

  }
}
