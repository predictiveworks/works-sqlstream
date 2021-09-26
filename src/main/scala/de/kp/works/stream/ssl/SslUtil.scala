package de.kp.works.stream.ssl
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

import com.google.common.base.Strings
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import org.bouncycastle.openssl.{PEMEncryptedKeyPair, PEMKeyPair, PEMParser}

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.nio.file.{Files, Paths}
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, KeyStore, PrivateKey, Security}
import javax.net.ssl._

object SslUtil {

  val CA_CERT_ALIAS = "caCert-cert"
  val CERT_ALIAS    = "cert"

  val PRIVATE_KEY_ALIAS = "private-key"

  /** * KEY STORE ONLY ** */

  def getStoreSslSocketFactory(keystoreFile: String, keystoreType: String, keystorePassword: String, keystoreAlgorithm: String, tlsVersion: String): SSLSocketFactory = {

    val sslContext = SSLContext.getInstance(tlsVersion)

    /* Build Key Managers */
    var keyManagers:Array[KeyManager] = null

    keyManagers = getStoreKeyManagerFactory(keystoreFile, keystoreType, keystorePassword, keystoreAlgorithm).getKeyManagers

    sslContext.init(keyManagers, null, null)
    sslContext.getSocketFactory

  }

  /** * KEY & TRUST STORE ** */

  def getStoreSslSocketFactory(keystoreFile: String, keystoreType: String, keystorePassword: String, keystoreAlgorithm: String, truststoreFile: String, truststoreType: String, truststorePassword: String, truststoreAlgorithm: String, tlsVersion: String): SSLSocketFactory = {

    val sslContext = SSLContext.getInstance(tlsVersion)

    /* Build Key Managers */
    var keyManagers:Array[KeyManager] = null

    val keyManagerFactory = getStoreKeyManagerFactory(keystoreFile, keystoreType, keystorePassword, keystoreAlgorithm)
    keyManagers = keyManagerFactory.getKeyManagers

    /* Build Trust Managers */
    var trustManagers:Array[TrustManager] = null

    val trustManagerFactory = getStoreTrustManagerFactory(truststoreFile, truststoreType, truststorePassword, truststoreAlgorithm)
    if (trustManagerFactory != null) trustManagers = trustManagerFactory.getTrustManagers

    sslContext.init(keyManagers, trustManagers, null)
    sslContext.getSocketFactory

  }

  /** * CERTIFICATE FILES * */

  def getCertFileSslSocketFactory(caCrtFile: String, crtFile: String, keyFile: String, password: String, tlsVersion: String): SSLSocketFactory = {

    Security.addProvider(new BouncyCastleProvider)

    val trustManagerFactory = getCertFileTrustManagerFactory(caCrtFile)
    val keyManagerFactory = getCertFileKeyManagerFactory(crtFile, keyFile, password)

    val sslContext = SSLContext.getInstance(tlsVersion)
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, null)

    /*
     * Set connect options to use the TLS
     * enabled socket factory
     */
    sslContext.getSocketFactory

  }

  /** * CERTIFICATES ** */

  def getCertSslSocketFactory(caCert: X509Certificate, cert: X509Certificate, privateKey: PrivateKey, password: String, tlsVersion: String): SSLSocketFactory = {

    Security.addProvider(new BouncyCastleProvider)

    val trustManagerFactory = getCertTrustManagerFactory(caCert)
    val keyManagerFactory = getCertKeyManagerFactory(cert, privateKey, password)

    val sslContext = SSLContext.getInstance(tlsVersion)
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, null)

    /*
     * Set connect options to use the TLS
     * enabled socket factory
     */
    sslContext.getSocketFactory

  }

  /** *** KEY MANAGER FACTORY **** */

  def getStoreKeyManagerFactory(keystoreFile: String, keystoreType: String, keystorePassword: String, keystoreAlgorithm: String): KeyManagerFactory = {

    var keystore = loadKeystore(keystoreFile, keystoreType, keystorePassword)
    /*
     * We have to manually fall back to default keystore. SSLContext won't provide
     * such a functionality.
     */
    if (keystore == null) {
      val ksFile = System.getProperty("javax.net.ssl.keyStore")
      val ksType = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType)
      val ksPass = System.getProperty("javax.net.ssl.keyStorePassword", "")

      keystore = loadKeystore(ksFile, ksType, ksPass)

    }
    val ksAlgo =
      if (Strings.isNullOrEmpty(keystoreAlgorithm)) KeyManagerFactory.getDefaultAlgorithm
      else keystoreAlgorithm

    val keyManagerFactory = KeyManagerFactory.getInstance(ksAlgo)
    val passwordArr =
      if (keystorePassword == null) null
      else keystorePassword.toCharArray

    keyManagerFactory.init(keystore, passwordArr)
    keyManagerFactory

  }

  def getCertFileKeyManagerFactory(crtFile: String, keyFile: String, password: String): KeyManagerFactory = {

    val cert = getX509CertFromPEM(crtFile)
    val privateKey = getPrivateKeyFromPEM(keyFile, password)

    getCertKeyManagerFactory(cert, privateKey, password)

  }

  def getCertKeyManagerFactory(cert: X509Certificate, privateKey: PrivateKey, password: String): KeyManagerFactory = {

    val ks = createKeystore
    /*
     * Add client certificate to key store, the client certificate alias is
     * 'certificate' (see IBM Watson IoT platform)
     */
    ks.setCertificateEntry(CERT_ALIAS, cert)
    /*
     * Add private key to keystore and distinguish between use case with and without
     * password
     */
    val passwordArray =
      if (password != null) {
        password.toCharArray
      }
      else {
        new Array[Char](0)
      }

    ks.setKeyEntry(PRIVATE_KEY_ALIAS, privateKey, passwordArray, Array(cert))
    /*
     * Initialize key manager from the key store; note, the default algorithm also
     * supported by IBM Watson IoT platform is PKIX
     */
    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)

    keyManagerFactory.init(ks, passwordArray)
    keyManagerFactory
  }

  /** *** TRUST MANAGER FACTORY **** */

  def getAllTrustManagers: Array[TrustManager] = {
    Array[TrustManager](new AllTrustManager())
  }

  def getStoreTrustManagerFactory(truststoreFile: String, truststoreType: String, truststorePassword: String, truststoreAlgorithm: String): TrustManagerFactory = {

    var factory:TrustManagerFactory = null
    val trustStore = loadKeystore(truststoreFile, truststoreType, truststorePassword)

    if (trustStore != null) {
      val trustStoreAlgorithm =
        if (Strings.isNullOrEmpty(truststoreAlgorithm)) TrustManagerFactory.getDefaultAlgorithm
        else truststoreAlgorithm

      factory = TrustManagerFactory.getInstance(trustStoreAlgorithm)
      factory.init(trustStore)

    }

    factory

  }

  def getCertFileTrustManagerFactory(caCrtFile: String): TrustManagerFactory = {

    val caCert = getX509CertFromPEM(caCrtFile)
    getCertTrustManagerFactory(caCert)

  }

  def getCertTrustManagerFactory(caCert: X509Certificate): TrustManagerFactory = {

    val ks = createKeystore
    /*
     * Add CA certificate to keystore; note, the CA certificate alias is set to
     * 'ca-certificate' (see IBM Watson IoT platform)
     */
    ks.setCertificateEntry(CA_CERT_ALIAS, caCert)
    /*
     * Establish certificate trust chain; note, the default algorithm also supported
     * by IBM Watson IoT platform is PKIX
     */
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)

    trustManagerFactory.init(ks)
    trustManagerFactory

  }

  /** *** X509 CERTIFICATE **** */

  def getX509CertFromPEM(crtFile: String): X509Certificate = {
    /*
     * Since Java cannot read PEM formatted certificates, this method is using
     * bouncy castle (http://www.bouncycastle.org/) to load the necessary files.
     *
     * IMPORTANT: Bouncycastle Provider must be added before this method can be
     * called
     *
     */
    val bytes = Files.readAllBytes(Paths.get(crtFile))
    val bais = new ByteArrayInputStream(bytes)

    val reader = new PEMParser(new InputStreamReader(bais))
    val cert = reader.readObject.asInstanceOf[X509Certificate]

    reader.close()
    cert

  }

  /** *** PRIVATE KEY **** */

  def getPrivateKeyFromPEM(keyFile: String, password: String): PrivateKey = {

    val bytes = Files.readAllBytes(Paths.get(keyFile))
    val bais = new ByteArrayInputStream(bytes)

    val reader = new PEMParser(new InputStreamReader(bais))
    val keyObject = reader.readObject

    reader.close()

    var keyPair:PEMKeyPair = null
    keyObject match {
      case pair: PEMEncryptedKeyPair =>
        if (password == null)
          throw new Exception("[ERROR] Reading private key from file without password is not supported.")
        val passwordArray = password.toCharArray
        val provider = new JcePEMDecryptorProviderBuilder().build(passwordArray)
        keyPair = pair.decryptKeyPair(provider)
      case _ => keyPair = keyObject.asInstanceOf[PEMKeyPair]
    }

    val factory = KeyFactory.getInstance("RSA", "BC")
    val keySpec = new PKCS8EncodedKeySpec(keyPair.getPrivateKeyInfo.getEncoded)

    factory.generatePrivate(keySpec)

  }

  /**
   * Load a Java KeyStore located at keystoreFile of keystoreType and
   * keystorePassword
   */
  def loadKeystore(keystoreFile: String, keystoreType: String, keystorePassword: String): KeyStore = {

    var keystore:KeyStore = null
    if (keystoreFile != null) {

      keystore = KeyStore.getInstance(keystoreType)
      val passwordArr = if (keystorePassword == null || keystorePassword.isEmpty) null
      else keystorePassword.toCharArray

      val is = Files.newInputStream(Paths.get(keystoreFile))

      try keystore.load(is, passwordArr)
      finally if (is != null) is.close()

    }

    keystore

  }

  def createKeystore:KeyStore = {
    /*
     * Create a default (JKS) keystore without any password.
     * Method load(null, null) indicates to create a new one
     */
    val keystore = KeyStore.getInstance(KeyStore.getDefaultType)
    keystore.load(null, null)
    keystore

  }

}
