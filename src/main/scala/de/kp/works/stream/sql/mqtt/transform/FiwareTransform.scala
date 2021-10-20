package de.kp.works.stream.sql.mqtt.transform
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
import org.apache.spark.sql.Row

object FiwareTransform {

  protected val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def transform(mqttEvent:String):Seq[Row] = {

    val deserialized = deserialize(mqttEvent)

    val service = deserialized("service").asInstanceOf[String]
    val servicePath = deserialized("servicePath").asInstanceOf[String]

    val payload = deserialized("payload").asInstanceOf[Map[String, Any]]
    /*
     * We expect 2 fields, `subscriptionId` and `data`
     */
    val subscription = payload("subscriptionId").asInstanceOf[String]
    val data = payload("data")

    data match {
      case values: List[_] =>
        val head = values.head
        head match {
          case _: Map[_, _] =>

            val entities = data.asInstanceOf[List[Map[String, _]]]
            entities.flatMap(entity => {

              val entityId = entity("id").asInstanceOf[String]
              val entityType = entity("type").asInstanceOf[String]

              val entityCtx = entity
                .getOrElse("@context", List("https://schema.lab.fiware.org/ld/context"))
                .asInstanceOf[List[String]]

              /*
               * Retrieve attributes
               */
              entity
                .filterKeys(x => x != "id" & x!= "type" )
                .map{case(k,v) =>

                  val attr = v.asInstanceOf[Map[String,Any]]

                  val attrName = k
                  val attrType = attr.getOrElse("type", "NULL").asInstanceOf[String]

                  val attrValu = attr.get("value") match {
                    case Some(v) => mapper.writeValueAsString(v)
                    case _ => ""
                  }

                  val metadata = attr.get("metadata") match {
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

                }

            })
          case _ =>
            throw new Exception(s"The provided entities must be a List.")
        }
      case _ =>
        throw new Exception(s"The provided entities must be a List.")
    }

  }

  private def deserialize(message:String):Map[String, Any] = {
    mapper.readValue(message, classOf[Map[String, Any]])
  }

}
