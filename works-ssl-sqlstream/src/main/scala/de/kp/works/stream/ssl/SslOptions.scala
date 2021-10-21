package de.kp.works.stream.ssl

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

import com.typesafe.config.Config

import java.security._
import java.security.cert.X509Certificate
import javax.net.ssl._
import org.bouncycastle.jce.provider.BouncyCastleProvider

import scala.collection.JavaConversions._

object SslOptions {

  def buildSslContext(securityCfg:Config):SSLContext = {

    val sslOptions = getSslOptions(securityCfg)
    sslOptions.getSslContext

  }

  def getSslOptions(securityCfg:Config): SslOptions = {

    val tlsVersion = {
      val v = securityCfg.getString("tlsVersion")
      if (v.isEmpty) "TLS" else v
    }

    val ksFile = {
      val v = securityCfg.getString("ksFile")
      if (v.isEmpty) None else Option(v)
    }

    val ksType = {
      val v = securityCfg.getString("ksType")
      if (v.isEmpty) None else Option(v)
    }

    val ksPass = {
      val v = securityCfg.getString("ksPass")
      if (v.isEmpty) None else Option(v)
    }

    val ksAlgo = {
      val v = securityCfg.getString("ksAlgo")
      if (v.isEmpty) None else Option(v)
    }

    val tsFile = {
      val v = securityCfg.getString("tsFile")
      if (v.isEmpty) None else Option(v)
    }

    val tsType = {
      val v = securityCfg.getString("tsType")
      if (v.isEmpty) None else Option(v)
    }

    val tsPass = {
      val v = securityCfg.getString("tsPass")
      if (v.isEmpty) None else Option(v)
    }

    val tsAlgo = {
      val v = securityCfg.getString("tsAlgo")
      if (v.isEmpty) None else Option(v)
    }

    val caCertFile = {
      val v = securityCfg.getString("caCertFile")
      if (v.isEmpty) None else Option(v)
    }

    val certFile = {
      val v = securityCfg.getString("certFile")
      if (v.isEmpty) None else Option(v)
    }

    val privateKeyFile = {
      val v = securityCfg.getString("privateKeyFile")
      if (v.isEmpty) None else Option(v)
    }

    val privateKeyFilePass = {
      val v = securityCfg.getString("privateKeyFilePass")
      if (v.isEmpty) None else Option(v)
    }

    new SslOptions(
      tlsVersion   = tlsVersion,

      /* KEYSTORE SUPPORT */
      keystoreFile = ksFile,
      keystoreType = ksType,
      keystorePass = ksPass,
      keystoreAlgo = ksAlgo,

      /* TRUSTSTORE SUPPORT */
      truststoreFile = tsFile,
      truststoreType = tsType,
      truststorePass = tsPass,
      truststoreAlgo = tsAlgo,

      /* CERTIFICATE SUPPORT */
      caCertFile         = caCertFile,
      certFile           = certFile,
      privateKeyFile     = privateKeyFile,
      privateKeyFilePass = privateKeyFilePass)
  }

  object Builder {

    /***** KEY & TRUST STORES *****/

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePass: String,
      keystoreAlgo: String):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePass = Option(keystorePass),
        keystoreAlgo = Option(keystoreAlgo))
    }

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePass: String,
      keystoreAlgo: String,
      cipherSuites: List[String]):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePass = Option(keystorePass),
        keystoreAlgo = Option(keystoreAlgo),
        cipherSuites = Option(cipherSuites.toArray))
    }

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePass: String,
      keystoreAlgorithm: String,
      truststoreFile: String,
      truststoreType: String,
      truststorePass: String,
      truststoreAlgo: String):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePass = Option(keystorePass),
        keystoreAlgo = Option(keystoreAlgorithm),
        truststoreFile = Option(truststoreFile),
        truststoreType = Option(truststoreType),
        truststorePass = Option(truststorePass),
        truststoreAlgo = Option(truststoreAlgo))
    }

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePass: String,
      keystoreAlgo: String,
      truststoreFile: String,
      truststoreType: String,
      truststorePass: String,
      truststoreAlgo: String,
      cipherSuites: List[String]):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePass = Option(keystorePass),
        keystoreAlgo = Option(keystoreAlgo),
        truststoreFile = Option(truststoreFile),
        truststoreType = Option(truststoreType),
        truststorePass = Option(truststorePass),
        truststoreAlgo = Option(truststoreAlgo),
        cipherSuites = Option(cipherSuites.toArray))
    }

    /***** CERTIFICATES *****/

    def buildCertOptions(
      tlsVersion: String,
      caCert: X509Certificate,
      cert: X509Certificate,
      privateKey: PrivateKey,
      privateKeyPass: String):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        caCert = Option(caCert),
        cert = Option(cert),
        privateKey = Option(privateKey),
        privateKeyPass = Option(privateKeyPass))

    }

    def buildCertOptions(
      tlsVersion: String,
      caCert: X509Certificate,
      cert: X509Certificate,
      privateKey: PrivateKey,
      privateKeyPass: String,
      cipherSuites: List[String]):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        caCert = Option(caCert),
        cert = Option(cert),
        privateKey = Option(privateKey),
        privateKeyPass = Option(privateKeyPass),
        cipherSuites = Option(cipherSuites.toArray))

    }

    /***** CERTIFICATE FILES *****/

    def buildCertFileOptions(
      tlsVersion: String,
      caCertFile: String,
      certFile: String,
      privateKeyFile: String,
      privateKeyFilePass: String):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        caCertFile = Option(caCertFile),
        certFile = Option(certFile),
        privateKeyFile = Option(privateKeyFile),
        privateKeyFilePass = Option(privateKeyFilePass))

    }

    def buildCertFileOptions(
      tlsVersion: String,
      caCertFile: String,
      certFile: String,
      privateKeyFile: String,
      privateKeyFilePass: String,
      cipherSuites: List[String]):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        caCertFile = Option(caCertFile),
        certFile = Option(certFile),
        privateKeyFile = Option(privateKeyFile),
        privateKeyFilePass = Option(privateKeyFilePass),
        cipherSuites = Option(cipherSuites.toArray))

    }

  }
}

class SslOptions(

  val tlsVersion: String,

  /* KEY STORE */
  val keystoreFile: Option[String] = None,
  val keystoreType: Option[String] = None,
  val keystorePass: Option[String] = None,
  val keystoreAlgo: Option[String] = None,

  /* TRUST STORE */

  val truststoreFile: Option[String] = None,
  val truststoreType: Option[String] = None,
  val truststorePass: Option[String] = None,
  val truststoreAlgo: Option[String] = None,

  /* CERTIFICATES */

  val caCert: Option[X509Certificate] = None,
  val cert: Option[X509Certificate] = None,
  val privateKey: Option[PrivateKey] = None,
  val privateKeyPass: Option[String] = None,

  /* CERTIFICATES FILES */

  val caCertFile: Option[String] = None,
  val certFile: Option[String] = None,
  val privateKeyFile: Option[String] = None,
  val privateKeyFilePass: Option[String] = None,

  val cipherSuites: Option[Array[String]] = None) {

  def getCipherSuites: java.util.List[String] = {

    if (cipherSuites.isDefined) {

      if (cipherSuites.get.nonEmpty)
          cipherSuites.get.toList

      else null

    }
    else
      null

  }

  def getHostnameVerifier: HostnameVerifier = {
    /*
     * Use https hostname verification.
     */
    null

  }

  def getSslContext: SSLContext = {

    Security.addProvider(new BouncyCastleProvider())

    var keyManagers:Array[KeyManager] = null
    var trustManagers:Array[TrustManager] = null

    /** KEY STORE **/

    val keyManagerFactory = getKeyManagerFactory
    if (keyManagerFactory != null)
      keyManagers = keyManagerFactory.getKeyManagers

    /** TRUST STORE **/

    val trustManagerFactory = getTrustManagerFactory
    if (trustManagerFactory != null)
      trustManagers = trustManagerFactory.getTrustManagers

    val secureRandom = Option(new SecureRandom())
    buildSslContext(keyManagers, trustManagers, secureRandom)

  }

  private def buildSslContext(keyManagers:Seq[KeyManager], trustManagers:Seq[TrustManager], secureRandom:Option[SecureRandom]) = {

    val sslContext = SSLContext.getInstance(tlsVersion)

    sslContext.init(nullIfEmpty(keyManagers.toArray), nullIfEmpty(trustManagers.toArray), secureRandom.orNull)
    sslContext

  }

  private def nullIfEmpty[T](array: Array[T]) = {
    if (array.isEmpty) null else array
  }

  def getSslSocketFactory: SSLSocketFactory = {

		Security.addProvider(new BouncyCastleProvider())

		val keyManagerFactory = getKeyManagerFactory
		val trustManagerFactory = getTrustManagerFactory

		val sslContext = SSLContext.getInstance(tlsVersion)
		sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new java.security.SecureRandom())

		sslContext.getSocketFactory

  }


  def getKeyManagerFactory: KeyManagerFactory = {

    try {

      if (keystoreFile.isDefined && keystoreType.isDefined && keystorePass.isDefined && keystoreAlgo.isDefined) {
        /*
         * SSL authentication based on an existing key store
         */
        val ksFile = keystoreFile.get
        val ksType = keystoreType.get

        val ksPass = keystorePass.get
        val ksAlgo = keystoreAlgo.get

        SslUtil.getStoreKeyManagerFactory(ksFile, ksType, ksPass, ksAlgo)

      } else if (cert.isDefined && privateKey.isDefined && privateKeyPass.isDefined) {
        /*
         * SSL authentication based on a provided client certificate,
         * private key and associated password; the certificate will
         * be added to a newly created key store
         */
        SslUtil.getCertKeyManagerFactory(cert.get, privateKey.get, privateKeyPass.get)

      } else if (certFile.isDefined && privateKeyFile.isDefined && privateKeyFilePass.isDefined) {
        /*
         * SSL authentication based on a provided client certificate file,
         * private key file and associated password; the certificate will
         * be added to a newly created key store
         */
        SslUtil.getCertFileKeyManagerFactory(certFile.get, privateKeyFile.get, privateKeyFilePass.get)

      } else
        throw new Exception("Failed to retrieve KeyManager factory.")

    } catch {

      case _: Throwable =>
        /* Do nothing */
        null
    }

  }

  def getTrustManagerFactory: TrustManagerFactory = {

    try {

      if (truststoreFile.isDefined && truststoreType.isDefined && truststorePass.isDefined && truststoreAlgo.isDefined) {
        /*
         * SSL authentication based on an existing trust store
         */
        val tsFile = truststoreFile.get
        val tsType = truststoreType.get

        val tsPass = truststorePass.get
        val tsAlgo = truststoreAlgo.get

        SslUtil.getStoreTrustManagerFactory(tsFile, tsType, tsPass, tsAlgo)
        
      } else if (caCert.isDefined) {
        /*
         * SSL authentication based on a provided CA certificate;
         * this certificate will be added to a newly created trust
         * store
         */
        SslUtil.getCertTrustManagerFactory(caCert.get)

      } else if (caCertFile.isDefined) {
        /*
         * SSL authentication based on a provided CA certificate file;
         * the certificate is loaded and will be added to a newly created 
         * trust store
         */
        SslUtil.getCertFileTrustManagerFactory(caCertFile.get)
      
      } else 
        throw new Exception("Failed to retrieve TrustManager factory.")

    } catch {

      case _: Throwable =>
        /* Do nothing */
        null
    }

  }
}