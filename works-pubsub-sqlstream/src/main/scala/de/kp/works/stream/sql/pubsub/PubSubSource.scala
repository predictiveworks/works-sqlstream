package de.kp.works.stream.sql.pubsub

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

import de.kp.works.stream.sql.{LongOffset, WorksSource}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

class PubSubSource(options: PubSubOptions) extends WorksSource(options) {

  private var receiver:PubSubReceiver = _
  buildPubSubReceiver()

  override def readSchema(): StructType =
    PubSubSchema.getSchema(options.getSchemaType)

  override def stop(): Unit = synchronized {
    receiver.stop()
  }

  private def toRow(event:PubSubEvent):Row = {

    val values = Seq(
      event.id,
      event.publishTime,
      event.attributes,
      event.data)

    Row.fromSeq(values)

  }

  private def buildPubSubReceiver():Unit = {

    val pubSubHandler = new PubSubHandler() {
      /*
       * This method transforms the provided `event`
       * into a Row and registers with the event
       * buffer and store
       */
      override def sendEvents(pubSubEvents: Seq[PubSubEvent]): Unit = {

        if (pubSubEvents.nonEmpty) {
          pubSubEvents.foreach(event => {

            val row = toRow(event)
            val offset = currentOffset.offset + 1L

            events.put(offset, row)
            store.store[Row](offset, row)

            currentOffset = LongOffset(offset)

          })

        }

        log.trace(s"Event arrived, $pubSubEvents")

      }
    }

    receiver = new PubSubReceiver(options, pubSubHandler)
    receiver.start()

  }

}
