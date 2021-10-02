package de.kp.works.stream.sql.ignite
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

object IGNITE_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.ignite.IgniteSinkProvider"
  /**
   * The maximum batch size of the internal cache
   * before writing to Apache Ignite
   */
  val BATCH_SIZE = "batch.size"

  val IGNITE_ADDRESSES = "ignite.addresses"
  val IGNITE_CACHE     = "ignite.cache"
  /**
   * The values are `partitioned` or `replicated`.
   * Default is `partitioned`.
   */
  val IGNITE_CACHE_MODE = "ignite.cache.mode"
  /**
   * The maximum number of retries to write
   * to an Apache Ignite cache
   */
  val IGNITE_MAX_RETRIES = "ignite.max.retries"

  val IGNITE_MEMORY_REGION = "ignite.memory.region"

  val IGNITE_MEMORY_MINIMUM = "ignite.memory.minimum"
  val IGNITE_MEMORY_MAXIMUM = "ignite.memory.maximum"

}
