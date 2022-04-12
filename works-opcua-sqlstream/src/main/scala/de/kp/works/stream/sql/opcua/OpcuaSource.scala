package de.kp.works.stream.sql.opcua

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

class OpcuaSource(options: OpcuaOptions) extends WorksSource(options) {

  private var receiver:OpcuaReceiver = _
  buildOpcuaReceiver()

  override def readSchema(): StructType =
    OpcuaSchema.getSchema(options.getSchemaType)

  /**
   * This method stops this streaming source
   */
  override def stop(): Unit = synchronized {

    receiver.shutdown().get()
    persistence.close()

  }

  private def toRow(event:OpcuaEvent):Row = {

    val dataValue = event.dataValue
    val (dataValueType, dataValueValue) = {
      dataValue match {
        case value: Double =>
          ("Double", value.asInstanceOf[Double])
        case value: Float =>
          ("Float", value.asInstanceOf[Float])
        case value: Int =>
          ("Int", value.asInstanceOf[Int])
        case value: Long =>
          ("Long", value.asInstanceOf[Long])
        case value: Short =>
          ("Short", value.asInstanceOf[Short])
        case value: String =>
          ("String", value.asInstanceOf[String])
        case _ =>
          ("", "")
      }
    }
    val values = Seq(
      /*
       * TOPIC representation
       */
      event.address,
      event.browsePath,
      event.topicName,
      event.topicType,
      event.systemName:String,
      /*
       * VALUE representation
       */
      event.sourceTime,
      event.sourcePicoseconds,
      event.serverTime,
      event.serverPicoseconds,
      event.statusCode,
      dataValueType,
      dataValueValue)

    Row.fromSeq(values)

  }
  /**
   * Build OPC-UA event receiver and start
   * the subscription listener to the pre-defined
   * topics
   */
  private def buildOpcuaReceiver(): Unit = {

    val opcuaHandler = new OpcuaHandler() {
      /*
       * This method transforms the provided `event`
       * into a Row and registers with the event
       * buffer and store
       */
      override def sendOpcuaEvent(event: Option[OpcuaEvent]): Unit = {

        if (event.isDefined) {

          val row = toRow(event.get)

          val offset = currentOffset.offset + 1L

          events.put(offset, row)
          store.store[Row](offset, row)

          currentOffset = LongOffset(offset)

        }

        log.trace(s"Event arrived, $event")

      }
    }

    receiver = new OpcuaReceiver(options)
    receiver.setOpcuaHandler(opcuaHandler)

    receiver.start()

  }

}
