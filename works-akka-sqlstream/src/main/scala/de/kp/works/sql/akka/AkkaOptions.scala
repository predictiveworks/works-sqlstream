package de.kp.works.sql.akka

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

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import de.kp.works.stream.sql.{Logging, RocksPersistence, WorksOptions}
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.rocksdb.RocksDB

import scala.collection.JavaConverters._

class AkkaOptions(options: DataSourceOptions) extends WorksOptions with Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def createActorSystem: ActorSystem = {

    val systemName = s"works-sqlstream-system"
    val systemConf = ConfigFactory.parseString(
      s"""akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
         |akka.remote.netty.tcp.port = "0"
         |akka.loggers.0 = "akka.event.slf4j.Slf4jLogger"
         |akka.log-dead-letters-during-shutdown = "off"
       """.stripMargin)

    ActorSystem(systemName, systemConf)

  }

  def getMaxRetries:Int = {
      settings.getOrElse(AKKA_STREAM_SETTINGS.AKKA_MAX_RETRIES, "10").toInt
  }

  def getSourcePersistence:RocksDB = {

    val path = settings.getOrElse(AKKA_STREAM_SETTINGS.PERSISTENCE, "")
    if (path.isEmpty)
      throw new Exception(s"No persistence path specified.")

    RocksPersistence.getOrCreate(path)

  }

  def getPublisherUrl:String = {

    val url = settings.getOrElse(AKKA_STREAM_SETTINGS.AKKA_PUBLISHER_URL, "")
    if (url.isEmpty)
      throw new Exception(s"No publisher url specified.")

    url

  }

  def getSchemaType:String = {

    val schemaType = settings.getOrElse(AKKA_STREAM_SETTINGS.SCHEMA_TYPE, "default")
    if (schemaType == "default") schemaType
    else
      throw new Exception(s"Schema type `$schemaType` not supported.")

  }

  def getTimeRange:Int = {
    settings.getOrElse(AKKA_STREAM_SETTINGS.AKKA_TIME_RANGE, "10").toInt
  }

}
