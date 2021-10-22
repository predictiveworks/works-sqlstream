package de.kp.works.stream.sql.transform

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
import com.google.gson.{JsonElement, JsonParser}
import org.apache.spark.sql.Row

object TransformUtil extends Serializable {

  protected val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def deserializeMqtt(event:String): (String, JsonElement) = {
    /*
     * The Mqtt event comes with a unified format:
     *
     * {
     *   type : ...,
     *   event: {
     *   }
     * }
     */
    val json = JsonParser.parseString(event)
      .getAsJsonObject

    val eventType = json.get("type").getAsString
    val eventData = JsonParser
      .parseString(json.get("event").getAsString)

    (eventType, eventData)

  }

  def deserializeSse(event:String): (String, JsonElement) = {
    /*
     * The SSE event comes with a unified format:
     *
     * {
     *   type : ...,
     *   event: {
     *   }
     * }
     */
    val json = JsonParser.parseString(event)
      .getAsJsonObject

    val eventType = json.get("type").getAsString
    val eventData = JsonParser
      .parseString(json.get("event").getAsString)

    (eventType, eventData)

  }

  def getBasicType(fieldValue: Any): String = {
    fieldValue match {
      /*
       * Basic data types: these data type descriptions
       * are harmonized with [ValueType]
       */
      case _: BigDecimal => "BigDecimal"
      case _: Boolean => "Boolean"
      case _: Byte => "Byte"
      case _: Double => "Double"
      case _: Float => "Float"
      case _: Int => "Int"
      case _: Long => "Long"
      case _: Short => "Short"
      case _: String => "String"
      /*
       * Datetime support
       */
      case _: java.sql.Date => "Date"
      case _: java.sql.Timestamp => "Timestamp"
      case _: java.util.Date => "Date"
      case _: java.time.LocalDate => "Date"
      case _: java.time.LocalDateTime => "Date"
      case _: java.time.LocalTime => "Timestamp"

      case _ =>
        val now = new java.util.Date().toString
        throw new Exception(s"[ERROR] $now - Basic data type not supported.")
    }

  }

}

trait BaseTransform extends Serializable {

  protected val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]]

  protected def getBasicType(fieldValue: Any): String =
    TransformUtil.getBasicType(fieldValue)

  def deserializeSSE(event:String): (String, JsonElement) =
    TransformUtil.deserializeSse(event)

}
