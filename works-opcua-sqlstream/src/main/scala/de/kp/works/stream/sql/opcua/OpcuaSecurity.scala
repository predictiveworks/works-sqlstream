package de.kp.works.stream.sql.opcua
/**
 * Copyright (c) 2019 - 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder

import java.nio.file.{Files, Path}
import java.security.cert.X509Certificate
import java.security.{KeyPair, KeyPairGenerator, KeyStore, PrivateKey}
import java.util.regex.Pattern

class OpcuaSecurity(options:OpcuaOptions) {
  /**
   * The security policy determines how to create the
   * connection to the UA server
   */
  private val securityPolicy:SecurityPolicy = options.getSecurityPolicy

  private var clientCertificate:X509Certificate = _
  private var clientKeyPair:KeyPair = _

  private val IP_ADDR_PATTERN = Pattern.compile(
    "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")

  init()

  def getClientCertificate:X509Certificate = {
    clientCertificate
  }

  def getClientKeyPair:KeyPair = {
    clientKeyPair
  }

  private def init(): Unit = {

    try {

      val path = options.getSecurityPath
      load(path)

    } catch {
      case _:Throwable => /* Do nothing */
    }
  }
  /**
   * Extract client certificate and key pair from the
   * configured keystore; if the keystore does not exist,
   * it is created, a self-signed certificate and the key
   * pair.
   */
  private def load(path:Path):Unit = {

    val ksInfo = options.getKeystoreInfo

    val ksPath = path.resolve(ksInfo.keystoreFile)
    val keystore = if (!Files.exists(ksPath)) {
      /*
       * Create a new key pair with a key size that matches
       * the provided security policy
       */
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(getKeySize)
      /*
       * Create a new certificate
       */
      val keyPair = keyPairGenerator.generateKeyPair()
      val certificate = buildCertificate(keyPair)
      /*
       * Create keystore and assign key pair and certificate
       */
      val ks = createKeystore(ksInfo.keystoreType, ksInfo.keystorePass)
      /*
       * Add client certificate to key store, the client certificate alias is
       * 'certificate' (see IBM Watson IoT platform)
       */
      ks.setCertificateEntry(ksInfo.certAlias, certificate)
      /*
       * Add private key to keystore and distinguish between use case with and without
       * password
       */
      ks.setKeyEntry(
        ksInfo.privateKeyAlias,
        keyPair.getPrivate,
        ksInfo.keystorePass.toCharArray, Array(certificate))

      val out = Files.newOutputStream(ksPath)
      ks.store(out, ksInfo.keystorePass.toCharArray)

      ks

    }
    else {
      loadKeystore(ksPath, ksInfo.keystoreType, ksInfo.keystorePass)

    }

    val privateKey = keystore.getKey(ksInfo.privateKeyAlias, ksInfo.keystorePass.toCharArray)
    privateKey match {
      case key: PrivateKey =>

        clientCertificate = keystore.getCertificate(ksInfo.certAlias).asInstanceOf[X509Certificate]
        val serverPublicKey = clientCertificate.getPublicKey

        clientKeyPair = new KeyPair(serverPublicKey, key)
      case _ =>
    }

  }
  /**
   * A helper method to build a self-signed certificate
   * from the provided certificate meta information.
   */
  private def buildCertificate(keyPair:KeyPair):X509Certificate = {

    val certInfo = options.getCertInfo
    val signatureAlgorithm = getSignatureAlgorithm

    val builder = new SelfSignedCertificateBuilder(keyPair)
      .setSignatureAlgorithm(signatureAlgorithm)
      /*
       * Set certificate information from provided
       * configuration and application specification
       */
      .setCommonName(OpcuaUtils.APPLICATION_NAME)
      .setApplicationUri(OpcuaUtils.APPLICATION_URI)
      /*
       * Assign certificate info to certificate builder
       */
      .setOrganization(certInfo.organization)
      .setOrganizationalUnit(certInfo.organizationalUnit)
      .setLocalityName(certInfo.localityName)
      .setCountryCode(certInfo.countryCode)
      /*
       * Append DNS name and IP address
       */
      .addDnsName(certInfo.dnsName)
      .addIpAddress(certInfo.ipAddress)

    /*
     * Retrieve  as many hostnames and IP addresses
     * to be listed in the certificate.
     */
    OpcuaUtils.getHostnames("0.0.0.0").foreach(hostname => {
      if (IP_ADDR_PATTERN.matcher(hostname).matches()) {
        builder.addIpAddress(hostname)

      } else {
        builder.addDnsName(hostname)
      }

    })

    builder.build()

  }

  private def getKeySize:Int = {
    if (securityPolicy.getUri.equals(SecurityPolicy.Basic128Rsa15.getUri)) {
      1024
    }
    else
      2048
  }

  private def getSignatureAlgorithm:String = {
    /*
     * Define the algorithm to use for certificate signatures.
     *
     * The OPC UA specification defines that the algorithm should be (at least)
     * "SHA1WithRSA" for application instance certificates used for security
     * policies Basic128Rsa15 and Basic256. For Basic256Sha256 it should be
     * "SHA256WithRSA".
     *
     */
    val uri = securityPolicy.getUri
    if (uri.equals(SecurityPolicy.None.getUri)) {
      "SHA1WithRSA"
    }
    else if (uri.equals(SecurityPolicy.Basic128Rsa15.getUri)) {
      "SHA1WithRSA"
    }
    else if (uri.equals(SecurityPolicy.Basic256.getUri)) {
      "SHA1WithRSA"
    }
    else {
      "SHA256WithRSA"
    }

  }

  private def createKeystore(ksType:String, ksPass:String):KeyStore =  {

    val keystore = KeyStore.getInstance(ksType)
    val password = if (ksPass == null) null else ksPass.toCharArray

    keystore.load(null, password)
    keystore

  }

  private def loadKeystore(ksPath:Path, ksType:String, ksPass:String):KeyStore = {

    if (ksPath != null) {

      val keystore = KeyStore.getInstance(ksType)
      val password = if (ksPass == null) null else ksPass.toCharArray

      val is = Files.newInputStream(ksPath)
      keystore.load(is, password)

      keystore

    } else null

  }

}
