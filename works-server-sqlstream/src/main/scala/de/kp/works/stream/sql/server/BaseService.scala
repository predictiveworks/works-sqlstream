package de.kp.works.stream.sql.server

/*
 * Copyright (c) 2020 Dr. Krusche & Partner PartG. All rights reserved.
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
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config
import de.kp.works.stream.ssl.SslOptions

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

abstract class BaseService(name:String) {

  private var server:Option[Future[Http.ServerBinding]] = None
  /**
   * Akka 2.6 provides a default materializer out of the box, i.e., for Scala
   * an implicit materializer is provided if there is an implicit ActorSystem
   * available. This avoids leaking materializers and simplifies most stream
   * use cases somewhat.
   */
  implicit val system: ActorSystem = ActorSystem(name)
  implicit lazy val context: ExecutionContextExecutor = system.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  /**
   * Common timeout for all Akka connection
   */
  implicit val timeout: Timeout = Timeout(5.seconds)

  def start(config:Option[String] = None):Unit = {

    ServerConf.init(config)

    if (!ServerConf.isInit) {

      val now = new java.util.Date().toString
      throw new Exception(s"[ERROR] $now - Loading configuration failed and stream service is not started.")

    }
    /*
     * Retrieve server configuration
     */
    val serverCfg = ServerConf.getCfg
    /*
     * Build stream server specific routes
     */
    val routes = buildRoute
    val binding = serverCfg.getConfig("binding")

    val host = binding.getString("host")
    val port = binding.getInt("port")

    val security = serverCfg.getConfig("security")
    server =
      if (security.getString("ssl") == "false")
        Some(Http().bindAndHandle(routes , host, port))

      else {
        val context = buildServerConnectionContext(security)
        Some(Http().bindAndHandle(routes, host, port, connectionContext = context))
      }

    /* After start processing */

    onStart(serverCfg)

  }
  /**
   * This method defines the base output `event` route
   * to retrieve the generated Server Sent Events (SSE).
   *
   * This route is always built independent of whether
   * the configured output channel is set to `sse`.
   */
  def buildRoute: Route = ???

  def onStart(cfg:Config):Unit

  def stop():Unit = {

    if (server.isEmpty)
      throw new Exception("Stream service was not launched.")

    server.get
      /*
       * rigger unbinding from port
       */
      .flatMap(_.unbind())
      /*
       * Shut down application
       */
      .onComplete(_ => {
        system.terminate()
      })

  }

  /** AKKA HTTP(S) SERVER SUPPORT **/

  def buildServerConnectionContext(cfg:Config): HttpsConnectionContext = {
    ConnectionContext.https(sslContext = SslOptions.buildSslContext(cfg))
  }

}
