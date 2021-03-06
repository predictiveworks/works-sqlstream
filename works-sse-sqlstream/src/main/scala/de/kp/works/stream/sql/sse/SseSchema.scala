package de.kp.works.stream.sql.sse

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
import de.kp.works.stream.sql.transform.fleet.FleetSchema
import de.kp.works.stream.sql.transform.opcua.OpcUaSchema
import de.kp.works.stream.sql.transform.opencti.CTISchema
import de.kp.works.stream.sql.transform.sensor.SensorSchema
import de.kp.works.stream.sql.transform.things.ThingsSchema
import de.kp.works.stream.sql.transform.tls.TLSSchema
import de.kp.works.stream.sql.transform.zeek.ZeekSchema
import org.apache.spark.sql.types._

object SseSchema {

  def getSchema(schemaType:String):StructType = {
    /*
     * The current implementation distinguishes between
     * SSE events that originate from one of the Works
     * Beats and other sources
     */
    if (schemaType.startsWith("beat")) {
      /*
       * A Works Beat event in this scenario is limited
       * to a certain event schema, i.e. a mix of multiple
       * event formats cannot be supported here.
       */
      getBeatSchema(schemaType)

    } else if (schemaType.startsWith("sensor")) {
      /*
       * A Sensor Beat event in this scenario is limited
       * to a certain event schema, i.e. a mix of multiple
       * event formats cannot be supported here.
       */
      getSensorSchema(schemaType)

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
  private def getBeatSchema(schemaType:String):StructType = {

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
         * Osquery table formats
         */
        val schema = FleetSchema.fromSchemaType(schemaType)
        if (schema == null) getPlainSchema else schema

      case Beats.OPCUA =>
        /*
         * OPC-UA events are normalized by the
         * OPC-UA Beat with a common schema
         */
        OpcUaSchema.schema()

      case Beats.OPENCTI =>
        /*
         * OpenCTI STIXv2 events
         */
        CTISchema.schema()

      case Beats.TLS =>
        /*
         * Osquery agent events that were published to
         * the respective Works Beat as TLS endpoint
         */
        val schema = TLSSchema.fromSchemaType(schemaType)
        if (schema == null) getPlainSchema else schema

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
  private def getSensorSchema(schemaType:String):StructType = {
    SensorSchema.schema()
  }

  /**
   * This method builds the default (or plain) schema
   * for the incoming SSE stream. It is independent
   * of the selected semantic source and derived from
   * the field variables provided by the SSE client.
   */
  private def getPlainSchema:StructType = {

    val fields:Array[StructField] = Array(
      StructField("id",   StringType, nullable = false),
      StructField("type", StringType, nullable = false),
      StructField("data", StringType, nullable = true)
    )

    StructType(fields)

  }

}
