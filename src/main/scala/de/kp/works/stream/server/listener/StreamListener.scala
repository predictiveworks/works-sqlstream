package de.kp.works.stream.server.listener
/*
 * Copyright (c) 2019 Dr. Krusche & Partner PartG. All rights reserved.
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

import akka.actor.ActorRef
import org.apache.spark.sql.streaming.StreamingQueryListener
/**
 * The [StreamListener] monitors running streaming (query)
 * applications and sends real-time metrics to an accompanied
 * Akka actor
 */
class StreamListener(actor:ActorRef) extends StreamingQueryListener {

  import StreamListenerActor._

  /**
   * This method informs that the DataStreamWriter was requested
   * to start execution of the streaming query (on the stream
   * execution thread)
   */
  override def onQueryStarted(event: StreamingQueryListener.QueryStartedEvent): Unit = {
    /* Delegate event to the accompanied actor */
    actor ! Started(event)
  }
  /**
   * This method informs that MicroBatchExecution has finished
   * triggerExecution phase (the end of a streaming batch)
   */
  override def onQueryProgress(event: StreamingQueryListener.QueryProgressEvent): Unit = {
    /* Delegate event to the accompanied actor */
    actor ! Progress(event)
  }
  /**
   * Informs that a streaming query was stopped or terminated
   * due to an error
   */
  override def onQueryTerminated(event: StreamingQueryListener.QueryTerminatedEvent): Unit = {
    /* Delegate event to the accompanied actor */
    actor ! Terminated(event)
  }

}
