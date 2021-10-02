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

import com.google.gson.{JsonObject, JsonParser}
import scala.collection.JavaConversions._

object DittoUtil {
  /**
   * This method transforms a certain [DittoMessage] into
   * a sequence of schema compliant values
   */
  def getValues(message:DittoMessage, schemaType:String):Seq[Any] = {

    schemaType.toLowerCase match {
      case "feature" =>
        getFeatureValues(message)
      case "features" =>
        getFeaturesValues(message)
      case "message" =>
        getMessageValues(message)
      case "plain" =>
        getPlainValues(message)
      case "thing" =>
        getThingValues(message)
      case _ =>
        throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }
  /**
   * The value representation of a certain feature change:
   *
   * - id
   * - timestamp
   * - feature (serialized)
   */
  def getFeatureValues(message:DittoMessage):Seq[Any] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString

    val json = JsonParser.parseString(message.payload)
      .getAsJsonObject

    val timestamp = json.get("timestamp").getAsLong

    val jFeature = new JsonObject
    jFeature.addProperty("id", json.get("id").getAsString)
    jFeature.add("properties", json.get("properties"))

    Seq(id, timestamp, jFeature.toString)

  }
  /**
   * The value representation of a certain feature change:
   *
   * - id
   * - timestamp
   * - features (Array of serialized)
   */
  def getFeaturesValues(message:DittoMessage):Seq[Any] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString

    val json = JsonParser.parseString(message.payload)
      .getAsJsonObject

    val timestamp = json.get("timestamp").getAsLong
    /*
     * Serialize each feature
     */
    val features  = json.get("features").getAsJsonArray
      .map(_.toString).toArray

    Seq(id, timestamp, features)

  }

  /**
   * The value representation of a certain live message:
   *
   * - id
   * - timestamp
   * - name
   * - namespace
   * - subject
   * - payload
   */
  def getMessageValues(message:DittoMessage):Seq[Any] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString

    val json = JsonParser.parseString(message.payload)
      .getAsJsonObject

    val timestamp = json.get("timestamp").getAsLong
    val name = json.get("name").getAsString

    val namespace = json.get("namespace").getAsString
    val subject = json.get("subject").getAsString

    val payload = json.get("payload").getAsString
    Seq(id, timestamp, name, namespace, subject, payload)

  }
  /**
   * The default and generic value representation of
   * multiple changes and live messages
   *
   * - id
   * - type
   * - payload
   */
  def getPlainValues(message:DittoMessage):Seq[Any] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString
    Seq(id, message.`type`, message.payload)

  }
  /**
   * The value representation of a certain thing change:
   *
   * - id
   * - timestamp
   * - name
   * - namespace
   * - features (Array of serialized)
   */
  def getThingValues(message:DittoMessage):Seq[Any] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString

    val json = JsonParser.parseString(message.payload)
      .getAsJsonObject

    val timestamp = json.get("timestamp").getAsLong

    val name = json.get("name").getAsString
    val namespace = json.get("namespace").getAsString

    /*
     * Serialize each feature
     */
    val features  = json.get("features").getAsJsonArray
      .map(_.toString).toArray

    Seq(id, timestamp, name, namespace, features)

  }

}
