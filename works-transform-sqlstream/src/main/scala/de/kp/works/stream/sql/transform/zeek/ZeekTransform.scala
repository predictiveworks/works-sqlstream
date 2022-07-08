package de.kp.works.stream.sql.transform.zeek
/**
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

import com.google.gson.{JsonElement, JsonObject}
import de.kp.works.stream.sql.json.JsonUtil
import de.kp.works.stream.sql.transform.{BaseTransform, Beats}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StringType, StructField, StructType}

import scala.collection.JavaConversions.asScalaSet

object ZeekTransform extends BaseTransform {
  /*
   * This method transforms a Zeek log event that refer to
   * a certain log file into a single Apache Spark [Row].
   *
   * The method interface is harmonized with all other
   * transform methods and exposes a Seq[Row]
   */
  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {
    /*
     * Validate whether the provided event type
     * refers to the support format for Zeek log
     * files:
     *            beat/zeek/<entity>.log
     */
    val tokens = eventType.split("\\/")
    if (tokens.size != 3)
      throw new Exception("Unknown format for event type detected.")

    if (tokens(1) != Beats.ZEEK.toString)
      throw new Exception("The event type provided does not describe a Zeek event.")

    if (!tokens(2).endsWith(".log"))
      throw new Exception("The event type provided does not describe a Zeek log file.")
    /*
     * Extract log file name and determine schema
     * that refers to log file name
     */
    val file = tokens(2)
    /*
     * The provide Zeek event is published by the Zeek Beat
     * and is formatted in an NGSI compliant format.
     *
     * This implies that `entity_id` and `entity_type` are
     * additional columns to complement the Zeek schema
     */
    val zeekSchema = ZeekSchema.fromFile(file)
    if (zeekSchema == null) return None

    try {
      /*
       * Enrich schema with NGSI entity `id` and `type`
       */
      val schema = StructType(
        Array(
          StructField("entity_id",   StringType, nullable = false),
          StructField("entity_type", StringType, nullable = false)) ++ zeekSchema.fields)

      val entityJson = eventData.getAsJsonObject

      val entity_id = entityJson.remove("id")
      entityJson.add("entity_id", entity_id)

      val entity_type = entityJson.remove("type")
      entityJson.add("entity_type", entity_type)

      entityJson.keySet.foreach(key => {
        /*
         * NGSI attribute format
         *
         * {
         *   "<attribute-name>": {
         *     "metadata": {...},
         *     "type": "...",
         *     "value": ...
         *   }
         * }
         *
         * is flattened to "<attribute-name>": value
         *
         */
        if (!Seq("entity_id", "entity_type").contains(key)) {

          val attrValue = entityJson.remove(key)
            .getAsJsonObject.get("value")

          entityJson.add(key, attrValue)

        }
      })

      val row = JsonUtil.json2Row(entityJson, schema)
      Some(Seq(row))

    } catch {
      case _:Throwable => None
    }

  }

}
