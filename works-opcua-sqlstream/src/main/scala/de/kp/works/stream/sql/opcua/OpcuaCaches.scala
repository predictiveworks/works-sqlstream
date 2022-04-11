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

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.google.gson._
import de.kp.works.stream.sql.Logging
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.types.enumerated.{BrowseDirection, BrowseResultMask, NodeClass}
import org.eclipse.milo.opcua.stack.core.types.structured.{BrowseDescription, ReferenceDescription}
import org.eclipse.milo.opcua.stack.core.{BuiltinReferenceType, Identifiers}

import java.util.concurrent.{ExecutionException, TimeUnit}
import scala.collection.JavaConversions._
import scala.collection.mutable

class OpcuaCaches(client:OpcUaClient, options:OpcuaOptions) extends Logging {

  private val cacheInfo = options.getAddressCacheInfo
  private val cacheLoader = new CacheLoader[String, List[(NodeId, String)]]() {

    override def load(id: String):List[(NodeId, String)] = {
      browseAddress(id)
    }

  }
  private val addressCache = CacheBuilder.newBuilder()
    .maximumSize(cacheInfo.maximumSize)
    .expireAfterAccess(cacheInfo.expireAfterSeconds, TimeUnit.SECONDS)
    .build[String, List[(NodeId, String)]](cacheLoader)

  /*
   * The `address` represents the browse path
   */
  private def browseAddress(address: String): List[(NodeId, String)] = {

    val resolvedNodeIds = mutable.ArrayBuffer.empty[(NodeId, String)]
    /* Objects/Test/+/+ */
    val items:Seq[String] = OpcuaTransform.splitAddress(address)
    /*
     * INLINE
     */
    def findInline(node:String, index:Int, path:String) {

      val item = items(index)

      val nodeId = NodeId.parseOrNull(node)
      if (nodeId != null) {

        val result = jsonArrayToList(browseNode(nodeId)).filter(jsonObject => {
          item == "#" || item == "+" || item == jsonObject.get("browseName").getAsString
        })

        val nextIdx = if (item != "#" && index + 1 < items.size) index + 1 else index

        result.foreach(jsonObject => {

          val childNodeId = NodeId
            .parseOrNull(jsonObject.get("nodeId").getAsString)

          val browseName = jsonObject.get("browseName").getAsString
          val browsePath = path + "/" + browseName

          if (childNodeId != null) {
            val nodeClass = jsonObject.get("nodeClass").getAsString
            nodeClass match {
              case "Object" =>
                findInline(jsonObject.get("nodeId").getAsString, nextIdx, browsePath)
              case "Variable" => resolvedNodeIds += ((childNodeId, browsePath))
              case _ =>
            }
          }

        })
      }

    }

    val start = OpcuaUtils.getRootNodeIdOfName(items.head)
    findInline(start, 1, items.head)

    resolvedNodeIds.toList
  }

  private def browseNode(nodeId: NodeId, maxLevel: Int = 1, flat: Boolean = false, reverse: Boolean = false): JsonArray = {
    /*
     * INLINE
     */
    def browseInline(startNodeId: NodeId, maxLevel: Int, level: Int, flat: Boolean, path: String=""): JsonArray = {

      val result = new JsonArray
      /*
       * INLINE
       */
      def addResultInline(references: List[ReferenceDescription]): Unit = {

        references.filter(rd => {
          val typeId = rd.getReferenceTypeId

          typeId == BuiltinReferenceType.Organizes.getNodeId    ||
          typeId == BuiltinReferenceType.HasComponent.getNodeId ||
          typeId == BuiltinReferenceType.HasProperty.getNodeId
        })
        .foreach(rd => {

          val item = new JsonObject
          val name = rd.getBrowseName.getName

          item.addProperty("browseName", name)
          item.addProperty("browsePath", path + name)

          item.addProperty("displayName", rd.getDisplayName.getText)

          item.addProperty("nodeId", rd.getNodeId.toParseableString)
          item.addProperty("nodeClass", rd.getNodeClass.toString)

          if (rd.getNodeClass == NodeClass.Variable || !flat) result.add(item)

          /*
           * Recursively browse to children if it is an object node
           */
          if ((maxLevel == -1 || level < maxLevel) && rd.getNodeClass == NodeClass.Object) {

            val namespaceTable = client.getNamespaceTable
            val rdNodeId = rd.getNodeId.toNodeId(namespaceTable)

            if (rdNodeId.isPresent) {

              val next = browseInline(rdNodeId.get(), maxLevel, level + 1, flat, path + name + "/")
              if (flat) {
                result.addAll(next)

              } else {
                item.add("nodes", next)
              }
            }
          }

        })
      } /* addResultInternal */

      /* BITWISE OR OPERATION */
      val nodeClassMask = NodeClass.Object.getValue | NodeClass.Variable.getValue

      val browseDescription = new BrowseDescription(
        startNodeId,
        if (reverse) BrowseDirection.Inverse else BrowseDirection.Forward,
        Identifiers.References,
        true,
        UInteger.valueOf(nodeClassMask),
        UInteger.valueOf(BrowseResultMask.All.getValue)
      )

      try {

        val browseResult = client.browse(browseDescription).get()

        if (browseResult.getStatusCode.isGood && browseResult.getReferences != null) {

          addResultInline(browseResult.getReferences.toList)

          var continuationPoint = browseResult.getContinuationPoint
          while (continuationPoint != null && continuationPoint.isNotNull) {

            val nextResult = client.browseNext(false, continuationPoint).get()
            addResultInline(browseResult.getReferences.toList)

            continuationPoint = nextResult.getContinuationPoint

          }
        }
        else {
          log.error(s"Browsing nodeId [$startNodeId] failed [${browseResult.getStatusCode.toString}]")
        }

      } catch {
        case e: InterruptedException =>
          log.error(s"Browsing nodeId [$startNodeId] exception: [${e.getLocalizedMessage}]")
        case e: ExecutionException =>
          log.error(s"Browsing nodeId [$startNodeId] exception: [${e.getLocalizedMessage}]")
      }

      result

    } /* browseInline */

    browseInline(nodeId, maxLevel, 1, flat)

  }

  private def jsonArrayToList(jsonArray:JsonArray):List[JsonObject] = {
    val buffer = mutable.ArrayBuffer.empty[JsonObject]
    (0 until jsonArray.size).foreach(i => {
      buffer += jsonArray.get(i).getAsJsonObject
    })

    buffer.toList
  }

  def resolveTopic(topic:OpcuaTopic):java.util.List[OpcuaTopic] = {

    val result = addressCache.get(topic.address)
    result.map{case(nodeId, browsePath) =>
      OpcuaTopic(
        address = nodeId.toParseableString,
        browsePath = browsePath,
        topicName = topic.topicName,
        topicType = topic.topicType,
        systemName = topic.systemName)
    }

  }

}
