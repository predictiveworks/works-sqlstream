package de.kp.works.stream.sql.mqtt.ditto

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

class DittoSource(options: DittoOptions) extends WorksSource(options) {

  private var client:DittoClient = _
  buildDittoClient()

  override def readSchema(): StructType =
    DittoSchema.getSchema(options.getSchemaType)

  override def stop(): Unit = synchronized {
    client.disconnect()
  }

  private def buildDittoClient():Unit = {

    client = DittoClient.build(options)
    val schemaType = options.getSchemaType

    val expose = new DittoExpose() {

      override def messageArrived(message:DittoMessage): Unit =  synchronized {
        /*
         * An incoming Eclipse Ditto message is exploded
         * and represented as a sequence of [Row]s.
         */
        val rows = DittoUtil.toRows(message, schemaType)
        rows.foreach(row => {

          val offset = currentOffset.offset + 1L

          events.put(offset, row)
          store.store[Row](offset, row)

          currentOffset = LongOffset(offset)

        })

        log.trace(s"Message arrived, ${message.`type`} ${message.payload}")

      }

    }

    client.setExpose(expose)
    client.connect()

  }

}
