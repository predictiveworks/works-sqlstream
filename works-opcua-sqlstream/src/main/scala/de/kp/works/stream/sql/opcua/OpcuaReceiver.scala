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
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.UaClient
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.{UaSubscription, UaSubscriptionManager}
import org.eclipse.milo.opcua.stack.core.UaException
import org.eclipse.milo.opcua.stack.core.types.builtin.{DateTime, StatusCode}
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil

import java.util.concurrent.{CompletableFuture, Future}
import java.util.function.{BiConsumer, Consumer}

class OpcuaReceiver(options:OpcuaOptions) extends Logging {

  private var opcUaClient:OpcUaClient = _
  private var subscription:UaSubscription = _

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

  def shutdown():Future[Boolean] = {
    disconnect()
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

  private def createSubscription(): Unit = {
  }
  /**
   * Method restricts endpoints to those that either
   * have no security policy implemented or a policy
   * the refers to configured policy.
   */
  private def endpointFilter(e:EndpointDescription):Boolean = {
    val securityPolicy = options.getSecurityPolicy
    securityPolicy == null || securityPolicy.getUri.equals(e.getSecurityPolicyUri)
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
