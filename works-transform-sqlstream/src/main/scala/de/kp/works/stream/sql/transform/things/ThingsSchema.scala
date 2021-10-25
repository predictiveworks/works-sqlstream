package de.kp.works.stream.sql.transform.things

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

object ThingsSchema {
  /**
   * Subscribing to topic v1/gateway/attributes results
   * in messages of the format:
   *
   * Message: {
   *    "device": "Device A",
   *    "data": {
   *        "attribute1": "value1",
   *        "attribute2": 42
   *    }
   * }
   *
   * The [ThingsBeat] transforms this format into an
   * NGSI-compliant representation, which is equivalent
   * to Fiware's format.
   */
  def schema():StructType = {

    val fields = Array(
      StructField("device_id",   StringType, nullable = false),
      StructField("device_type", StringType, nullable = false),
      /*
       * Each device is specified with a list of attributes,
       * and each attribute is described by name, type, value
       * and metadata.
       *
       * For a common representation, all attribute values are
       * represented by its STRING values
       */
      StructField("attr_name",  StringType, nullable = false),
      StructField("attr_type",  StringType, nullable = false),
      StructField("attr_value", StringType, nullable = false)
    )

    StructType(fields)

  }
}
