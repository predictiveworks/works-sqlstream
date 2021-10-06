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

import com.google.gson.{JsonElement, JsonParser}
import org.apache.spark.sql.Row

import scala.collection.JavaConversions._

object DittoUtil {
  /**
   * This method transforms a certain [DittoMessage] into
   * a sequence of schema compliant values
   */
  def toRows(message:DittoMessage, schemaType:String):Seq[Row] = {

    schemaType.toLowerCase match {
      case "feature" =>
        fromFeatureValues(message)
      case "features" =>
        fromFeaturesValues(message)
      case "message" =>
        fromMessageValues(message)
      case "plain" =>
        fromPlainValues(message)
      case "thing" =>
        fromThingValues(message)
      case _ =>
        throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }
  /**
   * The value representation of a certain feature change:
   *
   * - id
   * - timestamp
   * - featureId
   * - properties [
   *     {
   *        - name
   *        - type
   *        - value (serialized)
   *     }
   *   ]
   */
  def fromFeatureValues(message:DittoMessage):Seq[Row] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString

    val json = JsonParser.parseString(message.payload)
      .getAsJsonObject

    val timestamp = json.get("timestamp").getAsLong
    val featureId = json.get("id").getAsString

    val properties = json.get("properties")
      .getAsJsonArray
      .map(property2Row)
      .toArray

    val seq = Seq(id, timestamp, featureId, properties)
    val row = Row.fromSeq(seq)

    Seq(row)

  }
  /**
   * A helper method to transform a Ditto
   * property from JSON to Spark SQL Row
   */
  private def property2Row(property:JsonElement):Row = {

    val jsonObject = property.getAsJsonObject

    val propertyName = jsonObject.get("name").getAsString
    val propertyType = jsonObject.get("type").getAsString
    /*
     * Serialized representation of the property value
     */
    val propertyValue = jsonObject.get("value").toString

    val values = Seq(propertyName, propertyType, propertyValue)
    Row.fromSeq(values)

  }
  /**
   * The value representation of a certain feature change:
   *
   * - id
   * - timestamp
   * - features (Array of serialized)
   */
  def fromFeaturesValues(message:DittoMessage):Seq[Row] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString

    val json = JsonParser.parseString(message.payload)
      .getAsJsonObject

    val timestamp = json.get("timestamp").getAsLong
    /*
     * Serialize each feature
     */
    val features  = json.get("features").getAsJsonArray
      .map(_.toString).toArray

    // TODO Explode into multiple rows
    val seq = Seq(id, timestamp, features)
    val row = Row.fromSeq(seq)

    Seq(row)
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
  def fromMessageValues(message:DittoMessage):Seq[Row] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString

    val json = JsonParser.parseString(message.payload)
      .getAsJsonObject

    val timestamp = json.get("timestamp").getAsLong
    val name = json.get("name").getAsString

    val namespace = json.get("namespace").getAsString
    val subject = json.get("subject").getAsString

    val payload = json.get("payload").getAsString
    val seq = Seq(id, timestamp, name, namespace, subject, payload)

    val row = Row.fromSeq(seq)
    Seq(row)

  }
  /**
   * The default and generic value representation of
   * multiple changes and live messages
   *
   * - id
   * - type
   * - payload
   */
  def fromPlainValues(message:DittoMessage):Seq[Row] = {

    val id = "ditto-" + java.util.UUID.randomUUID.toString
    val seq = Seq(id, message.`type`, message.payload)

    val row = Row.fromSeq(seq)
    Seq(row)

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
  def fromThingValues(message:DittoMessage):Seq[Row] = {

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

    val seq = Seq(id, timestamp, name, namespace, features)
    val row = Row.fromSeq(seq)

    Seq(row)

  }

}
