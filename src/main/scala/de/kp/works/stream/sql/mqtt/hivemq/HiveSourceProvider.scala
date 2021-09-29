package de.kp.works.stream.sql.mqtt.hivemq
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

import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.sources.DataSourceRegister
import org.apache.spark.sql.sources.v2.reader.streaming.MicroBatchReader
import org.apache.spark.sql.sources.v2.{DataSourceOptions, DataSourceV2, MicroBatchReadSupport}
import org.apache.spark.sql.types.StructType

import java.util.Optional

class HiveSourceProvider extends DataSourceV2
  with MicroBatchReadSupport with DataSourceRegister with Logging {

  override def createMicroBatchReader(
    schema: Optional[StructType],
    checkpointLocation: String,
    options: DataSourceOptions): MicroBatchReader = {

    def e(s: String) = new IllegalArgumentException(s)

    if (schema.isPresent) {
      throw e("The mqtt source does not support a user-specified schema.")
    }
    /*
     * Transform options provided with the respective
     * datasource into a `Hive` specific representation
     */
    val hiveOptions = new HiveOptions(options)
    new HiveSource(hiveOptions)

  }

  override def shortName(): String = "hive"

}
