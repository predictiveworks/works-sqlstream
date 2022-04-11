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

object OpcuaTopicType extends Enumeration {

  val NodeId: OpcuaTopicType.Value = Value("NodeId")
  val Path: OpcuaTopicType.Value = Value("Path")

}

case class OpcuaTopic(
   address:String,
   browsePath:String = "",
   topicName:String,
   topicType: OpcuaTopicType.Value,
   systemName:String) {
  /**
   * The current implementation does not
   * validate the provided OPCUA topic
   */
  def isValid:Boolean = {
    true
  }

}

case class OpcuaTopicValue(
  sourceTime: Long,
  sourcePicoseconds: Int = 0,
  serverTime: Long,
  serverPicoseconds: Int = 0,
  value: Any,
  statusCode: Long)
