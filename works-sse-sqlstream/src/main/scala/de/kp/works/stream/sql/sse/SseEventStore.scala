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

import de.kp.works.stream.sql.Logging
import org.rocksdb.RocksDB

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.util.Try

/** An event store for SSE sql streaming. */
trait EventStore {

  /** Store a single id and corresponding serialized event */
  def store[T](id: Long, event: T): Boolean

  /** Retrieve event corresponding to a given id. */
  def retrieve[T](id: Long): T

  /** Highest offset we have stored */
  def maxProcessedOffset: Long

  /** Remove event corresponding to a given id. */
  def remove[T](id: Long): Unit

}

trait Serializer {

  def deserialize[T](x: Array[Byte]): T

  def serialize[T](x: T): Array[Byte]

}

class JavaSerializer extends Serializer with Logging {

  override def deserialize[T](x: Array[Byte]): T = {

    val bis = new ByteArrayInputStream(x)
    val in = new ObjectInputStream(bis)

    val obj = if (in != null) {
      val o = in.readObject()
      Try(in.close()).recover { case t: Throwable => log.warn("failed to close stream", t) }
      o
    } else {
      null
    }

    obj.asInstanceOf[T]

  }

  override def serialize[T](x: T): Array[Byte] = {

    val bos = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(bos)

    out.writeObject(x)
    out.flush()

    if (bos != null) {
      val bytes: Array[Byte] = bos.toByteArray
      Try(bos.close()).recover { case t: Throwable => log.warn("failed to close stream", t) }
      bytes
    } else {
      null
    }

  }
}

object JavaSerializer {

  private lazy val instance = new JavaSerializer()

  def getInstance(): JavaSerializer = instance

}

class SseEventStore(
  val persistentStore: RocksDB,
  val serializer: Serializer) extends EventStore with Logging {

  def this(persistentStore: RocksDB) =
    this(persistentStore, JavaSerializer.getInstance())

  private def get(id: Long) = persistentStore.get(id.toString.getBytes)

  override def maxProcessedOffset: Long = persistentStore.getLatestSequenceNumber

  /** Retrieve event corresponding to a given id. */
  override def retrieve[T](id: Long): T = {
    serializer.deserialize(get(id))
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
