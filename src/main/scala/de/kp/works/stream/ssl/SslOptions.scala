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

import java.security._
import java.security.cert.X509Certificate

import javax.net.ssl._
import org.bouncycastle.jce.provider.BouncyCastleProvider
import scala.collection.JavaConversions._

object SslOptions {

  object Builder {

    /***** KEY & TRUST STORES *****/

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePassword: String,
      keystoreAlgorithm: String):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePassword = Option(keystorePassword),
        keystoreAlgorithm = Option(keystoreAlgorithm))
    }

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePassword: String,
      keystoreAlgorithm: String,
      cipherSuites: List[String]):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePassword = Option(keystorePassword),
        keystoreAlgorithm = Option(keystoreAlgorithm),
        cipherSuites = Option(cipherSuites.toArray))
    }

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePassword: String,
      keystoreAlgorithm: String,
      truststoreFile: String,
      truststoreType: String,
      truststorePassword: String,
      truststoreAlgorithm: String):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePassword = Option(keystorePassword),
        keystoreAlgorithm = Option(keystoreAlgorithm),
        truststoreFile = Option(truststoreFile),
        truststoreType = Option(truststoreType),
        truststorePassword = Option(truststorePassword),
        truststoreAlgorithm = Option(truststoreAlgorithm))
    }

    def buildStoreOptions(
      tlsVersion: String,
      keystoreFile: String,
      keystoreType: String,
      keystorePassword: String,
      keystoreAlgorithm: String,
      truststoreFile: String,
      truststoreType: String,
      truststorePassword: String,
      truststoreAlgorithm: String,
      cipherSuites: List[String]):SslOptions = {

      new SslOptions(
        tlsVersion = tlsVersion,
        keystoreFile = Option(keystoreFile),
        keystoreType = Option(keystoreType),
        keystorePassword = Option(keystorePassword),
        keystoreAlgorithm = Option(keystoreAlgorithm),
        truststoreFile = Option(truststoreFile),
        truststoreType = Option(truststoreType),
        truststorePassword = Option(truststorePassword),
        truststoreAlgorithm = Option(truststoreAlgorithm),
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
  val keystorePassword: Option[String] = None,
  val keystoreAlgorithm: Option[String] = None,

  /* TRUST STORE */

  val truststoreFile: Option[String] = None,
  val truststoreType: Option[String] = None,
  val truststorePassword: Option[String] = None,
  val truststoreAlgorithm: Option[String] = None,

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

      if (keystoreFile.isDefined && keystoreType.isDefined && keystorePassword.isDefined && keystoreAlgorithm.isDefined) {
        /*
         * SSL authentication based on an existing key store
         */
        val ksFile = keystoreFile.get
        val ksType = keystoreType.get

        val ksPass = keystorePassword.get
        val ksAlgo = keystoreAlgorithm.get

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

      if (truststoreFile.isDefined && truststoreType.isDefined && truststorePassword.isDefined && truststoreAlgorithm.isDefined) {
        /*
         * SSL authentication based on an existing trust store
         */
        val tsFile = truststoreFile.get
        val tsType = truststoreType.get

        val tsPass = truststorePassword.get
        val tsAlgo = truststoreAlgorithm.get

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