package de.kp.works.stream.sql.transform.opcua

/*
 * Copyright (c) 2019 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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

object OpcUaSchema {

  def schema():StructType = {

    val fields = Array(
      StructField("address",      StringType, nullable = false),
      StructField("browse_path",  StringType, nullable = false),
      StructField("topic_name",   StringType, nullable = false),
      StructField("topic_type",   StringType, nullable = false),
      StructField("system_name",  StringType, nullable = false),
      StructField("source_time",  LongType, nullable = false),
      /* Source pico seconds */
      StructField("source_picos", IntegerType, nullable = false),
      StructField("server_time",  LongType, nullable = false),
      /* Server pico seconds */
      StructField("server_picos", IntegerType, nullable = false),
      StructField("status_code",  StringType, nullable = false),
      StructField("data_type",    StringType, nullable = false),
      StructField("data_value",   StringType, nullable = false)
    )

    StructType(fields)

  }
}
