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

object SseUtil {
  /**
   * This method transforms a certain [SseEvent] into
   * a sequence of schema compliant values
   */
  def getValues(event:SseEvent, schemaType:String):Seq[Any] = {

    schemaType.toLowerCase match {
      case "plain" =>
        getPlainValues(event)
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
  def getPlainValues(event:SseEvent):Seq[Any] = {
    Seq(
      event.sseId,
      event.sseType,
      event.sseData)

  }

}
