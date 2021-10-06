package de.kp.works.stream.sql.sse

import org.apache.spark.sql.Row

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

object SseUtil {
  /**
   * This method transforms a certain [SseEvent] into
   * a Spark SQL compliant [Row]
   */
  def toRows(event:SseEvent, schemaType:String):Seq[Row] = {

    schemaType.toLowerCase match {
      case "plain" =>
        fromPlainValues(event)
      case _ =>
        throw new Exception(s"Schema type `$schemaType` is not supported.")
    }

  }
  /**
   * The default and generic value representation
   *
   * - id
   * - type
   * - data
   */
  def fromPlainValues(event:SseEvent):Seq[Row] = {

    val seq = Seq(
      event.sseId,
      event.sseType,
      event.sseData)

    val row = Row.fromSeq(seq)
    Seq(row)
  }

}
