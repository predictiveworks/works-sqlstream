package de.kp.works.stream.sql.transform.fleet
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
import de.kp.works.stream.sql.json.JsonUtil
import de.kp.works.stream.sql.transform.{BaseTransform, Beats}
import org.apache.spark.sql.Row

import scala.collection.JavaConversions._

object FleetTransform extends BaseTransform {

  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {
    /*
     * Validate whether the provided event type
     * refers to the support format for Fleet log
     * files:
     *
     *      beat/fleet/<table>>
     */
    val tokens = eventType.split("\\/")
    if (tokens.size != 3)
      throw new Exception("Unknown format for event type detected.")

    if (tokens(0) != Beats.FLEET.toString)
      throw new Exception("The event type provided does not describe a Fleet event.")

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

      val batch = eventData.getAsJsonArray
      val rows = batch.map(batchElem => {
        /*
         * IMPORTANT: This implementation expects that the
         * event data originate from the Fleet Beat, as this
         * beat normalizes and transforms the Osquery result
         * format.
         */
        val batchObj = batchElem.getAsJsonObject
        JsonUtil.json2Row(batchObj, schema)

      }).toSeq

      Some(rows)

    }

  }

}
