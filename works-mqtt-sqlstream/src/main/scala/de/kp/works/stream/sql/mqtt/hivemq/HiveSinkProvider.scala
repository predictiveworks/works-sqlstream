package de.kp.works.stream.sql.mqtt.hivemq

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

import de.kp.works.stream.sql.mqtt.MqttRelation
import org.apache.spark.sql.sources.v2.writer.streaming.StreamWriter
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}
import org.apache.spark.sql.sources.{BaseRelation, CreatableRelationProvider, DataSourceRegister}
import org.apache.spark.sql.sources.v2.{DataSourceOptions, DataSourceV2, StreamWriteSupport}
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.StructType

class HiveSinkProvider extends DataSourceV2 with StreamWriteSupport
  with DataSourceRegister with CreatableRelationProvider {

  override def createRelation(
  sqlContext: SQLContext,
  mode: SaveMode,
  parameters: Map[String, String],
  data: DataFrame): BaseRelation = MqttRelation(sqlContext, data)

  override def createStreamWriter(
    queryId: String,
    schema: StructType,
    outputMode: OutputMode,
    options: DataSourceOptions): StreamWriter = {
    /*
     * Transform options provided with the respective
     * datasource into a `HiveMQ` specific representation
     */
    val pahoOptions = new HiveOptions(options)
    new HiveStreamWriter(pahoOptions, outputMode, schema)
  }

  override def shortName(): String = "hiveSink"

}
