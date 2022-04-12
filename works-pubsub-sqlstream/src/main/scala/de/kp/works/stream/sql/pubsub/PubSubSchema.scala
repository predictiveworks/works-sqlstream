package de.kp.works.stream.sql.pubsub

import org.apache.spark.sql.types.{ArrayType, ByteType, MapType, StringType, StructField, StructType}

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

object PubSubSchema {
  /*
   * This method determines the schema that is used
   * to represent OPCUA messages.
   */
  def getSchema(schemaType:String): StructType = {

    schemaType.toLowerCase match {
      case "default" =>StructType(Array(
        StructField("id", StringType, nullable = false),
        StructField("publishTime", StringType, nullable = false),
        StructField("attributes", MapType(StringType,StringType), nullable = false),
        StructField("data", ArrayType(ByteType), nullable = false)
      ))
      case _ => throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }

}
