package de.kp.works.stream.sql.mqtt

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

import de.kp.works.stream.sql.transform.Beats
import de.kp.works.stream.sql.transform.fiware.FiwareSchema
import de.kp.works.stream.sql.transform.opcua.OpcUaSchema
import de.kp.works.stream.sql.transform.things.ThingsSchema
import de.kp.works.stream.sql.transform.zeek.ZeekSchema
import org.apache.spark.sql.types._

object MqttSchema {

  def getSchema(schemaType:String):StructType = {
    /*
     * The current implementation distinguishes between
     * MQTT events that originate from one of the Works
     * Beats and other sources
     */
    if (schemaType.startsWith("beats")) {
      /*
       * A Works Beat event in this scenario is limited
       * to a certain event schema, i.e. a mix of multiple
       * event formats cannot be supported here.
       */
      getBeatsSchema(schemaType)

    }
    else {
      /*
       * The provided SSE event contains data that are
       * either different from those provided by one of
       * the different Works Beats, or,
       *
       * combines events with multiple schemas
       */
      getPlainSchema
    }

  }
  private def getBeatsSchema(schemaType:String):StructType = {

    val tokens = schemaType.split("\\.")
    val beat = try {
      Beats.withName(tokens(1))

    } catch {
      case _:Throwable => null
    }

    if (beat == null) return getPlainSchema
    beat match {
      case Beats.FIWARE =>
        /*
         * Fiware events are defined by a single
         * NGSI-compliant schema and do not need
         * any further sub specification.
         */
        FiwareSchema.schema()

      case Beats.FLEET =>
        /*
         * Fleet events distinguish between 200+
         * Osquery table formats (TODO)
         */
        getPlainSchema

      case Beats.OPCUA =>
        /*
         * OPC-UA events are normalized by the
         * OPC-UA Beat with a common schema
         */
        OpcUaSchema.schema()

      case Beats.OPENCTI =>
        // TODO
        getPlainSchema

      case Beats.OSQUERY =>
        // TODO
        getPlainSchema

      case Beats.THINGS =>
        /*
         * ThingsBoard gateway events describe changes
         * of device attributes and have a common schema
         * for all events.
         */
        ThingsSchema.schema()

      case Beats.ZEEK =>
        /*
         * Zeek events distinguish between 35+ Zeek log
         * formats; the schema type configuration is
         * expected to have the following format:
         *
         * sample = zeek.conn.log
         */
        val schema = ZeekSchema.fromSchemaType(schemaType)
        if (schema == null) getPlainSchema else schema

      case _ =>
        /* This should never happen */
        throw new Exception(s"The provided Works Beat is not supported.")

    }

  }

  /**
   * This method builds the default (or plain) schema
   * for the incoming MQTT stream. It is independent
   * of the selected semantic source and derived from
   * the field variables provided by the MQTT client(s).
   */
  private def getPlainSchema:StructType = {

    val fields:Array[StructField] = Array(
      /*
       * The message identifier
       */
      StructField("id", IntegerType, nullable = false),
      /*
       * The timestamp in milli seconds the message arrived
       */
      StructField("timestamp", LongType, nullable = false),
      /*
       * The MQTT topic of the message
       */
      StructField("topic", StringType, nullable = false),
      /*
       * The quality of service of the message
       */
      StructField("qos", IntegerType, nullable = false),
      /*
       * Indicates whether or not this message might be a
       * duplicate of one which has already been received.
       */
      StructField("duplicate", BooleanType, nullable = false),
      /*
       * Indicates whether or not this message should be/was
       * retained by the server.
       */
      StructField("retained", BooleanType, nullable = false),
      /*
       * The payload of this message
       */
      StructField("payload", BinaryType, nullable = true)
    )

    StructType(fields)

  }
}
