package de.kp.works.stream.server.listener
/*
 * Copyright (c) 2019 Dr. Krusche & Partner PartG. All rights reserved.
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

import akka.actor.Actor
import com.google.gson.{JsonObject, JsonParser}
import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.streaming.StreamingQueryListener

object StreamListenerActor {

  sealed trait StreamEvent

  case class Started(
    event: StreamingQueryListener.QueryStartedEvent) extends StreamEvent

  case class Progress(
    event: StreamingQueryListener.QueryProgressEvent) extends StreamEvent

  case class Terminated(
    event: StreamingQueryListener.QueryTerminatedEvent) extends StreamEvent

}
/**
 * This actor is introduced to receive stream query
 * events from the [StreamListener] and publish them
 * to multiple outputs.
 */
class StreamListenerActor extends Actor with Logging {

  import StreamListenerActor._

  override def receive: Receive = {

    case Started(event) =>
      /*
       * Serialize query event and send to output
       * handler
       */
      val json = new JsonObject
      json.addProperty("id", event.id.toString)

      json.addProperty("name", event.name)
      json.addProperty("runId", event.runId.toString)

      json.addProperty("type", "started")
      send(json.toString)

    case Progress(event) =>
      /*
       * Event progress is a complex data structures
       * that is published as its enriched Json object.
       *
       * For more details, see:
       *
       * org/apache/spark/sql/streaming/progress.scala
       */
      val json = JsonParser
        .parseString(event.progress.json).getAsJsonObject

      json.addProperty("type", "progress")
      send(json.toString)

    case Terminated(event) =>
      /*
       * Serialize query event and send to output
       * handler
       */
      val json = new JsonObject
      json.addProperty("id", event.id.toString)

      json.addProperty("runId", event.runId.toString)
      json.addProperty("exception", "")

      event.exception match {
        case Some(e) => json.addProperty("exception", e)
        case _ => /* Do nothing */
      }

      json.addProperty("type", "terminated")
      send(json.toString)

    case _ =>
      throw new Exception(s"Unknown streaming event detected")

  }

  def send(message:String):Unit = ???

}
