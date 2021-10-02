package de.kp.works.stream.sql.sse
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

import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.sources.v2.DataSourceOptions
import org.rocksdb.RocksDB

import scala.collection.JavaConverters.mapAsScalaMapConverter

class SseOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getPersistence:RocksDB = {

    val path = settings.getOrElse(SSE_STREAM_SETTINGS.PERSISTENCE, "")
    if (path.isEmpty)
      throw new Exception(s"No persistence path specified.")

    SsePersistence.getOrCreate(path)

  }

  def getSchemaType:String =
    settings.getOrElse(SSE_STREAM_SETTINGS.SCHEMA_TYPE, "plain")

}
