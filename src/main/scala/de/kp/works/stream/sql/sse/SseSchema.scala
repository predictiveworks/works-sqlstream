package de.kp.works.stream.sql.sse
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

object SseSchema {

  def getSchema(schemaType:String):StructType = {

    schemaType.toLowerCase match {
      case "plain" => getPlainSchema
      case _ =>
        throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }
  /**
   * This method builds the default (or plain) schema
   * for the incoming SSE stream. It is independent
   * of the selected semantic source and derived from
   * the field variables provided by the SSE client.
   */
  private def getPlainSchema:StructType = {

    val fields:Array[StructField] = Array(
      StructField("id",   StringType, nullable = false),
      StructField("type", StringType, nullable = false),
      StructField("data", StringType, nullable = true)
    )

    StructType(fields)

  }

}
