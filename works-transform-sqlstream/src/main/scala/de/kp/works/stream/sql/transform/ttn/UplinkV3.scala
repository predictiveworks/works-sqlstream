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
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import scala.collection.mutable

/**
 * This TTN uplink transformer refers to the Things Stack v3
 */
object UplinkV3 extends Uplink {

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  /**
   * This method expects an MQTT message (payload)
   * as a serialized JSON object and transforms
   * the message into an enriched and normalized
   * format.
   */
  def transform(message:String):Option[Row] = {

    try {

      val values = mutable.ArrayBuffer.empty[Any]
      val data = mapper.readValue(message, classOf[Map[String, Any]])
      /*
       * STEP #1: Unpack & flatten device and application
       * identifier to match the predefined schema
       */
      /*
       * "end_device_ids" : {
       *    "device_id" : "dev1",              // Device ID
       *    "application_ids" : {
       *        "application_id" : "app1"      // Application ID
       *    },
       *    "dev_eui" : "0004A30B001C0530",    // DevEUI of the end device
       *    "join_eui" : "800000000000000C",   // JoinEUI of the end device (also known as AppEUI in LoRaWAN versions below 1.1)
       *    "dev_addr" : "00BCB929"            // Device address known by the Network Server
       * },
       */
      val end_device_ids = data.get("end_device_ids") match {
        case Some(v) => v.asInstanceOf[Map[String, _]]
        case _ =>
          throw new Exception("The uplink v3 message does not contain any device identification.")

      }

      val params = List("device_id", "dev_eui", "join_eui", "dev_addr")
      params.foreach(p =>
        values += end_device_ids(p).asInstanceOf[String])

      val application_id = end_device_ids("application_ids")
        .asInstanceOf[Map[String,_]]("application_id").asInstanceOf[String]

      values += application_id
      /*
       * STEP #2: Unpack correlation identifiers
       * of the received message; this implementation
       * treats this parameter as optional
       *
       * "correlation_ids" : [
       *     "as:up:01E0WZGT6Y7657CPFPE5WEYDSQ",
       *     "gs:conn:01E0WDEC6T5T4XXBAX7S1VMFKE",
       *     "gs:uplink:01E0WZGSZWT07NE5TS2APTV1Z9",
       *     "ns:uplink:01E0WZGSZXZXZS8RFAWZX0F2FY",
       *     "rpc:/ttn.lorawan.v3.GsNs/HandleUplink:01E0WZGSZXGE1KS577PFBWRJEE"
       *   ],
       */
      val correlation_ids = data.getOrElse("correlation_ids", List.empty[String]).asInstanceOf[List[String]]
      values += correlation_ids
      /*
       * STEP #3: ISO 8601 UTC timestamp at which the message
       * has been received by the Application Server
       */
      val received_at_application = data("received_at").asInstanceOf[String]
      values += received_at_application
      /*
       * STEP #4: Unpack & flatten the provided uplink message
       *
       * "uplink_message" : {
       *    "session_key_id": "AXA50...",             // Join Server issued identifier for the session keys used by this uplink
       *    "f_cnt": 1,                               // Frame counter
       *    "f_port": 1,                              // Frame port
       *    "frm_payload": "gkHe",                    // Frame payload (Base64)
       *    "decoded_payload" : {                     // Decoded payload object, decoded by the device payload formatter
       *       "temperature": 1.0,
       *       "luminosity": 0.64
       *     },
       *
       *     "rx_metadata": [{                        // A list of metadata for each antenna of each gateway that received this message
       *       "gateway_ids": {
       *         "gateway_id": "gtw1",                // Gateway ID
       *         "eui": "9C5C8E00001A05C4"            // Gateway EUI
       *       },
       *       "time": "2020-02-12T15:15:45.787Z",    // ISO 8601 UTC timestamp at which the uplink has been received by the gateway
       *       "timestamp": 2463457000,               // Timestamp of the gateway concentrator when the message has been received
       *       "rssi": -35,                           // Received signal strength indicator (dBm)
       *       "channel_rssi": -35,                   // Received signal strength indicator of the channel (dBm)
       *       "snr": 5.2,                            // Signal-to-noise ratio (dB)
       *       "uplink_token": "ChIKEA...",           // Uplink token injected by gateway, Gateway Server or fNS
       *       "channel_index": 2                     // Index of the gateway channel that received the message
       *       "location": {                          // Gateway location metadata (only for gateways with location set to public)
       *         "latitude": 37.97155556731436,       // Location latitude
       *         "longitude": 23.72678801175413,      // Location longitude
       *         "altitude": 2,                       // Location altitude
       *         "source": "SOURCE_REGISTRY"          // Location source. SOURCE_REGISTRY is the location from the Identity Server.
       *       }
       *     }],
       *     "settings": {                            // Settings for the transmission
       *       "data_rate": {                         // Data rate settings
       *         "lora": {                            // LoRa modulation settings
       *           "bandwidth": 125000,               // Bandwidth (Hz)
       *           "spreading_factor": 7              // Spreading factor
       *         }
       *       },
       *       "data_rate_index": 5,                  // LoRaWAN data rate index
       *       "coding_rate": "4/6",                  // LoRa coding rate
       *       "frequency": "868300000",              // Frequency (Hz)
       *     },
       *     "received_at": "2020-02-12T15:15..."     // ISO 8601 UTC timestamp at which the uplink has been received by the Network Server
       *     "consumed_airtime": "0.056576s",         // Time-on-air, calculated by the Network Server using payload size and transmission settings
       *     "locations": {                           // End device location metadata
       *       "user": {
       *         "latitude": 37.97155556731436,       // Location latitude
       *         "longitude": 23.72678801175413,      // Location longitude
       *         "altitude": 10,                      // Location altitude
       *         "source": "SOURCE_REGISTRY"          // Location source. SOURCE_REGISTRY is the location from the Identity Server.
       *       }
       *     }
       * }
       */
      val uplink_message = data("uplink_message").asInstanceOf[Map[String,_]]

      val received_at_network = uplink_message("received_at").asInstanceOf[String]
      values += received_at_network

      val consumed_airtime = uplink_message("consumed_airtime").asInstanceOf[String]
      values += consumed_airtime
      /*
       * Unpack & flatten device location
       *
       *  "locations": {                           // End device location metadata
       *    "user": {
       *      "latitude": 37.97155556731436,       // Location latitude
       *      "longitude": 23.72678801175413,      // Location longitude
       *      "altitude": 10,                      // Location altitude
       *      "source": "SOURCE_REGISTRY"          // Location source. SOURCE_REGISTRY is the location from the Identity Server.
       *    }
       *  }
       */
      val locations = uplink_message("locations").asInstanceOf[Map[String,_]]
      val user = locations("user").asInstanceOf[Map[String,_]]

      val latitude = user("latitude").asInstanceOf[Double]
      val longitude = user("longitude").asInstanceOf[Double]
      val altitude = try {
        user("altitude").asInstanceOf[Double]
      } catch {
        case _:Throwable => user("altitude").asInstanceOf[Int].toDouble
      }

      values += latitude
      values += longitude
      values += altitude

      /* Unpack session data
      *
      *    "session_key_id": "AXA50...",             // Join Server issued identifier for the session keys used by this uplink
      *    "f_cnt": 1,                               // Frame counter
      *    "f_port": 1,                              // Frame port
      *    "frm_payload": "gkHe",                    // Frame payload (Base64)
      *    "decoded_payload" : {                     // Decoded payload object, decoded by the device payload formatter
      *       "temperature": 1.0,
      *       "luminosity": 0.64
      *     },
      */
      val session_key_id = uplink_message("session_key_id").asInstanceOf[String]
      values += session_key_id

      val f_cnt = uplink_message.getOrElse("f_cnt", 0).asInstanceOf[Int]
      values += f_cnt

      val f_port = uplink_message.getOrElse("f_port", 0).asInstanceOf[Int]
      values += f_port

      val frm_payload = uplink_message("frm_payload").asInstanceOf[String]
      values += frm_payload
      /*
       * Unpack attributes that are associated with
       * this Uplink message
       */
      val attributes = uplink_message("decoded_payload")
        .asInstanceOf[Map[String,_]]
        .map{case(k, v) =>

          val attr_name = k
          val attr_type = v match {
            case _: List[_]   => "List"
            case _: Map[_, _] => "Map"
            case _ => getDataType(v)
          }

          val attr_value = mapper.writeValueAsString(v)

          val values = Seq(
            attr_name,
            attr_type,
            attr_value
          )

          Row.fromSeq(values)
        }

      values += attributes

      values += gateways2Rows(uplink_message)
      values += settings2Row(uplink_message)

      val row = Row.fromSeq(values)
      Some(row)

    } catch {
      case t:Throwable => t.printStackTrace(); None
    }

  }
  /*
   *     "rx_metadata": [{                        // A list of metadata for each antenna of each gateway that received this message
   *       "gateway_ids": {
   *         "gateway_id": "gtw1",                // Gateway ID
   *         "eui": "9C5C8E00001A05C4"            // Gateway EUI
   *       },
   *       "time": "2020-02-12T15:15:45.787Z",    // ISO 8601 UTC timestamp at which the uplink has been received by the gateway
   *       "timestamp": 2463457000,               // Timestamp of the gateway concentrator when the message has been received
   *       "rssi": -35,                           // Received signal strength indicator (dBm)
   *       "channel_rssi": -35,                   // Received signal strength indicator of the channel (dBm)
   *       "snr": 5.2,                            // Signal-to-noise ratio (dB)
   *       "uplink_token": "ChIKEA...",           // Uplink token injected by gateway, Gateway Server or fNS
   *       "channel_index": 2                     // Index of the gateway channel that received the message
   *       "location": {                          // Gateway location metadata (only for gateways with location set to public)
   *         "latitude": 37.97155556731436,       // Location latitude
   *         "longitude": 23.72678801175413,      // Location longitude
   *         "altitude": 2,                       // Location altitude
   *         "source": "SOURCE_REGISTRY"          // Location source. SOURCE_REGISTRY is the location from the Identity Server.
   *       }
   *     }],
   */
  private def gateways2Rows(uplink_message:Map[String, Any]):Option[Seq[Row]] = {

    try {

      val gateways = uplink_message("rx_metadata").asInstanceOf[List[_]]
      val rows = gateways.map(v => {

        val gateway = v.asInstanceOf[Map[String,_]]
        val gateway_ids = gateway("gateway_ids").asInstanceOf[Map[String, _]]

        val gateway_id = gateway_ids("gateway_id").asInstanceOf[String]
        val eui = gateway_ids("eui").asInstanceOf[String]

        val time = gateway("time").asInstanceOf[String]
        val timestamp = gateway("time").asInstanceOf[Long]

        val rssi = gateway("rssi").asInstanceOf[Int]
        val channel_rssi = gateway("channel_rssi").asInstanceOf[Int]

        val snr = gateway("snr").asInstanceOf[Double]
        val uplink_token = gateway("uplink_token").asInstanceOf[String]

        val channel_index = gateway("channel_index").asInstanceOf[Int]
        /*
         * This implementation accepts that the geo location
         * of a certain gateway is not provided with this
         * uplink v3 message.
         */
        var latitude:Double  = 0D
        var longitude:Double = 0D
        var altitude:Double  = 0D

        gateway.get("location") match {
          case Some(v) =>

            val location = v.asInstanceOf[Map[String,_]]
            latitude  = location("latitude").asInstanceOf[Double]

            longitude  = location("longitude").asInstanceOf[Double]
            altitude  = try {
              location("altitude").asInstanceOf[Double]

            } catch {
              case _:Throwable =>
                location("altitude").asInstanceOf[Int].toDouble
            }
          case _ => /* Do nothing */
        }

        val values = Seq(
          gateway_id,
          eui,
          time,
          timestamp,
          rssi,
          channel_rssi,
          snr,
          uplink_token,
          channel_index,
          latitude,
          longitude,
          altitude
        )

        Row.fromSeq(values)

      })

      Some(rows)

    } catch {
      case _:Throwable => None
    }

  }
  /**
   * This method transforms the settings part of the uplink v3
   * message into an Apache Spark SQL Row
   */
  private def settings2Row(uplink_message:Map[String, Any]):Option[Row] = {

    try {

      val settings = uplink_message("settings").asInstanceOf[Map[String, _]]
      val coding_rate = settings("coding_rate").asInstanceOf[String]

      val data_rate = settings("data_rate").asInstanceOf[Map[String,_]]
      /*
       * We expect that tha data_rate is a Map with a single
       * key and this key specifies the modulation
       */
      val modulation = data_rate.keys.head
      val modulation_data = data_rate(modulation).asInstanceOf[Map[String,_]]

      val bandwidth = modulation_data("bandwidth").asInstanceOf[Long]
      val spreading_factor = modulation_data("spreading_factor").asInstanceOf[Int]

      val data_rate_index = settings("data_rate_index").asInstanceOf[Int]
      val frequency = settings("frequency").asInstanceOf[String]

      val values = Seq(
        coding_rate,
        modulation,
        bandwidth,
        spreading_factor,
        data_rate_index,
        frequency
      )

      val row = Row.fromSeq(values)
      Some(row)

    } catch {
      case _:Throwable => None
    }
  }

  private val AttributeType:StructType = {

    val fields = Array(
      StructField("attr_name",  StringType, nullable = false),
      StructField("attr_type",  StringType, nullable = false),
      StructField("attr_value", StringType, nullable = false)
    )

    StructType(fields)

  }
  private val GatewayType:StructType = {

    val fields = Array(
      /*
       * Gateway ID
       */
      StructField("gateway_id", StringType, nullable = false),
      /*
       * Gateway EUI
       */
      StructField("eui", StringType, nullable = false),
      /*
       * ISO 8601 UTC timestamp at which the uplink
       * has been received by the gateway
       */
      StructField("time", StringType, nullable = false),
      /*
       * Timestamp of the gateway concentrator
       * when the message has been received
       */
      StructField("timestamp", LongType, nullable = false),
      /*
       * Received signal strength indicator (dBm)
       */
      StructField("rssi", IntegerType, nullable = false),
      /*
       * Received signal strength indicator of the
       * channel (dBm)
       */
      StructField("channel_rssi", IntegerType, nullable = false),
      /*
       * Signal-to-noise ratio (dB)
       */
      StructField("snr", DoubleType, nullable = false),
      /*
       * Uplink token injected by gateway, Gateway
       * Server or fNS
       */
      StructField("uplink_token", StringType, nullable = false),
      /*
       * Index of the gateway channel that received the message
       */
      StructField("channel_index", IntegerType, nullable = false),
      /*
      * Gateway latitude
      */
      StructField("gateway_latitude",  DoubleType, nullable = false),
      /*
       * Gateway longitude
       */
      StructField("gateway_longitude", DoubleType, nullable = false),
      /*
       * Gateway altitude
       */
      StructField("gateway_altitude", StringType, nullable = false)
    )

    StructType(fields)

  }
  /*
   *   "settings": {
   *     "data_rate": {
   *       "lora": {
   *         "bandwidth": 125000,
   *         "spreading_factor": 7
   *       }
   *     },
   *     "data_rate_index": 5,
   *     "coding_rate": "4/6",
   *     "frequency": "868300000",
   *   },
   */
  private val SettingsType:StructType = {

    val fields = Array(
      /*
       * LoRa coding rate
       */
      StructField("coding_rate", StringType, nullable = false),
      /*
       * Data modulation
       */
      StructField("modulation", StringType, nullable = false),
      /*
       * Data rate bandwidth (Hz)
       */
      StructField("bandwidth", LongType, nullable = false),
      /*
       * Data rate spreading factory
       */
      StructField("spreading_factor", IntegerType, nullable = false),
      /*
       * LoRaWAN data rate index
       */
      StructField("data_rate_index", IntegerType, nullable = false),
      /*
       * Frequency (Hz)
       */
      StructField("frequency", StringType, nullable = false)
    )

    StructType(fields)
  }

  def schema():StructType = {

    val fields = Array(
      StructField("device_id",        StringType, nullable = false),
      StructField("dev_eui",          StringType, nullable = false),
      StructField("join_eui",         StringType, nullable = false),
      StructField("dev_addr",         StringType, nullable = false),
      StructField("application_id",   StringType, nullable = false),
      /*
       * Correlation identifiers of the received message
       */
      StructField("correlation_ids",  ArrayType(StringType, containsNull = true), nullable = true),
      /*
       * ISO 8601 UTC timestamp at which the message
       * has been received by the Application Server
       */
      StructField("received_at_application", StringType, nullable = false),
      /*
       * ISO 8601 UTC timestamp at which the uplink
       * has been received by the Network Server
       */
      StructField("received_at_network", StringType, nullable = false),
      /*
       * Time-on-air, calculated by the Network Server
       * using payload size and transmission settings
       */
      StructField("consumed_airtime", StringType, nullable = false),
      /*
       * End device latitude
       */
      StructField("device_latitude",  DoubleType, nullable = false),
      /*
       * End device longitude
       */
      StructField("device_longitude", DoubleType, nullable = false),
      /*
       * End device altitude
       */
      StructField("device_altitude", StringType, nullable = false),
      /*
       * Join Server issued identifier for the session
       * keys used by this uplink
       */
      StructField("session_key_id", StringType, nullable = false),
      /*
       * Frame counter
       */
      StructField("f_cnt", IntegerType, nullable = false),
      /*
       * Frame port
       */
      StructField("f_port", IntegerType, nullable = false),
      /*
       * Frame payload (Base64)
       */
      StructField("frm_payload", StringType, nullable = false),
      /*
       * Decoded payload object, decoded by the device
       * payload formatter
       */
      StructField("attributes", ArrayType(AttributeType, containsNull = false), nullable = false),
      /*
       * A list of metadata for each antenna of each gateway
       * that received this message
       */
      StructField("gateways", ArrayType(GatewayType, containsNull = false), nullable = false),
      /*
       * Settings for the transmission
       */
      StructField("transmission", SettingsType, nullable = false)
   )

    StructType(fields)

  }

}
