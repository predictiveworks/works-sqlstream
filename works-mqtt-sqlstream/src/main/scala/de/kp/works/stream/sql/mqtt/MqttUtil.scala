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

import de.kp.works.stream.sql.transform.fiware.FiwareTransform
import de.kp.works.stream.sql.transform.fleet.FleetTransform
import de.kp.works.stream.sql.transform.opcua.OpcUaTransform
import de.kp.works.stream.sql.transform.opencti.CTITransform
import de.kp.works.stream.sql.transform.things.ThingsTransform
import de.kp.works.stream.sql.transform.tls.TLSTransform
import de.kp.works.stream.sql.transform.zeek.ZeekTransform
import de.kp.works.stream.sql.transform.{Beats, TransformUtil}
import org.apache.spark.sql.Row

object MqttUtil {
  /**
   * This method transforms a certain [MqttEvent] into
   * a sequence of schema compliant values
   */
  def toRows(event:MqttEvent, schemaType:String):Option[Seq[Row]] = {
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
   * from one of Works Beats via MQTT channel with the
   * best matching representation.
   *
   * Note, the subsequent implementation is similar to
   * the one for SSE support. MQTT & SSE output channels
   * of the Works Beats provide a unified event format.
   */
  def fromBeatsValues(event:MqttEvent):Option[Seq[Row]] = {
    /*
     * Check whether the topic provided with this
     * event originates from a Works Beat:
     *
     *           beat/fiware/notification
     *
     */
    val topic = event.topic
    val tokens = topic.split("\\/")
    /*
     * We expect at least 2 tokens, and that the
     * second one identifies the respective Beat
     */
    val beat = try {
      Beats.withName(tokens(1))

    } catch {
      case _:Throwable => null
    }

    if (beat == null) fromPlainValues(event)
    else {

      val (eventType, eventData) =
        TransformUtil.deserializeMqtt(new String(event.payload, "UTF-8"))
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
       * Transform the received Mqtt event into a
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

  private def fromPlainValues(event:MqttEvent):Option[Seq[Row]] = {

    val seq = Seq(
      event.id,
      event.timestamp,
      event.topic,
      event.qos,
      event.duplicate,
      event.retained,
      event.payload)

    val row = Row.fromSeq(seq)
    Some(Seq(row))

  }

}
