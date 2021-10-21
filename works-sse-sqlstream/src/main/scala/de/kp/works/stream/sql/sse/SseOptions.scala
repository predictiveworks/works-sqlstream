package de.kp.works.stream.sql.sse

/*
 * Copyright (c) 2019 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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
import de.kp.works.stream.ssl.SslOptions
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.rocksdb.RocksDB

import scala.collection.JavaConverters.mapAsScalaMapConverter

class SseOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def isSsl:Boolean = {

    var ssl:Boolean = false
    settings.keySet.foreach(key => {
      if (key.startsWith("ssl.")) ssl = true
    })

    ssl

  }

  def getOAuthToken:Option[String] =
    settings.get(SSE_STREAM_SETTINGS.AUTH_TOKEN)

  def getPersistence:RocksDB = {

    val path = settings.getOrElse(SSE_STREAM_SETTINGS.PERSISTENCE, "")
    if (path.isEmpty)
      throw new Exception(s"No persistence path specified.")

    SsePersistence.getOrCreate(path)

  }

  def getSchemaType:String =
    settings
      .getOrElse(SSE_STREAM_SETTINGS.SCHEMA_TYPE, "plain")
      .toLowerCase

  def getServerUrl:String = {
    val url = settings.get(SSE_STREAM_SETTINGS.SERVER_URL)
    if (url.isEmpty)
      throw new Exception(s"No server url specified.")

    url.get
  }

  def getSslOptions:SslOptions = {

    val tlsVersion = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_PROTOCOL, "TLS")

    /*
     * The keystore file must be defined
     */
    val keyStoreFile = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_KEYSTORE_FILE,
        throw new Exception(s"No keystore file specified."))
    /*
     * - JKS      Java KeyStore
     * - JCEKS    Java Cryptography Extension KeyStore
     * - PKCS #12 Public-Key Cryptography Standards #12 KeyStore
     * - BKS      Bouncy Castle KeyStore
     * - BKS-V1   Older and incompatible version of Bouncy Castle KeyStore
     */
    val keyStoreType = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_KEYSTORE_TYPE, "JKS")

    val keyStorePass = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_KEYSTORE_PASS, "")

    val keyStoreAlgo = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_KEYSTORE_ALGO, "SunX509")

    val trustStoreFile = settings.get(SSE_STREAM_SETTINGS.SSL_TRUSTSTORE_FILE)
    /*
     * - JKS      Java KeyStore
     * - JCEKS    Java Cryptography Extension KeyStore
     * - PKCS #12 Public-Key Cryptography Standards #12 KeyStore
     * - BKS      Bouncy Castle KeyStore
     * - BKS-V1   Older and incompatible version of Bouncy Castle KeyStore
     */
    val trustStoreType = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_TRUSTSTORE_TYPE, "JKS")

    val trustStorePass = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_TRUSTSTORE_PASS, "")

    val trustStoreAlgo = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_TRUSTSTORE_ALGO, "SunX509")

    val cipherSuites = settings
      .getOrElse(SSE_STREAM_SETTINGS.SSL_CIPHER_SUITES, "")
      .split(",").map(_.trim).toList

    val sslOptions = if (trustStoreFile.isEmpty) {
      SslOptions.Builder.buildStoreOptions(
        tlsVersion,
        keyStoreFile,
        keyStoreType,
        keyStorePass,
        keyStoreAlgo,
        cipherSuites
      )
    }
    else {
      SslOptions.Builder.buildStoreOptions(
        tlsVersion,
        keyStoreFile,
        keyStoreType,
        keyStorePass,
        keyStoreAlgo,
        trustStoreFile.get,
        trustStoreType,
        trustStorePass,
        trustStoreAlgo,
        cipherSuites)

    }

    sslOptions

  }

}
