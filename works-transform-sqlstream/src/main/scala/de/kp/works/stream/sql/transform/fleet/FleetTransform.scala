package de.kp.works.stream.sql.transform.fleet

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

import com.google.gson.{JsonElement, JsonObject}
import de.kp.works.stream.sql.json.JsonUtil
import de.kp.works.stream.sql.transform.{BaseTransform, Beats}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

import scala.collection.JavaConversions._
/**
 * [FleetTransform] is part of PredictiveWorks.
 * Cyber Analytics support. The event originates
 * either from [FleetBeat] MQTT or SSE channel:
 *
 * Fleet Sensor --> [FleetBeat] --> [FleetStream]
 */
object FleetTransform extends BaseTransform {

  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {
    /*
     * Validate whether the provided event type
     * refers to the support format for Fleet log
     * files:
     *
     *      beat/fleet/<table>
     */
    val tokens = eventType.split("\\/")
    if (tokens.size != 3)
      throw new Exception("Unknown format for event type detected.")

    if (tokens(0) != Beats.FLEET.toString)
      throw new Exception("The event type provided does not describe a Fleet event.")
    /*
     * This step determine whether the specified
     * table is known and refers to one of the
     * supported osquery tables.
     */
    val table = FleetTablesUtil.fromTable(tokens(2))
    if (table == null) return None

    if (table == FleetTables.OSQUERY_STATUS) {
      /*
       * For status events, the entire message is serialized
       * and provided as column value
       */
      val values = Seq(eventData.toString)
      val row = Row.fromSeq(values)

      Some(Seq(row))

    }
    else {
      /*
       * Retrieve the Fleet schema, as it is required to
       * convert each (batch) event object into a [Row]
       */
      val schema = FleetSchema.fromTable(table.toString)
      if (schema == null) return None
      /*
       * The result log format is explicitly specified
       * for subsequent data processing and publishing
       *
       * {
       *  "format" : "..."
       *  "entity": {
       *    "id": "...",
       *    "type": "...",
       *    "timestamp": {...},
       *    "rows": [
       *      {
       *        "action": {...},
       *        "<column>": {...},
       *      }
       *    ]
       *  }
       * }
       *
       */
      val eventJson = eventData.getAsJsonObject
      val entityJson = eventJson.get("entity").getAsJsonObject
      /*
       * Extract entity specific fields from the
       * entity JSON and make the compatible with
       * the schema definition
       */
      val entity_id = entityJson.remove("id").getAsString
      entityJson.addProperty("entity_id", entity_id)

      val entity_type = entityJson.remove("type").getAsString
      entityJson.addProperty("entity_type", entity_type)

      val entity_time = entityJson.remove("timestamp")
        .getAsJsonObject
        .get("value").getAsLong

      entityJson.addProperty("entity_time", entity_time)
      /*
       * Extract event format, event, differential or snapshot
       * and transform into Spark compliant rows
       */
      val format = eventJson.get("format").getAsString
      format match {
        case "event" =>
          transformEvent(entityJson, schema)

        case "differential" =>
          transformDifferential(entityJson, schema)

        case "snapshot" =>
          transformSnapshot(entityJson, schema)

        case _ =>
          throw new Exception(s"The specified format `$format` is not supported.")
      }

    }

  }

  private def transformEvent(entityJson:JsonObject, schema:StructType):Option[Seq[Row]] = {
    /*
     * The `entityJson` contains a single row
     * and this extracted, flatted and reassigned
     * to the JSON object
     */
    val rowsJson = entityJson.remove("rows").getAsJsonArray
    val rowJson = rowsJson.head.getAsJsonObject

    rowJson.keySet.foreach(key => {

      if (key == "action") {
        entityJson.addProperty("entity_action", rowJson.get(key)
          .getAsString)

      } else {
        entityJson.add(key, rowJson.get(key)
          .getAsJsonObject.get("value"))
      }

    })
    /*
     * Transform the respective `entityJson` into
     * a Spark [Row]
     */
    val row = JsonUtil.json2Row(entityJson, schema)
    Some(Seq(row))

  }

  private def transformDifferential(entityJson:JsonObject, schema:StructType):Option[Seq[Row]] = {

    val rowsJson = entityJson.remove("rows").getAsJsonArray
    val rows = rowsJson.map(row => {

      val newEntityJson = entityJson
      val rowJson = row.getAsJsonObject

      rowJson.keySet.foreach(key => {

        if (key == "action") {
          newEntityJson.addProperty("entity_action", rowJson.get(key)
            .getAsString)

        } else {
          newEntityJson.add(key, rowJson.get(key)
            .getAsJsonObject.get("value"))
        }

      })

      JsonUtil.json2Row(newEntityJson, schema)

    }).toSeq

    Some(rows)

  }

  private def transformSnapshot(entityJson:JsonObject, schema:StructType):Option[Seq[Row]] = {

    val rowsJson = entityJson.remove("rows").getAsJsonArray
    val rows = rowsJson.map(row => {

      val newEntityJson = entityJson
      val rowJson = row.getAsJsonObject

      rowJson.keySet.foreach(key => {

        if (key == "action") {
          newEntityJson.addProperty("entity_action", rowJson.get(key)
            .getAsString)

        } else {
          newEntityJson.add(key, rowJson.get(key)
            .getAsJsonObject.get("value"))
        }

      })

      JsonUtil.json2Row(newEntityJson, schema)

    }).toSeq

    Some(rows)

  }

}
