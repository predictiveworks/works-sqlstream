package de.kp.works.stream.sql.mqtt.paho
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
import de.kp.works.stream.sql.mqtt.{LocalEventStore, LongOffset, MqttEvent, MqttSchema}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.reader.{InputPartition, InputPartitionReader}
import org.apache.spark.sql.sources.v2.reader.streaming.{MicroBatchReader, Offset}
import org.apache.spark.sql.types.StructType
import org.eclipse.paho.client.mqttv3._

import java.util
import java.util.Optional
import javax.annotation.concurrent.GuardedBy
import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer

class PahoSource(options: PahoOptions)
  extends MicroBatchReader with Logging {

  private var startOffset: Offset = _
  private var endOffset: Offset   = _

  private val events = new TrieMap[Long, MqttEvent]

  private val persistence = options.getPersistence
  private val store = new LocalEventStore(persistence)

  @GuardedBy("this")
  private var currentOffset: LongOffset = LongOffset(-1L)

  @GuardedBy("this")
  private var lastOffsetCommitted: LongOffset = LongOffset(-1L)

  private var client:MqttClient = _
  buildMqttClient()

  override def commit(offset: Offset): Unit = synchronized {

    val newOffset = LongOffset.convert(offset)
    if (newOffset.isEmpty) {

      val message = s"[PahoSource] Method `commit` received an offset (${offset.toString}) that did not originate from this source.)"
      sys.error(message)

    }

    val offsetDiff = (newOffset.get.offset - lastOffsetCommitted.offset).toInt
    if (offsetDiff < 0) {

      val message = s"[PahoSource] Offsets committed are out of order: $lastOffsetCommitted followed by $offset.toString"
      sys.error(message)

    }

    (lastOffsetCommitted.offset until newOffset.get.offset)
      .foreach { x =>
        events.remove(x + 1)
        store.remove(x + 1)
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

    val rawEvents: IndexedSeq[MqttEvent] = synchronized {

      val sliceStart = LongOffset.convert(startOffset).get.offset + 1
      val sliceEnd   = LongOffset.convert(endOffset).get.offset + 1

      for (i <- sliceStart until sliceEnd) yield
        events.getOrElse(i, store.retrieve[MqttEvent](i))
    }

    val spark = SparkSession.getActiveSession.get
    val numPartitions = spark.sparkContext.defaultParallelism
    /*
     * `slices` prepares the partitioned output
     */
    val slices = Array.fill(numPartitions)(new ListBuffer[MqttEvent])

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
               * Schema compliant value representation
               * of an [MqttEvent].
               */
              InternalRow(slice(currentIdx).getValues)
            }

            override def close(): Unit = {/* Do nothing */}

          }
      }

    }.toList.asJava

  }

  override def readSchema(): StructType = MqttSchema.getSchema

  override def setOffsetRange(start: Optional[Offset], end: Optional[Offset]): Unit = synchronized {

    startOffset = start.orElse(LongOffset(-1L))
    endOffset   = end.orElse(currentOffset)

  }

  /**
   * This method stops this streaming source
   */
  override def stop(): Unit = synchronized {

    client.disconnect()
    persistence.close()

    client.close()

  }

  private def buildMqttClient(): Unit = {

    val brokerUrl = options.getBrokerUrl
    val clientId  = options.getClientId

    client = new MqttClient(brokerUrl, clientId, persistence)
    val callback = new MqttCallbackExtended() {

      override def messageArrived(topic_ : String, message: MqttMessage): Unit = synchronized {

        val mqttEvent = new MqttEvent(topic_, message)

        val offset = currentOffset.offset + 1L
        events.put(offset, mqttEvent)

        store.store(offset, mqttEvent)

        currentOffset = LongOffset(offset)
        log.trace(s"Message arrived, $topic_ $mqttEvent")

      }

      override def deliveryComplete(token: IMqttDeliveryToken): Unit = {
        /* Do nothing */
      }

      override def connectionLost(cause: Throwable): Unit = {
        log.warn("Connection to mqtt server lost.", cause)
      }

      override def connectComplete(reconnect: Boolean, serverURI: String): Unit = {
        log.info(s"Connect complete $serverURI. Is it a reconnect?: $reconnect")
      }
    }
    /* Register callback */
    client.setCallback(callback)

    /* Assign Mqtt options */
    val mqttOptions = options.getMqttOptions
    client.connect(mqttOptions)

    /* Subscribe to Mqtt topics */
    val qos    = options.getQos
    val topics = options.getTopics

    val quality = topics.map(_ => qos)
    client.subscribe(topics, quality)

  }
}
