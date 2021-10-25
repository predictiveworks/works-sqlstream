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

import de.kp.works.stream.sql.transform.{Beats, TransformUtil}
import de.kp.works.stream.sql.transform.fiware.FiwareTransform
import de.kp.works.stream.sql.transform.fleet.FleetTransform
import de.kp.works.stream.sql.transform.opcua.OpcUaTransform
import de.kp.works.stream.sql.transform.opencti.CTITransform
import de.kp.works.stream.sql.transform.things.ThingsTransform
import de.kp.works.stream.sql.transform.tls.TLSTransform
import de.kp.works.stream.sql.transform.zeek.ZeekTransform
import org.apache.spark.sql.Row

object SseUtil {
  /**
   * This method transforms a certain [SseEvent] into
   * a Spark SQL compliant [Row]
   */
  def toRows(event:SseEvent, schemaType:String):Option[Seq[Row]] = {
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
      fromBeatsValues(event)

    } else
      /*
       * The provided SSE event contains data that are
       * either different from those provided by one of
       * the different Works Beats, or,
       *
       * combines events with multiple schemas
       */
      fromPlainValues(event)

  }
  /**
   * This method tries to resolve events originating
   * from one of Works Beats via SSE channel with the
   * best matching representation
   */
  def fromBeatsValues(event:SseEvent):Option[Seq[Row]] = {
    /*
     * Check whether the event type provided with
     * this event originates from a Works Beat
     */
    val beat = try {
      Beats.withName(event.sseType.toLowerCase)

    } catch {
      case _:Throwable => null
    }

    if (beat == null) fromPlainValues(event)
    else {
      /*
       * The SSE event format contains the serialized
       * payload `sseData`, which comes with a unified
       * format:
       *
       * {
       *   type : ...,
       *   event: ...
       * }
       *
       */
      val (eventType, eventData) =
        TransformUtil.deserializeSse(event.sseData)
      /*
       * Validate whether `eventType` and detected
       * beat are compliant. Sample:
       *
       *           beat/fiware/notification
       */
      val tokens = eventType.split("\\/")
      if (tokens(1) != beat.toString)
        throw new Exception("Event type and Works Beat specification are inconsistent.")
      /*
       * Transform the received SSE event into a
       * Beat and schema-compliant representation.
       */
      beat match {
        case Beats.FIWARE =>
          /*
           * Events that originate from a Fiware Context
           * Broker have a common NGSI-compliant format,
           * and can be described with a single schema
           */
          FiwareTransform.fromValues(eventType, eventData)

        case Beats.FLEET =>
          FleetTransform.fromValues(eventType, eventData)

        case Beats.OPCUA =>
          OpcUaTransform.fromValues(eventType, eventData)

        case Beats.OPENCTI =>
          CTITransform.fromValues(eventType, eventData)

        case Beats.TLS =>
         TLSTransform.fromValues(eventType, eventData)

        case Beats.THINGS =>
          /*
           * Events originate from ThingsBoard's gateway
           * service and describe device attribute changes.
           *
           * The [ThingsBeat] normalizes gateway events
           * similar to the Fiware format.
           */
          ThingsTransform.fromValues(eventType, eventData)

        case Beats.ZEEK =>
          ZeekTransform.fromValues(eventType, eventData)

        case _ =>
          throw new Exception(s"The provided Works Beat is not supported.")

      }

    }

  }

  /**
   * The default and generic value representation
   *
   * - id
   * - type
   * - data
   */
  def fromPlainValues(event:SseEvent):Option[Seq[Row]] = {

    val seq = Seq(
      event.sseId,
      event.sseType,
      event.sseData)

    val row = Row.fromSeq(seq)
    Some(Seq(row))

  }

}
