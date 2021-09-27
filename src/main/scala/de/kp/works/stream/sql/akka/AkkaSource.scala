package de.kp.works.stream.sql.akka
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

import de.kp.works.stream.sql.Logging
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.reader.InputPartition
import org.apache.spark.sql.sources.v2.reader.streaming.{MicroBatchReader, Offset}
import org.apache.spark.sql.types.StructType

import java.util
import java.util.Optional

class AkkaSource(options: AkkaOptions)
  extends MicroBatchReader with Logging {

  override def commit(offset: Offset): Unit = ???

  override def deserializeOffset(s: String): Offset = ???

  override def getStartOffset: Offset = ???

  override def getEndOffset: Offset = ???

  override def planInputPartitions(): util.List[InputPartition[InternalRow]] = ???

  override def readSchema(): StructType = ???

  override def setOffsetRange(optional: Optional[Offset], optional1: Optional[Offset]): Unit = ???

  override def stop(): Unit = ???

}
