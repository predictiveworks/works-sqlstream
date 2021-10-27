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
 * This TTN uplink transformer refers to the Things Stack v2
 */
object UplinkV2 extends Uplink {

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  val AttributeType:StructType = {

    val fields = Array(
      StructField("attr_name",  StringType, nullable = false),
      StructField("attr_type",  StringType, nullable = false),
      StructField("attr_value", StringType, nullable = false)
    )

    StructType(fields)

  }

  val GatewayType:StructType = {

    val fields = Array(
      /*
       * EUI of the gateway
       */
      StructField("gtw_id", StringType, nullable = false),
      /*
       * Time when the gateway received the message;
       * left out when gateway does not have synchronized
       * time
       */
      StructField("time", StringType, nullable = true),
      /*
       * Timestamp when the gateway received the message
       */
      StructField("timestamp", LongType, nullable = false),
      /*
       * Channel where the gateway received the message
       */
      StructField("channel", IntegerType, nullable = false),
      /*
       * Signal strength of the received message
       */
      StructField("rssi", IntegerType, nullable = false),
      /*
       * Signal to noise ratio of the received message
       */
      StructField("snr", DoubleType, nullable = false),
      /*
       * RF chain where the gateway received the message
       */
      StructField("rf_chain", IntegerType, nullable = false),
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

  val MetadataType:StructType = {

    val fields = Array(
      /*
       * Airtime in nanoseconds
       */
      StructField("airtime", LongType, nullable = false),
      /*
       * Time when the server received the message
       */
      StructField("time", StringType, nullable = false),
      /*
       * Frequency at which the message was sent
       */
      StructField("frequency", DoubleType, nullable = false),
      /*
       * Modulation that was used - LORA or FSK
       */
      StructField("modulation", StringType, nullable = false),
      /*
       * Data rate that was used - if LORA modulation
       */
      StructField("data_rate", StringType, nullable = true),
      /*
       + Bit rate that was used - if FSK modulation
       */
      StructField("bit_rate", DoubleType, nullable = true),
      /*
       * Coding rate that was used
       */
      StructField("coding_rate", StringType, nullable = true),
      /*
      * Device latitude
      */
      StructField("device_latitude",  DoubleType, nullable = false),
      /*
       * Device longitude
       */
      StructField("device_longitude", DoubleType, nullable = false),
      /*
       * Device altitude
       */
      StructField("device_altitude", StringType, nullable = false),
      /*
       * Gateways
       */
      StructField("gateways", ArrayType(GatewayType, containsNull = false), nullable = true)

    )

    StructType(fields)

  }

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
       * Device type
       */
      val app_id = data("app_id").asInstanceOf[String]
      values += app_id
      /*
       * Device name
       */
      val dev_id = data("dev_id").asInstanceOf[String]
      values += dev_id
      /*
       * In case of LoRaWAN: the DevEUI
       */
      val hardware_serial = data("hardware_serial").asInstanceOf[String]
      values += hardware_serial
      /*
       * LoRaWAN frame counter
       */
      val counter = data.getOrElse("counter", -1).asInstanceOf[Int]
      values += counter

      val port = data.getOrElse("port", -1).asInstanceOf[Int]
      values += port

      val is_retry = data("is_retry").asInstanceOf[Boolean]
      values += is_retry

      val confirmed = data("confirmed").asInstanceOf[Boolean]
      values += confirmed

      val payload_raw = data("payload_raw").asInstanceOf[String]
      values += payload_raw

      /*
       * Extract payload fields
       */
      val attributes = data.get("payload_fields") match {
        case Some(v) => getAttributes(v)
        case _ => null
      }

      values += attributes

      /*
       * Assign metadata
       */
      val metadata = data.get("metadata") match {
        case Some(v) => getMetadata(v)
        case _ => null
      }

      values += metadata

      val row = Row.fromSeq(values)
      Some(row)

    } catch {
      case t:Throwable => t.printStackTrace(); None
    }

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
  private def getMetadata(metaVal:Any):Row = {

    val values = mutable.ArrayBuffer.empty[Any]
    val metadata = metaVal.asInstanceOf[Map[String, _]]

    val airtime = metadata
      .getOrElse("airtime", System.currentTimeMillis).asInstanceOf[Long]

    values += airtime

    val time = metadata
      .getOrElse("time", "").asInstanceOf[String]

    values += time

    val frequency = metadata
      .getOrElse("frequency", 0D).asInstanceOf[Double]

    values += frequency

    val modulation = metadata
      .getOrElse("modulation", "").asInstanceOf[String]

    values += modulation

    val data_rate = metadata
      .getOrElse("data_rate", "").asInstanceOf[String]

    values += data_rate

    val bit_rate = metadata.get("bit_rate") match {
      case Some(v) => try {
        v.asInstanceOf[Double]
      } catch {
        case _:Throwable =>
          v.asInstanceOf[Int].toDouble
      }
      case _ => 0D
    }

    values += bit_rate

    val coding_rate = metadata
      .getOrElse("coding_rate", "").asInstanceOf[String]

    values += coding_rate

    val latitude = metadata
      .getOrElse("latitude", 0D).asInstanceOf[Double]

    values += latitude

    val longitude = metadata
      .getOrElse("longitude", 0D).asInstanceOf[Double]

    values += longitude

    val altitude = metadata.get("altitude") match {
      case Some(v) => try {
        v.asInstanceOf[Double]
      } catch {
        case _:Throwable =>
          v.asInstanceOf[Int].toDouble
      }
      case _ => 0D
    }

    values += altitude
    /*
     * Gateways
     */
    val gateways = metadata.get("gateways") match {
      case Some(v) => getGateways(v)
      case _ => Seq.empty[Row]
    }

    values += gateways

    val row = Row.fromSeq(values)
    row

  }
  /**
  "gateways": [
      {
        "gtw_id": "ttn-herengracht-ams",
        "timestamp": 12345,
        "time": "1970-01-01T00:00:00Z",
        "channel": 0,
        "rssi": -25,
        "snr": 5,
        "rf_chain": 0,
        "latitude": 52.1234,
        "longitude": 6.1234,
        "altitude": 6
      },
      //...more if received by more gateways...
    ],
   */

  def getGateways(gateVal:Any):Seq[Row] = {

    val gateways = gateVal.asInstanceOf[List[Map[String, _]]]
    gateways.map(gateway => {

      val values = mutable.ArrayBuffer.empty[Any]

      val gtw_id = gateway
        .getOrElse("gtw_id", "").asInstanceOf[String]

      values += gtw_id

      val time = gateway
        .getOrElse("time", "").asInstanceOf[String]

      values += time

      val timestamp = gateway.get("timestamp") match {
        case Some(v) => try {
          v.asInstanceOf[Long]
        } catch {
          case _:Throwable => 1000 * v.asInstanceOf[Int].toLong
        }
        case _ => 0L
      }

      values += timestamp

      val channel = gateway
        .getOrElse("channel", -1).asInstanceOf[Int]

      values += channel

      val rssi = gateway
        .getOrElse("rssi", -1).asInstanceOf[Int]

      values += rssi

      val snr = gateway.get("snr") match {
        case Some(v) => try {
          v.asInstanceOf[Double]
        } catch {
          case _:Throwable =>
            v.asInstanceOf[Int].toDouble
        }
        case _ => 0D
      }

      values += snr

      val rf_chain = gateway
        .getOrElse("rf_chain", -1).asInstanceOf[Int]

      values += rf_chain

      val latitude = gateway
        .getOrElse("latitude", 0D).asInstanceOf[Double]

      values += latitude

      val longitude = gateway
        .getOrElse("longitude", 0D).asInstanceOf[Double]

      values += longitude

      val altitude = gateway.get("altitude") match {
        case Some(v) => try {
          v.asInstanceOf[Double]
        } catch {
          case _:Throwable =>
            v.asInstanceOf[Int].toDouble
        }
        case _ => 0D
      }

      values += altitude

      Row.fromSeq(values)

    })

  }
  /**
   * Transform the payload fields of an uplink message
   */
  private def getAttributes(attrsVal:Any):Seq[Row] = {

    val attributes = attrsVal.asInstanceOf[Map[String,_]]
    attributes.map{case(k,v) =>

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

    }.toSeq

  }

  def schema():StructType = {

    val fields = Array(
      StructField("app_id",          StringType, nullable = false),
      StructField("dev_id",          StringType, nullable = false),
      StructField("hardware_serial", StringType, nullable = false),
      /*
       * LoRaWAN frame counter
       */
      StructField("counter", IntegerType, nullable = true),
      /*
       * LoRaWAN FPort
       */
      StructField("port", IntegerType, nullable = true),
      /*
       * Is set to true if this message is a retry
       * (you could also detect this from the counter)
       */
      StructField("is_retry", BooleanType, nullable = false),
      /*
       * Is set to true if this message was a confirmed
       * message
       */
      StructField("confirmed", BooleanType, nullable = false),
      /*
       * Base64 encoded payload: [0x01, 0x02, 0x03, 0x04]
       */
      StructField("payload_raw", StringType, nullable = false),
      /*
       * Decoded payload object, decoded by the device
       * payload formatter
       */
      StructField("attributes", ArrayType(AttributeType, containsNull = true), nullable = true),
      /*
       * The metadata object
       */
      StructField("metadata", MetadataType, nullable = true)
    )

    StructType(fields)
  }
}
