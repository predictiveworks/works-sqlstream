package de.kp.works.stream.sql.pubsub

/**
 * Copyright (c) 2019 - 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.pubsub.Pubsub
import com.google.api.services.pubsub.Pubsub.Builder
import com.google.api.services.pubsub.model.{AcknowledgeRequest, PullRequest, Subscription}
import com.google.cloud.hadoop.util.RetryHttpInitializer
import de.kp.works.stream.sql.Logging

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

object ConnectionUtils {

  val transport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
  val jacksonFactory: JacksonFactory = JacksonFactory.getDefaultInstance
  /**
   * The topic or subscription already exists. This is an error
   * on creation operations.
   */
  val ALREADY_EXISTS = 409
  /**
   * Client can retry with these response status
   */
  val RESOURCE_EXHAUSTED = 429

  val CANCELLED = 499
  val INTERNAL = 500

  val UNAVAILABLE = 503
  val DEADLINE_EXCEEDED = 504

  def retryable(status: Int): Boolean = {

    status match {
      case RESOURCE_EXHAUSTED | CANCELLED | INTERNAL | UNAVAILABLE | DEADLINE_EXCEEDED => true
      case _ => false
    }

  }
}

class PubSubReceiver(options:PubSubOptions, handler:PubSubHandler) extends Logging {

  private var stopped = false

  val projectFullName: String = options.getProjectName
  val subscriptionFullName: String = options.getSubscriptionName
  /**
   * Construction of the PubSub client
   */
  lazy val client: Pubsub = {

    val builder = new Builder(
      ConnectionUtils.transport,
      ConnectionUtils.jacksonFactory,
      new RetryHttpInitializer(options.getCredential, options.getAppName)
    )

    builder
      .setApplicationName(options.getAppName)
      .build()

  }

  def start(): Unit = {

    options.getTopicName match {
      case Some(t) =>
        val sub: Subscription = new Subscription
        sub.setTopic(t)
        try {

          client
            .projects()
            .subscriptions()
            .create(subscriptionFullName, sub)
            .execute()

        } catch {

          case e: GoogleJsonResponseException =>
            if (e.getDetails.getCode == ConnectionUtils.ALREADY_EXISTS) {

              /* Ignore subscription already exists exception. */

            } else {
              log.error("Failed to create subscription", e)
            }
          case NonFatal(e) =>
            log.error("Failed to create subscription", e)
        }
      case None => /* Do nothing */
    }

    new Thread() {

      override def run() {
        receive()
      }

    }.start()

  }

  def stop(): Unit = {stopped = true}

  private def receive(): Unit = {

    val pullRequest = new PullRequest()
      .setMaxMessages(options.getMessageMax)
      .setReturnImmediately(false)

    var backoff = options.getBackoffInit
    while (!stopped) {

      try {

        val pullResponse = client
          .projects()
          .subscriptions()
          .pull(subscriptionFullName, pullRequest)
          .execute()

        val receivedMessages = pullResponse.getReceivedMessages
        if (receivedMessages != null) {

          val messages = receivedMessages.asScala
          val output = messages
            .filter(m => !m.isEmpty)
            .map(m => {

              val message = m.getMessage
              /*
               * Convert to a HashMap because com.google.api.client.util.ArrayMap
               * is not serializable.
               */
              val attributes = new java.util.HashMap[String,String]()
              attributes.putAll(message.getAttributes)

              PubSubEvent(
                id = message.getMessageId,
                publishTime = message.getPublishTime,
                attributes = attributes.asScala.toMap,
                data = message.decodeData()
              )

            })

          if (output.nonEmpty)
            handler.sendEvents(output)

          if (options.getAutoAck) {

            val ackRequest = new AcknowledgeRequest()
            ackRequest.setAckIds(messages.map(x => x.getAckId).asJava)

            client
              .projects()
              .subscriptions()
              .acknowledge(subscriptionFullName, ackRequest)
              .execute()
          }

        }

        backoff = options.getBackoffInit

      } catch {
        case e: GoogleJsonResponseException =>
          if (ConnectionUtils.retryable(e.getDetails.getCode)) {

            Thread.sleep(backoff)
            backoff = Math.min(backoff * 2, options.getBackoffMax)

          } else {
            log.error("Failed to pull messages", e)

          }
        case NonFatal(e) => log.error("Failed to pull messages", e)
      }
    }

  }

}
