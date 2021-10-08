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

import org.apache.spark.sql.types._

object DittoSchema {
  /*
   * This method determines the schema that is used
   * to represent Eclipse Ditto messages.
   */
  def getSchema(schemaType:String): StructType = {

    schemaType.toLowerCase match {
      case "feature" =>
        getFeatureSchema
      case "features" =>
        getFeaturesSchema
      case "message" =>
        getMessageSchema
      case "plain" =>
        getPlainSchema
      case "thing" =>
        getThingSchema
      case _ =>
        throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }

  private def featureType:StructType = {
    /*
     * This is an embedded and shorted version
     * of the feature schema defined below.
     */
    StructType(
      StructField("featureId",  StringType, nullable = false) ::
      StructField("properties", ArrayType(propertyType, containsNull = false), nullable = true) :: Nil
    )

  }
  private def propertyType:StructType = {
    /*
     * A property representation consists of a
     * property name, data type and serialized
     * value
     */
    StructType(
      StructField("name", StringType, nullable = false) ::
      StructField("type", StringType, nullable = false) ::
      StructField("value", StringType, nullable = true) :: Nil
    )

  }
  private def getFeatureSchema:StructType = {

    StructType(
      StructField("id", StringType, nullable = false) ::
      StructField("timestamp",  LongType, nullable = false) ::
      StructField("featureId",  StringType, nullable = false) ::
      StructField("properties", ArrayType(propertyType, containsNull = false), nullable = true) :: Nil
    )

  }

  private def getFeaturesSchema:StructType = {

    StructType(
      StructField("id", StringType, nullable = false) ::
      StructField("timestamp", LongType, nullable = false) ::
      StructField("features",  ArrayType(featureType, containsNull = false), nullable = true) :: Nil
    )

  }

  private def getMessageSchema:StructType = {

    StructType(
      StructField("id", StringType, nullable = false) ::
      StructField("timestamp", LongType, nullable = false) ::
      StructField("name", StringType, nullable = true) ::
      StructField("namespace", StringType, nullable = true) ::
      StructField("subject", StringType, nullable = true) ::
      StructField("payload", StringType, nullable = true) :: Nil
    )

  }

  /**
   * This method builds the default (or plain) schema
   * for the incoming Ditto message stream.
   */
  private def getPlainSchema: StructType = {

    StructType(
      StructField("id", StringType, nullable = false) ::
      StructField("type", StringType, nullable = false) ::
      StructField("payload", StringType, nullable = true) :: Nil
    )

  }

  private def getThingSchema:StructType = {

    StructType(
      StructField("id", StringType, nullable = false) ::
      StructField("timestamp", LongType, nullable = false) ::
      StructField("name", StringType, nullable = true) ::
      StructField("namespace", StringType, nullable = true) ::
      StructField("features",  ArrayType(featureType, containsNull = false), nullable = true) :: Nil
    )

  }

}
