package de.kp.works.stream.sql.transform.opencti
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
/**
 * The OpenCTI schema used for Works SQL Stream support
 * is compliant to an NGSI v2 format and was defined to
 * describe different OpenCTI STIX v2 events with a common
 * data representation.
 */
object CTISchema {

  def schema():StructType = {

    val fields = Array(
      StructField("entity_id",   StringType, nullable = false),
      StructField("entity_type", StringType, nullable = false),
      /*
       * Each entity is specified with a list of attributes,
       * and each attribute is described by name, type, value
       * and metadata.
       *
       * For a common representation, all attribute values are
       * represented by its STRING values
       */
      StructField("attr_name",  StringType, nullable = false),
      StructField("attr_type",  StringType, nullable = false),
      StructField("attr_value", StringType, nullable = false),
      StructField("metadata",   StringType, nullable = false)
    )

    StructType(fields)

  }
}
