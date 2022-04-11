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
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.{UaMonitoredItem, UaSubscription}
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.types.builtin.{DataValue, ExtensionObject, NodeId, QualifiedName}
import org.eclipse.milo.opcua.stack.core.types.enumerated.{DataChangeTrigger, DeadbandType, MonitoringMode, TimestampsToReturn}
import org.eclipse.milo.opcua.stack.core.types.structured.{DataChangeFilter, MonitoredItemCreateRequest, MonitoringParameters, ReadValueId}

import java.util
import java.util.concurrent.{CompletableFuture, Future}
import java.util.function.{BiConsumer, Consumer}
import scala.collection.JavaConversions._

class OpcuaSubscriber(client: OpcUaClient, options: OpcuaOptions,
                      subscription: UaSubscription, outputHandler: OpcuaHandler) extends Logging {

  private val caches = new OpcuaCaches(client, options)
  /*
   * Unpack monitoring configuration
   */
  private val monitorInfo = options.getMonitorInfo
  private val dataChangeTrigger = DataChangeTrigger.valueOf(monitorInfo.dataChangeTrigger)
  /**
   * Subscribe a list of nodes (in contrast to paths)
   */
  private def subscribeNodes(topics: List[OpcuaTopic]): CompletableFuture[Boolean] = {

    val future: CompletableFuture[Boolean] = new CompletableFuture[Boolean]
    if (topics.isEmpty) {
      future.complete(true)

    }
    else {

      var requests: List[MonitoredItemCreateRequest] = List.empty[MonitoredItemCreateRequest]

      val nodeIds: List[NodeId] = topics
        .map(topic => NodeId.parseOrNull(topic.address))

      val filter: ExtensionObject =
        ExtensionObject.encode(
          client.getStaticSerializationContext,
          new DataChangeFilter(dataChangeTrigger, UInteger.valueOf(DeadbandType.None.getValue), 0.0))

      nodeIds.foreach(nodeId => {
        /*
         * IMPORTANT: The client handle must be unique per item within
         * the context of a subscription. The UaSubscription's client
         * handle sequence is provided as a convenience.
         */
        val clientHandle: UInteger = subscription.nextClientHandle
        val readValueId: ReadValueId = new ReadValueId(nodeId, AttributeId.Value.uid, null, QualifiedName.NULL_VALUE)

        val parameters = new MonitoringParameters(
          clientHandle, monitorInfo.samplingInterval, filter, UInteger.valueOf(monitorInfo.bufferSize), monitorInfo.discardOldest)

        val request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters)
        requests ++= List(request)

      })
      /*
       * When creating items in MonitoringMode.Reporting this callback is where
       * each item needs to have its value/event consumer hooked up. The alternative
       * is to create the item in sampling mode, hook up the consumer after the creation
       * call completes, and then change the mode for all items to reporting.
       */

      val onItemCreated: UaSubscription.ItemCreationCallback = new UaSubscription.ItemCreationCallback {
        override def onItemCreated(item: UaMonitoredItem, id: Int): Unit = {

          val topic: OpcuaTopic = topics.get(id)
          if (item.getStatusCode.isGood) {
            OpcuaRegistry.addMonitoredItem(MonitoredItem(item), topic)
          }
          val valueConsumer = new Consumer[DataValue] {
            override def accept(data: DataValue): Unit = {
              consumeValue(topic, data)
            }
          }
          item.setValueConsumer(valueConsumer)

        }
      }

      subscription
        .createMonitoredItems(TimestampsToReturn.Both, requests, onItemCreated)
        .thenAccept(new Consumer[util.List[UaMonitoredItem]] {
          override def accept(monitoredItems: util.List[UaMonitoredItem]): Unit = {
            try {
              monitoredItems.foreach(monitoredItem => {
                if (monitoredItem.getStatusCode.isGood) {
                  log.debug("Monitored item created for nodeId: " + monitoredItem.getReadValueId.getNodeId)
                }
                else {
                  log.warn("Failed to create monitored item for nodeId: " + monitoredItem.getReadValueId.getNodeId + " with status code: " + monitoredItem.getStatusCode.toString)
                }

              })

              future.complete(true)

            } catch {
              case e: Exception =>
                log.error(e.getLocalizedMessage)
                future.complete(false)
            }

          }
        })

    }
    future
  }

  /**
   * The support to subscribe by (browse) path is implemented
   * in 2 steps: first the address cache is used to retrieve
   * the [NodeId]s that refer to the provided path-based
   * `address`.
   *
   * Then address and browse path is modified.
   */
  private def subscribePath(topics: List[OpcuaTopic]): CompletableFuture[Boolean] = {

    val future: CompletableFuture[Boolean] = new CompletableFuture[Boolean]
    if (topics.isEmpty) {
      future.complete(true)
    }
    else {

      try {

        var resolvedTopics: List[OpcuaTopic] = List.empty[OpcuaTopic]
        topics.foreach(topic => {
          resolvedTopics ++= caches.resolveTopic(topic)
        })

        if (resolvedTopics.isEmpty) {
          future.complete(false)
        }
        else {
          val res: Boolean = subscribeNodes(resolvedTopics).get
          future.complete(res)
        }
      } catch {
        case e: Exception =>
          log.error(e.getLocalizedMessage)
          future.complete(false)
      }
    }

    future
  }

  def subscribeTopic(clientId: String, topic: OpcuaTopic): Future[Boolean] = {

    val future: CompletableFuture[Boolean] = new CompletableFuture[Boolean]
    try {

      val tuple = OpcuaRegistry.addClient(clientId, topic)

      val added: Boolean = tuple._2.asInstanceOf[Boolean]
      val count: Int = tuple._1.asInstanceOf[Int]

      if (!added) {
        future.complete(true)
      }
      else {
        if (count == 1) {
          val res: Boolean = subscribeTopics(List(topic)).get
          future.complete(res)
        }
        else {
          future.complete(true)
        }
      }
    } catch {
      case e: Exception =>
        log.error(e.getLocalizedMessage)
        future.complete(false)
    }
     future
  }

  def subscribeTopics(topics: List[OpcuaTopic]): Future[Boolean] = {

    val future: CompletableFuture[Boolean] = new CompletableFuture[Boolean]
    /*
     * Distinguish between node (id) based and
     * browse path based topics
     */
    val nodeTopics = topics.filter(topic => topic.topicType == OpcuaTopicType.NodeId)
    val pathTopics = topics.filter(topic => topic.topicType == OpcuaTopicType.Path)

    CompletableFuture
      .allOf(subscribeNodes(nodeTopics), subscribePath(pathTopics))
      .whenComplete(new BiConsumer[Void, Throwable] {
        override def accept(ignore: Void, error: Throwable): Unit = {
          if (error == null) {
            future.complete(true)
          }
          else {
            future.complete(false)
          }

        }
      })

    future

  }

  def unsubscribeTopics(topics: List[OpcuaTopic], items: List[MonitoredItem]): Future[Boolean] = {

    val future: CompletableFuture[Boolean] = new CompletableFuture[Boolean]
    try {

      if (items.nonEmpty) {

        val opcUaItems: List[UaMonitoredItem] = items.map(item => item.item)
        subscription.deleteMonitoredItems(opcUaItems)

      }

      future.complete(true)

    } catch {
      case e: Exception =>
        log.error(e.getLocalizedMessage)
        future.complete(false)
    }

    future

  }

  private def consumeValue(dataTopic: OpcuaTopic, dataValue: DataValue): Unit = {

    try {

      val opcuaEvent = OpcuaTransform.transform(dataTopic, dataValue)
      outputHandler.sendOpcuaEvent(Some(opcuaEvent))

    } catch {
      case _: Exception =>
        outputHandler.sendOpcuaEvent(None)

    }
  }

}
