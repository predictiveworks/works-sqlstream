package de.kp.works.sql.akka

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

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor._
import de.kp.works.stream.sql.{Logging, LongOffset}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.reader.streaming.{MicroBatchReader, Offset}
import org.apache.spark.sql.sources.v2.reader.{InputPartition, InputPartitionReader}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Row, SparkSession}

import java.nio.ByteBuffer
import java.util
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.GuardedBy
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

case class SubscribeReceiver(receiverActor: ActorRef)
case class UnsubscribeReceiver(receiverActor: ActorRef)

case class Statistics(
  numberOfMsgs: Int,
  numberOfWorkers: Int,
  numberOfHiccups: Int,
  otherInfo: String)

sealed trait ReceiverRecord
case class SingleRecord(item: String) extends ReceiverRecord

object Ack extends ReceiverRecord

class AkkaSource(options: AkkaOptions)
  extends MicroBatchReader with Logging {

  private var startOffset: Offset = _
  private var endOffset: Offset   = _

  private val messages = new TrieMap[Long, Row]

  private val persistence = options.getPersistence
  private val store = new AkkaEventStore(persistence)

  private val initLock = new CountDownLatch(1)

  @GuardedBy("this")
  private var currentOffset: LongOffset = LongOffset(-1L)

  @GuardedBy("this")
  private var lastOffsetCommitted: LongOffset = LongOffset(-1L)

  private var actorSystem: ActorSystem = _
  private var actorSupervisor: ActorRef = _

  initialize()
  /**
   * Define the Actor receiver
   */
  private class ActorReceiver(publisherUrl:String) extends Actor {

    lazy private val remotePublisher =
      context.actorSelection(publisherUrl)

    override def preStart(): Unit =
      remotePublisher ! SubscribeReceiver(context.self)

    override def postStop(): Unit =
      remotePublisher ! UnsubscribeReceiver(context.self)
    /**
     * The actor receiver supports [String] messages
     * and serializable bytes
     */
    override def receive: PartialFunction[Any, Unit] = {
      case bytes: ByteBuffer => store(new String(bytes.array()))
      case message: String => store(message)
    }

    def store(item: String): Unit = {
      context.parent ! SingleRecord(item)
    }

  }

  private class ActorSupervisor(publisherUrl:String) extends Actor {
    /**
     * Parameters to control the handling of failed child actors:
     * it is the number of retries within a certain time window.
     *
     * The supervisor strategy restarts a child up to 10 restarts
     * per minute. The child actor is stopped if the restart count
     * exceeds maxNrOfRetries during the withinTimeRange duration.
     */
    private val maxRetries: Int = options.getMaxRetries
    protected val timeRange: FiniteDuration = {
      val value = options.getTimeRange
      value.millis
    }

    override val supervisorStrategy: OneForOneStrategy =
      OneForOneStrategy(maxNrOfRetries = maxRetries, withinTimeRange = timeRange) {
        case _: RuntimeException => Restart
        case _: Exception => Escalate
      }

    private val worker = context
      .actorOf(Props(new ActorReceiver(publisherUrl)), "ActorReceiver")

    private val n: AtomicInteger = new AtomicInteger(0)
    private val hiccups: AtomicInteger = new AtomicInteger(0)

    override def receive: PartialFunction[Any, Unit] = {

      case data =>
        initLock.await()
        val temp = currentOffset.offset + 1

        data match {
          case SingleRecord(msg) =>
            messages.put(temp, message2Row(msg))
            n.incrementAndGet()
        }

        currentOffset = LongOffset(temp)

    }

  }

  private def message2Row(message:String):Row = {
    val timestamp = new java.sql.Timestamp(System.currentTimeMillis())
    Row.fromSeq(Seq(message, timestamp))
  }

  override def commit(offset: Offset): Unit = {

    val newOffset = LongOffset.convert(offset)
    if (newOffset.isEmpty) {

      val message = s"[AkkaSource] Method `commit` received an offset (${offset.toString}) that did not originate from this source.)"
      sys.error(message)

    }

    val offsetDiff = (newOffset.get.offset - lastOffsetCommitted.offset).toInt
    if (offsetDiff < 0) {

      val message = s"[AkkaSource] Offsets committed are out of order: $lastOffsetCommitted followed by $offset.toString"
      sys.error(message)

    }

    (lastOffsetCommitted.offset until newOffset.get.offset)
      .foreach { x =>
        messages.remove(x + 1)
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

    val rawMessages: IndexedSeq[Row] = synchronized {

      val sliceStart = LongOffset.convert(startOffset).get.offset + 1
      val sliceEnd   = LongOffset.convert(endOffset).get.offset + 1

      for (i <- sliceStart until sliceEnd) yield {
        val message = messages(i)
        store.store(i,message)

        messages(i)
      }

    }

    val spark = SparkSession.getActiveSession.get
    val numPartitions = spark.sparkContext.defaultParallelism
    /*
     * `slices` prepares the partitioned output
     */
    val slices = Array.fill(numPartitions)(new ListBuffer[Row])
    rawMessages.zipWithIndex
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
              val values = slice(currentIdx).toSeq
              InternalRow(values)
            }

            override def close(): Unit = {/* Do nothing */}
          }
      }

    }.toList.asJava

  }

  override def readSchema(): StructType =
    AkkaSchema.getSchema(options.getSchemaType)

  override def setOffsetRange(start: Optional[Offset], end: Optional[Offset]): Unit = synchronized {

    startOffset = start.orElse(LongOffset(-1L))
    endOffset   = end.orElse(currentOffset)

  }

  override def stop(): Unit = {

    actorSupervisor ! PoisonPill
    persistence.close()

    Await.ready(actorSystem.terminate(), Duration.Inf)

  }

  private def initialize():Unit = {

    actorSystem = options.createActorSystem
    actorSupervisor = actorSystem
      .actorOf(Props(new ActorSupervisor(options.getPublisherUrl)), "ActorSupervisor")

    if (store.maxProcessedOffset > 0) {
      currentOffset = LongOffset(store.maxProcessedOffset)
    }

    initLock.countDown()

  }

}
