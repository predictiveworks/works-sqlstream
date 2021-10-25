package de.kp.works.stream.sql.transform.opencti

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

import com.google.gson.JsonElement
import de.kp.works.stream.sql.transform.{BaseTransform, Beats}
import org.apache.spark.sql.Row

object CTITransform extends BaseTransform {

  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {
    /*
      * Validate whether the provided event type
      * refers to the support format for OpenCTI
      * events:
      *
      *      beat/opencti/<operation>
      */
    val tokens = eventType.split("\\/")
    if (tokens.size != 3)
      throw new Exception("Unknown format for event type detected.")

    if (tokens(0) != Beats.OPENCTI.toString)
      throw new Exception("The event type provided does not describe an OpenCTI event.")

    val operation = tokens(2)
    operation match {
      case "create" | "delete" | "update" =>

        try {

          val event = mapper.readValue(eventData.toString, classOf[Map[String, Any]])

          val entityId = event("id").asInstanceOf[String]
          val entityType = event("type").asInstanceOf[String]

          val rows = event
            .filter{case(k,_) => k != "id" && k != "type"}
            .map{case(k, v) =>

              val attrName = k
              val attrObj = v.asInstanceOf[Map[String, Any]]
              val metadata = attrObj.getOrElse("metadata", "NULL").asInstanceOf[String]

              val attrType = attrObj.getOrElse("type", "NULL").asInstanceOf[String]
              val attrValu = attrObj.get("value") match {
                case Some(value) => mapper.writeValueAsString(value)
                case _ => ""
              }
              val values = Seq(
                entityId,
                entityType,
                attrName,
                attrType,
                attrValu,
                metadata)

              Row.fromSeq(values)
            }
            .toSeq

          Some(rows)

        } catch {
          case _:Throwable => None
        }
      case _ =>
        throw new Exception(s"Unknown operation detected.")
    }

  }

}
