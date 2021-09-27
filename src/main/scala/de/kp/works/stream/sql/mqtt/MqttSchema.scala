package de.kp.works.stream.sql.mqtt
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

object MqttSchema {

  def getSchema:StructType = {

    val fields:Array[StructField] = Array(
      /*
       * The message identifier
       */
      StructField("id", IntegerType, nullable = false),
      /*
       * The timestamp in milli seconds the message arrived
       */
      StructField("timestamp", LongType, nullable = false),
      /*
       * The MQTT topic of the message
       */
      StructField("topic", StringType, nullable = false),
      /*
       * The quality of service of the message
       */
      StructField("qos", IntegerType, nullable = false),
      /*
       * Indicates whether or not this message might be a
       * duplicate of one which has already been received.
       */
      StructField("duplicate", BooleanType, nullable = false),
      /*
       * Indicates whether or not this message should be/was
       * retained by the server.
       */
      StructField("retained", BooleanType, nullable = false),
      /*
       * The payload of this message
       */
      StructField("payload", BinaryType, nullable = true)
    )

    StructType(fields)

  }
}