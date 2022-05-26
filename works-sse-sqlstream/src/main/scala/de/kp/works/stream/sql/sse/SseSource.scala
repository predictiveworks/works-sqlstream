package de.kp.works.stream.sql.sse

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

import de.kp.works.stream.sql.{Logging, LongOffset}
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.reader.{InputPartition, InputPartitionReader}
import org.apache.spark.sql.sources.v2.reader.streaming.{MicroBatchReader, Offset}
import org.apache.spark.sql.types.StructType

import java.util
import java.util.Optional
import javax.annotation.concurrent.GuardedBy
import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
/**
 * [SseSource] is built to support Works & Sensor
 * Beats as streaming sources for Apache Spark SQL
 * or Structured Streaming
 */
class SseSource(options: SseOptions)
  extends MicroBatchReader with Logging {

  private var startOffset: Offset = _
  private var endOffset: Offset   = _

  private val events = new TrieMap[Long, Row]

  private val persistence = options.getPersistence
  private val store = new SseEventStore(persistence)

  @GuardedBy("this")
  private var currentOffset: LongOffset = LongOffset(-1L)

  @GuardedBy("this")
  private var lastOffsetCommitted: LongOffset = LongOffset(-1L)

  private var client:SseClient = _
  buildSseClient()

  override def commit(offset: Offset): Unit = synchronized {

    val newOffset = LongOffset.convert(offset)
    if (newOffset.isEmpty) {

      val message = s"[SseSource] Method `commit` received an offset (${offset.toString}) that did not originate from this source.)"
      sys.error(message)

    }

    val offsetDiff = (newOffset.get.offset - lastOffsetCommitted.offset).toInt
    if (offsetDiff < 0) {

      val message = s"[SseSource] Offsets committed are out of order: $lastOffsetCommitted followed by $offset.toString"
      sys.error(message)

    }

    (lastOffsetCommitted.offset until newOffset.get.offset)
      .foreach { x =>
        events.remove(x + 1)
        store.remove[Row](x + 1)
      }

    lastOffsetCommitted = newOffset.get

  }

  override def deserializeOffset(json: String): Offset = {
    LongOffset(json.toLong)
  }

  override def getStartOffset: Offset =
    Option(startOffset)
      .getOrElse(throw new IllegalStateException("Start offset is not set."))

  override def getEndOffset: Offset =
    Option(endOffset)
      .getOrElse(throw new IllegalStateException("End offset is not set."))

  override def planInputPartitions(): util.List[InputPartition[InternalRow]] = {

    val rawEvents: IndexedSeq[Row] = synchronized {

      val sliceStart = LongOffset.convert(startOffset).get.offset + 1
      val sliceEnd   = LongOffset.convert(endOffset).get.offset + 1

      for (i <- sliceStart until sliceEnd) yield
        events.getOrElse(i, store.retrieve[Row](i))
    }

    val spark = SparkSession.getActiveSession.get
    val numPartitions = spark.sparkContext.defaultParallelism
    /*
     * `slices` prepares the partitioned output
     */
    val slices = Array.fill(numPartitions)(new ListBuffer[Row])

    rawEvents.zipWithIndex
      .foreach {case (rawEvent, index) => slices(index % numPartitions).append(rawEvent)}
    /*
     * Transform `slices` into [DataFrame] compliant [InternalRow]s.
     * Note, the order of values must be compliant to the defined
     * schema
     */
    (0 until numPartitions).map{i =>

      val slice = slices(i)
      new InputPartition[InternalRow] {

        override def createPartitionReader(): InputPartitionReader[InternalRow] =
          new InputPartitionReader[InternalRow] {
            private var currentIdx = -1

            override def next(): Boolean = {
              currentIdx += 1
              currentIdx < slice.size
            }

            override def get(): InternalRow = {
              /*
               * [SseUtil] transforms the [SseEvent] into
               * a schema compliant sequence of values
               */
              val values = slice(currentIdx).toSeq
              InternalRow(values)
            }

            override def close(): Unit = {/* Do nothing */}

          }
      }

    }.toList.asJava

  }

  override def readSchema(): StructType =
    SseSchema.getSchema(options.getSchemaType)

  override def setOffsetRange(start: Optional[Offset], end: Optional[Offset]): Unit = synchronized {

    startOffset = start.orElse(LongOffset(-1L))
    endOffset   = end.orElse(currentOffset)

  }

  override def stop(): Unit = synchronized {
    client.disconnect()
  }

  private def buildSseClient():Unit = {

    client = SseClient.build(options)
    val schemaType = options.getSchemaType

    val expose = new SseExpose() {

      override def eventArrived(event:SseEvent): Unit =  synchronized {
        /*
         * This method transforms an incoming SseEvent into
         * a schema compliant row
         */
        val rows = SseUtil.toRows(event, schemaType)

        if (rows.isDefined) {
          rows.get.foreach(row => {

            val offset = currentOffset.offset + 1L

            events.put(offset, row)
            store.store[Row](offset, row)

            currentOffset = LongOffset(offset)

          })

        }

        log.trace(s"Event arrived, ${event.sseType} ${event.sseData}")

      }

    }

    client.setExpose(expose)
    client.connect()

  }

}
