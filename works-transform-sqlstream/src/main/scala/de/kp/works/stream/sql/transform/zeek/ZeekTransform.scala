package de.kp.works.stream.sql.transform.zeek
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

import com.google.gson.JsonElement
import de.kp.works.stream.sql.transform.{BaseTransform, Beats}
import org.apache.spark.sql.Row

object ZeekTransform extends BaseTransform {

  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {
    /*
     * Validate whether the provided event type
     * refers to the support format for Zeek log
     * files:
     *            beat/zeek/<entity>.log
     */
    val tokens = eventType.split("\\/")
    if (tokens.size != 3)
      throw new Exception("Unknown format for event type detected.")

    if (tokens(0) != Beats.ZEEK.toString)
      throw new Exception("The event type provided does not describe a Zeek event.")

    if (!tokens(2).endsWith(".log"))
      throw new Exception("The event type provided does not describe a Zeek log file.")
    /*
     * Extract log file name and determine schema
     * that refers to log file name
     */
    val file = tokens(1) + ".log"

    val format = ZeekFormatUtil.fromFile(file)
    if (format == null) return null

    ???
  }

}
