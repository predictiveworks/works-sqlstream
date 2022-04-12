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
import de.kp.works.stream.sql.{LongOffset, WorksSource}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
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

class AkkaSource(options: AkkaOptions) extends WorksSource(options) {

  private val initLock = new CountDownLatch(1)

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
            events.put(temp, message2Row(msg))
            n.incrementAndGet()
        }

        currentOffset = LongOffset(temp)

    }

  }

  private def message2Row(message:String):Row = {
    val timestamp = new java.sql.Timestamp(System.currentTimeMillis())
    Row.fromSeq(Seq(message, timestamp))
  }

  override def readSchema(): StructType =
    AkkaSchema.getSchema(options.getSchemaType)

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
