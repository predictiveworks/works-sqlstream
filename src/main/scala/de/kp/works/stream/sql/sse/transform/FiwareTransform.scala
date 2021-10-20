package de.kp.works.stream.sql.sse.transform
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.gson.JsonElement
import org.apache.spark.sql.Row

import scala.collection.JavaConversions._

/**
 * [FiwareTransform] converts an unpacked SSE-based Fiware
 * notification into an Apache SQL compliant ROW, and thereby
 * follows the [FiwareSchema]
 */
object FiwareTransform extends BaseTransform {

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def fromValues(eventType:String, eventData:JsonElement):Option[Seq[Row]] = {
    try {
      /*
       * The `eventType` parameter is not used here, as it specifies
       * a Fiware notification without providing further details
       */
      val jsonObj = eventData.getAsJsonObject

      val service = jsonObj.get("service").getAsString
      val servicePath = jsonObj.get("servicePath").getAsString

      val payload = jsonObj.get("payload").getAsJsonObject
      /*
       * We expect 2 fields, `subscriptionId` and `data`
       */
      val subscription = payload.get("subscriptionId").getAsString
      val entities = jsonObj.get("data").getAsJsonArray

      val rows = entities.flatMap(elem => {

        val entity = elem.getAsJsonObject

        val entityId = entity.get("id").getAsString
        val entityType = entity.get("type").getAsString

        val entityCtx =
          if (entity.has("@context"))
            entity.get("@context").getAsJsonArray.map(_.getAsString)

          else
            List("https://schema.lab.fiware.org/ld/context")

        /*
         * Extract attributes from entity
         */
        val excludes = List("id", "type")
        val attrNames = entity.keySet().filter(attrName => !excludes.contains(attrName))

        attrNames.map(attrName => {
          /*
           * Leverage Jackson mapper to determine the data type of the
           * provided value
           */
          val attrObj = mapper.readValue(entity.get(attrName).toString, classOf[Map[String, Any]])

          val attrType = attrObj.getOrElse("type", "NULL").asInstanceOf[String]
          val attrValu = attrObj.get("value") match {
            case Some(v) => mapper.writeValueAsString(v)
            case _ => ""
          }

          val metadata = attrObj.get("metadata") match {
            case Some(v) => mapper.writeValueAsString(v)
            case _ => ""
          }

          val values = Seq(
            subscription,
            service,
            servicePath,
            entityId,
            entityType,
            attrName,
            attrType,
            attrValu,
            metadata,
            entityCtx)

          Row.fromSeq(values)

        })

      })
      .toSeq

      Some(rows)

    } catch {
      case _:Throwable => None
    }
  }

}
