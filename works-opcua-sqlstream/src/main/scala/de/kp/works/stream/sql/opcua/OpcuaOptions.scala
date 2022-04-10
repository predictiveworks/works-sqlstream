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

import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.eclipse.milo.opcua.sdk.client.api.identity.{AnonymousProvider, IdentityProvider, UsernameProvider}
import org.rocksdb.RocksDB

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable

case class OpcuaCredentials(user:String, pass:String)

class OpcuaOptions(options: DataSourceOptions) {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getIdentityProvider: IdentityProvider = {

    val creds = getCredentials
    if (creds.isEmpty) new AnonymousProvider
    else {
      new UsernameProvider(creds.get.user, creds.get.pass)
    }

  }

  private def getCredentials:Option[OpcuaCredentials] = {

    val user = settings.get(OPCUA_STREAM_SETTINGS.OPCUA_USER_NAME)
    val pass = settings.get(OPCUA_STREAM_SETTINGS.OPCUA_USER_PASS)

    if (user.isEmpty || pass.isEmpty) None
    else
      Some(OpcuaCredentials(user.get, pass.get))

  }

  def getPersistence:RocksDB = {

    val path = settings.getOrElse(OPCUA_STREAM_SETTINGS.PERSISTENCE, "")
    if (path.isEmpty)
      throw new Exception(s"No persistence path specified.")

    OpcuaPersistence.getOrCreate(path)

  }

  def getSchemaType:String = {

    val schemaType = settings.getOrElse(OPCUA_STREAM_SETTINGS.SCHEMA_TYPE, "default")
    if (schemaType == "default") schemaType
    else
      throw new Exception(s"Schema type `$schemaType` not supported.")

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

    val topics = settings.get(OPCUA_STREAM_SETTINGS.OPCUA_TOPICS)
    if (topics.isEmpty) List.empty[String]
    else {
      topics.get.split(",").toList

    }
  }

}
