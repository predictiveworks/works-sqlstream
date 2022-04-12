package de.kp.works.stream.sql.mqtt.ditto

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

import de.kp.works.stream.sql.RocksPersistence
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.eclipse.ditto.model.things.ThingId
import org.rocksdb.RocksDB

import scala.collection.JavaConverters._

class DittoOptions(options: DataSourceOptions) {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getEndpoint:String = {

    if (settings.contains(DITTO_STREAM_SETTINGS.DITTO_ENDPOINT))
      settings(DITTO_STREAM_SETTINGS.DITTO_ENDPOINT)

    else
      throw new Exception(s"No endpoint path specified.")

  }

  /**
   * Check whether a certain feature identifier is provided to
   * restrict events to a certain feature
   */
  def getFeatureId:String = {

    if (settings.contains(DITTO_STREAM_SETTINGS.DITTO_FEATURE_ID))
      settings(DITTO_STREAM_SETTINGS.DITTO_FEATURE_ID)

    else null

  }

  def getFeatureChangeHandler:String =
    DITTO_STREAM_SETTINGS.DITTO_FEATURE_CHANGES_HANDLER

  def getFeaturesChangeHandler:String =
    DITTO_STREAM_SETTINGS.DITTO_FEATURES_CHANGES_HANDLER

  def getLiveMessagesHandler:String =
    DITTO_STREAM_SETTINGS.DITTO_LIVE_MESSAGES

  def getOAuthClientId:Option[String] =
    settings.get(DITTO_STREAM_SETTINGS.DITTO_OAUTH_CLIENT_ID)

  def getOAuthClientSecret:Option[String] =
    settings.get(DITTO_STREAM_SETTINGS.DITTO_OAUTH_CLIENT_SECRET)

  def getOAuthScopes:Option[String] =
    settings.get(DITTO_STREAM_SETTINGS.DITTO_OAUTH_SCOPES)

  def getOAuthTokenEndpoint:Option[String] =
    settings.get(DITTO_STREAM_SETTINGS.DITTO_OAUTH_TOKEN_ENDPOINT)

  def getPersistence:RocksDB = {

    val path = settings.getOrElse(DITTO_STREAM_SETTINGS.PERSISTENCE, "")
    if (path.isEmpty)
      throw new Exception(s"No persistence path specified.")

    RocksPersistence.getOrCreate(path)

  }

  def getProxy:(Option[String], Option[String]) = {

    val proxyHost =
      settings.get(DITTO_STREAM_SETTINGS.DITTO_PROXY_HOST)

    val proxyPort =
      settings.get(DITTO_STREAM_SETTINGS.DITTO_PROXY_PORT)

    (proxyHost, proxyPort)

  }

  def getSchemaType:String = {

    var schemaType = settings.getOrElse(DITTO_STREAM_SETTINGS.SCHEMA_TYPE, "plain")
    /*
     * Selecting a schema type makes sense
     * if a subscription is made to a single
     * topic or message
     */
    var numSubscriptions = 0
    if (isThingChanges) {
      numSubscriptions += 1
      schemaType = "thing"
    }

    if (isFeaturesChanges) {
      numSubscriptions += 1
      schemaType = "features"
    }

    if (isFeatureChanges) {
      numSubscriptions += 1
      schemaType = "feature"
    }

    if (isLiveMessages) {
      numSubscriptions += 1
      schemaType = "message"
    }

    if (numSubscriptions > 1)
      schemaType = "plain"

    schemaType

  }

  def getTrustStore:(Option[String], Option[String]) = {

    val location =
      settings.get(DITTO_STREAM_SETTINGS.DITTO_TRUSTSTORE_LOCATION)

    val password =
      settings.get(DITTO_STREAM_SETTINGS.DITTO_TRUSTSTORE_PASSWORD)

    (location, password)

  }

  /* User authentication */

  def getUserAndPass:(Option[String], Option[String]) = {

    val username =
      settings.get(DITTO_STREAM_SETTINGS.DITTO_USER)

    val password =
      settings.get(DITTO_STREAM_SETTINGS.DITTO_PASS)

    (username, password)

  }

  def getThingsChangeHandler:String =
    DITTO_STREAM_SETTINGS.DITTO_THING_CHANGES_HANDLER
  /**
   * Check whether a certain thing identifier is provided to
   * restrict events to a certain thing
   */
  def getThingId:ThingId = {

    if (settings.contains(DITTO_STREAM_SETTINGS.DITTO_THING_ID))
      ThingId.of(settings(DITTO_STREAM_SETTINGS.DITTO_THING_ID))

    else null

  }

  def isFeatureChanges:Boolean = {

    val changes = settings.get(DITTO_STREAM_SETTINGS.DITTO_FEATURE_CHANGES)
    if (changes.isEmpty) return false

    if (changes.get == "true") true else false

  }

  def isFeaturesChanges:Boolean = {

    val changes = settings.get(DITTO_STREAM_SETTINGS.DITTO_FEATURES_CHANGES)
    if (changes.isEmpty) return false

    if (changes.get == "true") true else false

  }

  def isLiveMessages:Boolean = {

    val changes = settings.get(DITTO_STREAM_SETTINGS.DITTO_LIVE_MESSAGES)
    if (changes.isEmpty) return false

    if (changes.get == "true") true else false

  }

  def isThingChanges:Boolean = {

    val changes = settings.get(DITTO_STREAM_SETTINGS.DITTO_THING_CHANGES)
    if (changes.isEmpty) return false

    if (changes.get == "true") true else false

  }

}
