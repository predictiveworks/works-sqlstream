package de.kp.works.stream.sql.transform.ttn

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
import com.google.gson.{JsonArray, JsonObject, JsonParser}

import java.util
import scala.collection.JavaConversions._

/**
 * This TTN uplink transformer refers to the Things Stack v2
 */
object UplinkV2 extends Serializable {

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  /**
   * This method expects an MQTT message (payload)
   * as a serialized JSON object and transforms
   * the message into an enriched and normalized
   * format.
   */
  def transform(message:String):JsonObject = {

    val json = JsonParser.parseString(message)

    if (!json.isJsonObject) {
      val now = new util.Date()
      throw new Exception(s"[UplinkTransformer] ${now.toString} - Uplink messages must be JSON objects.")
    }

    val jMessage = json.getAsJsonObject
    var newObject = new JsonObject()

    /* deviceName */
    newObject.addProperty("deviceName", jMessage.get("dev_id").getAsString)

    /* deviceType */
    newObject.addProperty("deviceType", jMessage.get("app_id").getAsString)

    /* airtime, latitude, longitude, altitude */
    if (jMessage.has("metadata")) {
      val metadata = jMessage.get("metadata").getAsJsonObject
      newObject = getMetadata(newObject, metadata.getAsJsonObject())
    }
    else
      newObject = getMetadata(newObject, null)

    /* payload fields */
    if (jMessage.has("payload_fields")) {
      val fields = jMessage.get("payload_fields").getAsJsonObject
      newObject = getPayloadFields(newObject, fields)
    }
    else
      newObject = getPayloadFields(newObject, null)

    newObject

  }
  /**
   * "metadata": {
   * "airtime": 46336000, 				// Airtime in nanoseconds
   *
   * "time": "1970-01-01T00:00:00Z", 	// Time when the server received the message
   * "frequency": 868.1, 				// Frequency at which the message was sent
   * "modulation": "LORA", 			// Modulation that was used - LORA or FSK
   * "data_rate": "SF7BW125", 			// Data rate that was used - if LORA modulation
   * "bit_rate": 50000, 				// Bit rate that was used - if FSK modulation
   * "coding_rate": "4/5", 			// Coding rate that was used
   *
   * "gateways": [
   * {
   * "gtw_id": "ttn-herengracht-ams", 	// EUI of the gateway
   * "timestamp": 12345, 				// Timestamp when the gateway received the message
   * "time": "1970-01-01T00:00:00Z", 	// Time when the gateway received the message - left out when gateway does not have synchronized time
   * "channel": 0, 					// Channel where the gateway received the message
   * "rssi": -25, 						// Signal strength of the received message
   * "snr": 5, 						// Signal to noise ratio of the received message
   * "rf_chain": 0, 					// RF chain where the gateway received the message
   * "latitude": 52.1234, 				// Latitude of the gateway reported in its status updates
   * "longitude": 6.1234, 				// Longitude of the gateway
   * "altitude": 6 					// Altitude of the gateway
   * }, ...
   * ],
   *
   * "latitude": 52.2345, // Latitude of the device
   * "longitude": 6.2345, // Longitude of the device
   * "altitude": 2 // Altitude of the device
   * }
   */
  private def getMetadata(newObject:JsonObject, metadata:JsonObject):JsonObject = {

    if (metadata == null) {
      /*
       * `airtime` of the message in nanoseconds (nanoseconds is taken
       * from the official TTN documentation :
       *
       * https://www.thethingsnetwork.org/docs/applications/mqtt/api/
       *
       * However, we never saw a use case or TTN application, where
       * this parameter is different from milli seconds
       */
      newObject.addProperty("airtime", System.currentTimeMillis)

      /* `latitude` of the device */
      newObject.addProperty("latitude", 0D)

      /* longitude of the device */
      newObject.addProperty("longitude", 0D)

      /* altitude of the device */
      newObject.addProperty("altitude", 0D)

    } else {

      /* airtime of the message in nanoseconds */

      var airtime:Long = System.currentTimeMillis
      if (metadata.has("airtime"))
        airtime = metadata.get("airtime").getAsLong

      newObject.addProperty("airtime", airtime)

      /* latitude of the device */

      var latitude:Double = 0D
      if (metadata.has("latitude"))
        latitude = metadata.get("latitude").getAsDouble

      newObject.addProperty("latitude", latitude)

      /* longitude of the device */

      var longitude:Double = 0D
      if (metadata.has("longitude"))
        longitude = metadata.get("longitude").getAsDouble

      newObject.addProperty("longitude", longitude)

      /* altitude of the device */

      var altitude:Double = 0D
      if (metadata.has("altitude"))
        altitude = metadata.get("altitude").getAsDouble

      newObject.addProperty("altitude", altitude)

    }

    newObject

  }
  /**
   * Transform the payload fields of an uplink message
   */
  private def getPayloadFields(newObject:JsonObject, fields:JsonObject):JsonObject = {

    val columns = new JsonArray
    val fieldMap = mapper.readValue(fields.toString, classOf[Map[String, Any]])

    fields.entrySet().foreach(field => {

      val fieldName = field.getKey
      val fieldValue = field.getValue

      val column = new JsonObject
      column.addProperty("name", fieldName)
      column.add("value", fieldValue)

      /* In addition to the field specification, a column
       * is also enriched with the data type
       */
      var dataType:String = "NULL"
      fieldMap(fieldName) match {
        case _: List[_] =>
          dataType = "LIST"
        case _: Map[_, _] =>
          dataType = "MAP"
        case _ =>
          dataType = getBasicType(fieldMap(fieldName))
      }

      column.addProperty("type", dataType)
      columns.add(column)

    })

    newObject.add("columns", columns)
    newObject

  }

  private def getBasicType(fieldValue: Any): String = {
    fieldValue match {
      /*
       * Basic data types: these data type descriptions
       * are harmonized with [ValueType]
       */
      case _: BigDecimal => "DECIMAL"
      case _: Boolean => "BOOLEAN"
      case _: Byte => "BYTE"
      case _: Double => "DOUBLE"
      case _: Float => "FLOAT"
      case _: Int => "INT"
      case _: Long => "LONG"
      case _: Short => "SHORT"
      case _: String => "STRING"
      /*
       * Datetime support
       */
      case _: java.sql.Date => "DATE"
      case _: java.sql.Timestamp => "TIMESTAMP"
      case _: java.util.Date => "DATE"
      case _: java.time.LocalDate => "DATE"
      case _: java.time.LocalDateTime => "DATE"
      case _: java.time.LocalTime => "TIMESTAMP"

     case _ =>
        val now = new java.util.Date().toString
        throw new Exception(s"[ERROR] $now - Basic data type not supported.")
    }

  }

}
