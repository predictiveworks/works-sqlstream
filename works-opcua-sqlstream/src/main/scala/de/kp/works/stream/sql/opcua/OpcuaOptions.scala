package de.kp.works.stream.sql.opcua

/**
 * Copyright (c) 2020 - 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

import de.kp.works.stream.sql.RocksPersistence
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.eclipse.milo.opcua.sdk.client.api.identity.{AnonymousProvider, IdentityProvider, UsernameProvider}
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy
import org.rocksdb.RocksDB

import java.nio.file.{Files, Path, Paths}
import scala.collection.JavaConverters.mapAsScalaMapConverter

case class OpcuaAddressCacheInfo(
  maximumSize:Int,
  expireAfterSeconds:Int)

case class OpcuaCertInfo(
  organization:String,
  organizationalUnit:String,
  localityName:String,
  countryCode:String,
  dnsName:String,
  ipAddress:String)

case class OpcuaClientInfo(
  clientId:String,
  connectTimeout:Int,
  endpointUrl:String,
  keepAliveFailuresAllowed:Int,
  requestTimeout:Int,
  subscriptionSamplingInterval:Double,
  updateEndpointUrl:Boolean)

case class OpcuaCredentials(
  user:String, pass:String)

case class OpcuaKeystoreInfo(
  keystoreFile:String,
  keystorePass:String,
  keystoreType:String,
  certAlias:String,
  privateKeyAlias:String)

case class OpcuaMonitorInfo(
  bufferSize:Int,
  dataChangeTrigger:String,
  discardOldest:Boolean,
  samplingInterval:Double)

class OpcuaOptions(options: DataSourceOptions) {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  OpcuaConf.init()

  def getAddressCacheInfo:OpcuaAddressCacheInfo = {
    val cfg = OpcuaConf.getCfg("addressCacheInfo")
    OpcuaAddressCacheInfo(
      maximumSize = cfg.getInt("maximumSize"),
      expireAfterSeconds = cfg.getInt("expireAfterSeconds")
    )
  }

  def getCertInfo:OpcuaCertInfo = {
    val cfg = OpcuaConf.getCfg("certInfo")
    OpcuaCertInfo(
      organization = cfg.getString("organization"),
      organizationalUnit = cfg.getString("organizationalUnit"),
      localityName = cfg.getString("localityName"),
      countryCode = cfg.getString("countryCode"),
      dnsName = cfg.getString("dnsName"),
      ipAddress = cfg.getString("ipAddress")
    )
  }

  def getClientInfo:OpcuaClientInfo = {
    val cfg = OpcuaConf.getCfg("clientInfo")
    OpcuaClientInfo(
      clientId = cfg.getString("clientId"),
      connectTimeout = cfg.getInt("connectTimeout"),
      endpointUrl = cfg.getString("endpointUrl"),
      keepAliveFailuresAllowed = cfg.getInt("keepAliveFailuresAllowed"),
      requestTimeout = cfg.getInt("requestTimeout"),
      subscriptionSamplingInterval = cfg.getDouble("subscriptionSamplingInterval"),
      updateEndpointUrl = cfg.getBoolean("updateEndpointUrl"))
  }

  private def getCredentials:Option[OpcuaCredentials] = {

    val user = settings.get(OPCUA_STREAM_SETTINGS.OPCUA_USER_NAME)
    val pass = settings.get(OPCUA_STREAM_SETTINGS.OPCUA_USER_PASS)

    if (user.isEmpty || pass.isEmpty) None
    else
      Some(OpcuaCredentials(user.get, pass.get))

  }

  def getIdentityProvider: IdentityProvider = {

    val creds = getCredentials
    if (creds.isEmpty) new AnonymousProvider
    else {
      new UsernameProvider(creds.get.user, creds.get.pass)
    }

  }

  def getKeystoreInfo:OpcuaKeystoreInfo = {
    val cfg = OpcuaConf.getCfg("keystoreInfo")
    OpcuaKeystoreInfo(
      keystoreFile = cfg.getString("keystoreFile"),
      keystorePass = cfg.getString("keystorePass"),
      keystoreType = cfg.getString("keystoreType"),
      certAlias = cfg.getString("certAlias"),
      privateKeyAlias = cfg.getString("privateKeyAlias"))
  }

  def getMonitorInfo:OpcuaMonitorInfo = {
    val cfg = OpcuaConf.getCfg("monitorInfo")
    OpcuaMonitorInfo(
      bufferSize = cfg.getInt("bufferSize"),
      dataChangeTrigger = cfg.getString("dataChangeTrigger"),
      discardOldest = cfg.getBoolean("discardOldest"),
      samplingInterval = cfg.getDouble("samplingInterval"))
  }

  def getPersistence:RocksDB = {

    val path = settings.getOrElse(OPCUA_STREAM_SETTINGS.PERSISTENCE, "")
    if (path.isEmpty)
      throw new Exception(s"No persistence path specified.")

    RocksPersistence.getOrCreate(path)

  }

  def getRetryWait:Int = {
    settings.getOrElse(OPCUA_STREAM_SETTINGS.OPCUA_RETRY_WAIT, "5000").toInt
  }

  def getSchemaType:String = {

    val schemaType = settings.getOrElse(OPCUA_STREAM_SETTINGS.SCHEMA_TYPE, "default")
    if (schemaType == "default") schemaType
    else
      throw new Exception(s"Schema type `$schemaType` not supported.")

  }

  def getSecurityPath:Path = {

    val folder = settings.get(OPCUA_STREAM_SETTINGS.OPCUA_SECURITY_FOLDER)
    if (folder.isEmpty)
      throw new Exception("Configuration does not contain the path to security related information.")

    val securityPath = Paths.get(folder.get)
    Files.createDirectories(securityPath)

    if (!Files.exists(securityPath)) {
      throw new Exception(s"Unable to create security directory: $folder")
    }

    securityPath

  }
  /**
   * The configuration of the security policy is
   * part of the dynamic settings (in contrast to
   * file based configurations)
   */
  def getSecurityPolicy:SecurityPolicy = {
    /*
     * The security policies supported by Eclipse Milo
     *
     * - None
     * - Basic128Rsa15
     * - Basic256
     * - Basic256Sha256
     * - Aes128_Sha256_RsaOaep
     * - Aes256_Sha256_RsaPss
     */
    val policy = settings
      .getOrElse(OPCUA_STREAM_SETTINGS.OPCUA_SECURITY_POLICY, "None")

    policy match  {
      case "None" =>
        SecurityPolicy.None
      case "Basic128Rsa15" =>
        SecurityPolicy.Basic128Rsa15
      case "Basic256" =>
        SecurityPolicy.Basic256
      case "Basic256Sha256" =>
        SecurityPolicy.Basic256Sha256
      case "Aes128_Sha256_RsaOaep" =>
        SecurityPolicy.Aes128_Sha256_RsaOaep
      case "Aes256_Sha256_RsaPss" =>
        SecurityPolicy.Aes256_Sha256_RsaPss
      case _ => null
    }

  }
  /**
   * The list of OPC-UA topics to subscribe
   * to during startup. Sample:
   *
   * "node/ns=2;s=ExampleDP_Float.ExampleDP_Arg1",
   * "node/ns=2;s=ExampleDP_Text.ExampleDP_Text1",
   * "path/Objects/Test/+/+",
   */
  def getTopics:List[String] = {

    val topics = settings.get(OPCUA_STREAM_SETTINGS.OPCUA_STARTUP_TOPICS)
    if (topics.isEmpty) List.empty[String]
    else {
      topics.get.split(",").toList

    }
  }

}
