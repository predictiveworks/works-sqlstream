package de.kp.works.stream.sql.transform.opcua

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
import de.kp.works.stream.sql.transform.BaseTransform
import org.apache.spark.sql.Row

object OpcUaTransform extends BaseTransform {
  /*
   * {
   *   address:String,
   *   browsePath:String = "",
   *   topicName:String,
   *   topicType:String,
   *   systemName:String,
   *   sourceTime: Long,
   *   sourcePicoseconds: Int,
   *   serverTime: Long,
   *   serverPicoseconds: Int,
   *   statusCode: Long,
   *   dataValue: Any
   * }
   */
  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {

    val event = mapper.readValue(eventData.toString, classOf[Map[String, Any]])

    val address = event("address").asInstanceOf[String]
    val browsePath = event("browsePath").asInstanceOf[String]

    val topicName = event("topicName").asInstanceOf[String]
    val topicType = event("topicType").asInstanceOf[String]

    val systemName = event("systemName").asInstanceOf[String]

    val sourceTime = event("sourceTime").asInstanceOf[Long]
    val sourcePicoseconds = event("sourcePicoseconds").asInstanceOf[Int]

    val serverTime = event("serverTime").asInstanceOf[Long]
    val serverPicoseconds = event("serverPicoseconds").asInstanceOf[Int]

    val statusCode = event("statusCode").asInstanceOf[Long]
    /*
     * Extract data value and infer the respective data type
     */
    val dataValue = event("dataValue")
    val dataType  = getBasicType(dataValue)

    val serialized = mapper.writeValueAsString(dataValue)

    val values = Seq(
      address,
      browsePath,
      topicName,
      topicType,
      systemName,
      sourceTime,
      sourcePicoseconds,
      serverTime,
      serverPicoseconds,
      statusCode,
      dataType,
      serialized)

    val row = Row.fromSeq(values)
    Some(Seq(row))

  }

}
