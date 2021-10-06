package de.kp.works.stream.server.session
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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.StreamingQueryListener

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration

/**
 * [StreamSession] is a wrapper for Apache Spark Session
 * and complements this session with streaming specific
 * methods
 */
class StreamSession(val session:SparkSession, connTimeout:Int) {
  /**
   * The name of the actor system that is used to control
   * the actor-based streaming query listener
   */
  private val systemName = "stream-session"
  /**
   * Unpack the streaming manager
   */
  private val streams = session.streams
  /**
   * In version 2.6, Akka provides a default materializer
   * out of the box, i.e., for Scala an implicit materializer
   * is provided if there is an implicit ActorSystem available.
   *
   * This avoids leaking materializers and simplifies most stream
   * use cases somewhat.
   */
  implicit val system: ActorSystem = ActorSystem(systemName)
  implicit lazy val context: ExecutionContextExecutor = system.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  /**
   * Common timeout for all Akka connection
   */
  implicit val timeout: Timeout =
    Timeout(new FiniteDuration(connTimeout, TimeUnit.SECONDS))

  /**
   * This method wraps the respective streaming manager
   * method to add listeners
   */
  def addListener(listener:StreamingQueryListener):Unit = {
    streams.addListener(listener)
  }

}
