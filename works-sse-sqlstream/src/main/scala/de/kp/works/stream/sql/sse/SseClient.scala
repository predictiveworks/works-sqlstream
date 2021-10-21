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

import de.kp.works.stream.sql.Logging
import de.kp.works.stream.ssl.SslUtil
import okhttp3.sse.{EventSource, EventSourceListener, EventSources}
import okhttp3.{OkHttpClient, Request, Response}

import javax.net.ssl.SSLContext

object SseClient {

  def build(options:SseOptions):SseClient =
    new SseClient(options)

}

class SseClient(options:SseOptions) extends Logging {

  private var expose:SseExpose = _
  private var httpClient:OkHttpClient = _

  def setExpose(sseExpose:SseExpose):Unit = {
    expose = sseExpose
  }

  def connect():Unit = {
    /*
     * Build HTTP client and optionally authenticated request
     */
    httpClient = getHttpClient
    val request = getRequest

    /** SSE **/

    val factory = EventSources.createFactory(httpClient)
    val listener = new EventSourceListener() {

      override def onOpen(eventSource:EventSource, response:Response):Unit = {
        /* Do nothing */
      }

      override def onEvent(eventSource:EventSource, id:String, `type`:String, data:String):Unit = {
        val event = SseEvent(id, `type`, data)
        expose.eventArrived(event)
      }

      override def onClosed(eventSource:EventSource) {
        /* Do nothing */
      }

      override def onFailure(eventSource:EventSource, t:Throwable, response:Response): Unit = {
        /*
         * Re-initialize the HTTP client and the respective
         * request and re-start with a fresh listener
         */
        reconnect()
      }

    }

    factory.newEventSource(request, listener)

  }

  private def reconnect():Unit = connect()

  def disconnect():Unit = {
    /* Do nothing */
  }

  /** HELPER METHODS **/

  private def getHttpClient:OkHttpClient = {

    if (options.isSsl)
      createSafeClient

    else
      createUnsafeClient

  }

  private def getRequest:Request = {
    /*
     * Build request with an optional authentication token
     */
    val builder = new Request.Builder()
      .url(options.getServerUrl)

    val request = {
      val authToken = options.getOAuthToken
      if (authToken.isDefined)
        builder
          .addHeader("Authorization", "Bearer " + authToken.get)
      else
        builder

    }.build

    request

  }

  private def createUnsafeClient:OkHttpClient = {

    try {

      val allTrustManagers = SslUtil.getAllTrustManagers
      val sslContext =  SSLContext.getInstance("TLS")

      sslContext.init(null, allTrustManagers, new java.security.SecureRandom())
      val sslSocketFactory = sslContext.getSocketFactory

      val builder = new OkHttpClient.Builder()

      val x509TrustManager = allTrustManagers(0).asInstanceOf[javax.net.ssl.X509TrustManager]
      builder.sslSocketFactory(sslSocketFactory, x509TrustManager)

      builder.build

    } catch {
      case t:Throwable => null
    }

  }

  private def createSafeClient:OkHttpClient = {

    try {

      val sslOptions = options.getSslOptions

      val sslSocketFactory = sslOptions.getSslSocketFactory
      val x509TrustManager = sslOptions.getTrustManagerFactory
        .getTrustManagers()(0).asInstanceOf[javax.net.ssl.X509TrustManager]

      val builder = new OkHttpClient.Builder()
      builder.sslSocketFactory(sslSocketFactory, x509TrustManager)

      builder.build

    } catch {
      case _:Throwable => null
    }

  }


}
