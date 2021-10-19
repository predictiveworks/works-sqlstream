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

import org.apache.spark.sql.types._

object SseSchema {

  def getSchema(schemaType:String):StructType = {
    /*
     * The current implementation distinguishes between
     * SSE events that originate from one of the Works
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
      case Beats.FLEET =>
        /*
         * Fleet events distinguish between 200+
         * Osquery table formats
         */
      case Beats.OPCUA =>
      case Beats.OPENCTI =>
      case Beats.OSQUERY =>
      case Beats.THINGS =>
      case Beats.ZEEK =>
        /*
         * Zeek events distinguish between 35+
         * Zeek log formats
         */
      case _ =>
        /* This should never happen */
        throw new Exception(s"The provided Works Beat is not supported.")

    }

    ???
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
