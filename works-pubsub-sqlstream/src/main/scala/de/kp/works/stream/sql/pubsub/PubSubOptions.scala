package de.kp.works.stream.sql.pubsub

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

import com.google.api.client.auth.oauth2.Credential
import de.kp.works.stream.sql.{Logging, RocksPersistence, WorksOptions}
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.rocksdb.RocksDB

import scala.collection.JavaConverters.mapAsScalaMapConverter

class PubSubOptions(options: DataSourceOptions) extends WorksOptions {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  private val defaultAppName = "WORKS_SQLSTREAM_PUBSUB"

  def getAppName:String = {
    settings.getOrElse(PUBSUB_STREAM_SETTINGS.PUBSUB_APP_NAME, defaultAppName)
  }

  def getAutoAck:Boolean = {
    val autoAck = settings.getOrElse(PUBSUB_STREAM_SETTINGS.PUBSUB_AUTO_ACK, "false")
    if (autoAck == "true") true else false
  }

  def getBackoffInit:Int = {
    settings.getOrElse(PUBSUB_STREAM_SETTINGS.PUBSUB_BACKOFF_INIT, "100").toInt
  }

  def getBackoffMax:Int = {
    settings.getOrElse(PUBSUB_STREAM_SETTINGS.PUBSUB_BACKOFF_MAX, "10000").toInt
  }

  def getCredential:Credential = ???

  def getMessageMax:Int = {
    settings.getOrElse(PUBSUB_STREAM_SETTINGS.PUBSUB_MESSAGE_MAX, "1000").toInt
  }

  def getProjectName:String = {

    val projectName = settings.get(PUBSUB_STREAM_SETTINGS.PUBSUB_PROJECT_NAME)
    if (projectName.isEmpty)
      throw new Exception(s"No project name provided.")

    val projectFullName: String = s"projects/$projectName"
    projectFullName

  }

  def getSourcePersistence:RocksDB = {

    val path = settings.getOrElse(PUBSUB_STREAM_SETTINGS.PERSISTENCE, "")
    if (path.isEmpty)
      throw new Exception(s"No persistence path specified.")

    RocksPersistence.getOrCreate(path)

  }

  def getSubscriptionName:String = {

    val projectFullName = getProjectName

    val subscriptionName = settings.get(PUBSUB_STREAM_SETTINGS.PUBSUB_SUBSCRIPTION_NAME)
    if (subscriptionName.isEmpty)
      throw new Exception(s"No subscription name provided.")

    val subscriptionFullName = s"$projectFullName/subscriptions/$subscriptionName"
    subscriptionFullName

  }

  def getTopicName:Option[String] = {

    val projectFullName = getProjectName

    val topicName = settings.get(PUBSUB_STREAM_SETTINGS.PUBSUB_TOPIC_NAME)
    if (topicName.isEmpty) None
    else {
      val topicFullName = s"$projectFullName/topics/$topicName"
      Some(topicFullName)

    }

  }

}

