package de.kp.works.stream.sql

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

import org.apache.spark.sql.execution.streaming.{Offset, SerializedOffset}
import org.apache.spark.sql.sources.v2.reader.streaming.{Offset => OffsetV2}

/**
 * A simple offset for sources that produce a single linear stream of data.
 */
case class LongOffset(offset: Long) extends OffsetV2 {

  override val json: String = offset.toString

  def +(increment: Long): LongOffset = new LongOffset(offset + increment)
  def -(decrement: Long): LongOffset = new LongOffset(offset - decrement)
}

object LongOffset {

  /**
   * LongOffset factory from serialized offset.
   */
  def apply(offset: SerializedOffset) : LongOffset = new LongOffset(offset.json.toLong)

  /**
   * Convert generic Offset to LongOffset if possible.
   */
  def convert(offset: Offset): Option[LongOffset] = offset match {
    case lo: LongOffset => Some(lo)
    case so: SerializedOffset => Some(LongOffset(so))
    case _ => None
  }
}

