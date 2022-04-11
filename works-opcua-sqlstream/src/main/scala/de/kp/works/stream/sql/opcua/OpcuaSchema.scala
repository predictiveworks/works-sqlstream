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

import org.apache.spark.sql.types.{IntegerType, LongType, StringType, StructField, StructType}

object OpcuaSchema {
  /*
   * This method determines the schema that is used
   * to represent OPCUA messages.
   */
  def getSchema(schemaType:String): StructType = {

    schemaType.toLowerCase match {
      case "default" =>
        StructType(Array(
          /*
           * TOPIC representation
           */
          StructField("address",    StringType, nullable = false),
          StructField("browsePath", StringType, nullable = false),
          StructField("topicName",  StringType, nullable = false),
          StructField("topicType",  StringType, nullable = false),
          StructField("systemName", StringType, nullable = false),
          /*
           * VALUE representation
           */
          StructField("sourceTime",        LongType, nullable = false),
          StructField("sourcePicoseconds", IntegerType, nullable = false),
          StructField("serverTime",        LongType, nullable = false),
          StructField("serverPicoseconds", IntegerType, nullable = false),
          StructField("statusCode",        LongType, nullable = false),
          StructField("dataValueType",     StringType, nullable = false),
          StructField("dataValueValue",    StringType, nullable = false)
        ))
      case _ => throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }

}
