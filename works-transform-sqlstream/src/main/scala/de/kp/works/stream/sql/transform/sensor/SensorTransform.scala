package de.kp.works.stream.sql.transform.sensor
/**
 * Copyright (c) 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

import com.google.gson.{JsonArray, JsonElement}
import de.kp.works.stream.sql.transform.BaseTransform
import org.apache.spark.sql.Row

import scala.collection.JavaConversions.asScalaSet

object SensorTransform extends BaseTransform {

  override def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {

    try {

      val sensor = eventData.getAsJsonObject

      val sensorId = sensor.get("id").getAsString
      val sensorType = sensor.get("type").getAsString

      /*
        * Extract attributes from sensor
        */
      val excludes = List("id", "type")
      val attrNames = sensor.keySet().filter(attrName => !excludes.contains(attrName))

      val rows = attrNames.map(attrName => {
        /*
         * Leverage Jackson mapper to determine the
         * data type of the provided value
         */
        val attrObj = mapper.readValue(sensor.get(attrName).toString, classOf[Map[String, Any]])

        val attrType = attrObj.getOrElse("type", "NULL").asInstanceOf[String]
        val attrValu = attrObj.get("value") match {
          case Some(v) => mapper.writeValueAsString(v)
          case _ => ""
        }

        val values = Seq(
          sensorId,
          sensorType,
          attrName,
          attrType,
          attrValu)

        Row.fromSeq(values)

      }).toSeq

      Some(rows)

    } catch {
      case _: Throwable => None
    }
  }

}
/*

  def toJson:JsonObject = {
    /*
     * Information type as a regular NGSI attribute
     */
    val infoType = new JsonObject
    infoType.addProperty("type", "String")
    infoType.addProperty("value", sensorInfo.toString)

    json.add("infoType", infoType)

    sensorAttrs.foreach(sensorAttr => {

      val attr = new JsonObject
      attr.addProperty("type", sensorAttr.attrType)
      attr.addProperty("value", sensorAttr.attrValue)

      json.add(sensorAttr.attrName, attr)

    })

    json

  }

 */