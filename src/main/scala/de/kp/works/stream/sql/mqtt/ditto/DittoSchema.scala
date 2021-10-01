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

import org.apache.spark.sql.types.{BinaryType, BooleanType, IntegerType, LongType, StringType, StructField, StructType}

object DittoSchema {

  def getSchema(schemaType: String): StructType = {

    schemaType.toLowerCase match {
      case "plain" => getPlainSchema
      case _ =>
        throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }

  /**
   * This method builds the default (or plain) schema
   * for the incoming Ditto message stream.
   */
  private def getPlainSchema: StructType = {

    val fields:Array[StructField] = Array(
      /*
       * The message identifier
       */
      StructField("id", StringType, nullable = false),
      /*
       * The Ditto message type
       */
      StructField("type", StringType, nullable = false),
      /*
       * The payload of this message
       */
      StructField("payload", StringType, nullable = true)
    )

    StructType(fields)

  }

}
