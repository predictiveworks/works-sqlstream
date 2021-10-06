package de.kp.works.stream.sql.mqtt.ngsi
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

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

/**
 * NSGI v2 support intends to transform messages from Works [FiwareBeat],
 * i.e. notifications from a [FIWARE] Context Broker.
 */
object NgsiV2 extends Ngsi {
  /**
   * This method provides the Apache Spark SQL schema that
   * is assigned to NGSI V2 messages
   */
  def schema:StructType = {
    StructType(
      StructField("creationTime",LongType,   nullable = false) ::
      StructField("service",     StringType, nullable = false) ::
      StructField("servicePath", StringType, nullable = false) ::
      StructField("entityId",    StringType, nullable = false) ::
      StructField("entityType",  StringType, nullable = false) ::
      StructField("attrName",    StringType, nullable = false) ::
      StructField("attrType",    StringType, nullable = false) ::
      StructField("attrValue",   StringType, nullable = false) :: Nil
    )
  }

  /**
   * This method transforms a certain NGSI V2 message into
   * a list Apache Spark [Row]s
   */
  def transform(message:String):List[Row] = {

    val deserialized = deserialize(message)

    val service = deserialized("service").asInstanceOf[String]
    val servicePath = deserialized("servicePath").asInstanceOf[String]

    val payload = deserialized("payload")
    payload match {
      case values: List[_] =>
        val head = values.head
        head match {
          case _: Map[_, _] =>

            val creationTime = System.currentTimeMillis

            val entities = payload.asInstanceOf[List[Map[String, _]]]
            entities.flatMap(entity => {

              val entityId = entity("id").asInstanceOf[String]
              val entityType = entity("type").asInstanceOf[String]
              /*
               * Retrieve attributes
               */
              entity
                .filterKeys(x => x != "id" & x!= "type" )
                .map{case(k,v) =>

                  val attrValSpec = v.asInstanceOf[Map[String,Any]]

                  val attrName = k
                  val attrType = attrValSpec.getOrElse("type", "NULL").asInstanceOf[String]

                  val attrValu = attrValSpec.get("value") match {
                    case Some(v) => mapper.writeValueAsString(v)
                    case _ => ""
                  }

                  val seq = Seq(
                    creationTime,
                    service,
                    servicePath,
                    entityId,
                    entityType,
                    attrName,
                    attrType,
                    attrValu)

                  Row.fromSeq(seq)

                }

             })
          case _ =>
            throw new Exception(s"The provided message payload must be a List.")
        }
      case _ =>
        throw new Exception(s"The provided message payload must be a List.")
    }

  }
}
