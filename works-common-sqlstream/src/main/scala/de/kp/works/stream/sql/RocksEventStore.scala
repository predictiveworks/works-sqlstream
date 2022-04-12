package de.kp.works.stream.sql

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

import org.rocksdb.RocksDB

class RocksEventStore(
  val persistentStore: RocksDB,
  val serializer: Serializer) extends EventStore with Logging {

  def this(persistentStore: RocksDB) =
    this(persistentStore, JavaSerializer.getInstance())

  private def get(id: Long) = persistentStore.get(id.toString.getBytes)

  override def maxProcessedOffset: Long = persistentStore.getLatestSequenceNumber

  /** Retrieve event corresponding to a given id. */
  override def retrieve[T](id: Long): T = {
    serializer.deserialize[T](get(id))
  }

  override def store[T](id: Long, message: T): Boolean = {

    val bytes: Array[Byte] = serializer.serialize(message)
    try {

      persistentStore.put(id.toString.getBytes(), bytes)
      true

    } catch {
      case e: Exception => log.warn(s"Failed to store message Id: $id", e)
        false
    }
  }

  override def remove[T](id: Long):Unit = {
    persistentStore.delete(id.toString.getBytes)
  }

}