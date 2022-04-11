package de.kp.works.stream.sql.mqtt.paho

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

import de.kp.works.stream.sql.Logging
import de.kp.works.stream.sql.mqtt.MqttWriterCommitMessage
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.writer.{DataWriter, DataWriterFactory, WriterCommitMessage}
import org.apache.spark.sql.sources.v2.writer.streaming.StreamWriter
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.StructType
import org.eclipse.paho.client.mqttv3.{MqttClient, MqttMessage}

class PahoStreamWriter(
  options:PahoOptions,
  outputMode:OutputMode,
  schema:StructType) extends StreamWriter with Logging {
  /**
   * Aborts this writing job because some data writers are failed and keep failing when retried, or
   * the Spark job fails with some unknown reasons, or #commit(WriterCommitMessage[]) fails.
   *
   * If this method fails (by throwing an exception), the underlying data source may require manual
   * cleanup.
   *
   * Unless the abort is triggered by the failure of commit, the given messages will have some
   * null slots, as there may be only a few data writers that were committed before the abort
   * happens, or some data writers were committed but their commit messages haven't reached the
   * driver when the abort is triggered. So this is just a "best effort" for data sources to
   * clean up the data left by data writers.
   */
  override def abort(epochId: Long, writerCommitMessages: Array[WriterCommitMessage]): Unit = {
    log.info(s"epoch $epochId of PahoStreamWriter aborted.")
  }
  /**
   * Commits this writing job for the specified epoch with a list of commit messages. The commit
   * messages are collected from successful data writers and are produced by DataWriter#commit().
   *
   * If this method fails (by throwing an exception), this writing job is considered to have been
   * failed, and the execution engine will attempt to call #abort(WriterCommitMessage[]).
   *
   * The execution engine may call commit() multiple times for the same epoch in some circumstances.
   * To support exactly-once data semantics, implementations must ensure that multiple commits for
   * the same epoch are idempotent.
   */
  override def commit(epochId: Long, writerCommitMessages: Array[WriterCommitMessage]): Unit = {
    log.info(s"epoch $epochId of PahoStreamWriter committed.")
  }

  override def createWriterFactory(): DataWriterFactory[InternalRow] =
    PahoStreamWriterFactory(options, outputMode, schema)

}

/**
 * A [DataWriterFactory] for Paho writing. This factory
 * will be serialized and sent to executors to generate the
 * per-task data writers.
 */
case class PahoStreamWriterFactory(
  options:PahoOptions,
  outputMode:OutputMode,
  schema: StructType) extends DataWriterFactory[InternalRow] with Logging {

  override def createDataWriter(
    partitionId: Int,
    taskId: Long,
    epochId: Long): DataWriter[InternalRow] = {

    log.info(s"Create date writer for epochId=$epochId, taskId=$taskId, and partitionId=$partitionId.")
    PahoStreamDataWriter(options, outputMode, schema)

  }

}

/**
 * A [DataWriter] for Paho writing. A data writer will be created
 * in each partition to process incoming rows.
 */
case class PahoStreamDataWriter(
  options:PahoOptions,
  outputMode:OutputMode,
  schema: StructType) extends DataWriter[InternalRow] with Logging {

  private val mqttClient = buildMqttClient

  override def abort(): Unit = {
    log.info(s"Abort writing.")
    if (mqttClient.nonEmpty) mqttClient.get.disconnect()
  }

  override def commit(): WriterCommitMessage = MqttWriterCommitMessage

  override def write(row: InternalRow): Unit = {

    val mqttMessage = new MqttMessage(row.getBinary(0))
    mqttMessage.setQos(options.getQos)

    val mqttTopic = options.getTopics.head
    if (mqttClient.nonEmpty) {
      try {
        mqttClient.get.publish(mqttTopic, mqttMessage)

      } catch {
        case _:Throwable =>
          Thread.sleep(options.getRetryWait)
          mqttClient.get.publish(mqttTopic, mqttMessage)
      }
    }

  }

  private def buildMqttClient:Option[MqttClient] = {

    val brokerUrl = options.getBrokerUrl
    val clientId  = options.getClientId

    val persistence = options.getSinkPersistence
    val client = new MqttClient(brokerUrl, clientId, persistence)

    /* Assign Mqtt options */
    val mqttOptions = options.getMqttOptions
    client.connect(mqttOptions)

    if (client.isConnected) {
      log.info(s"[PahoStreamDataWriter] Mqtt client connected.")
      Some(client)

    }
    else {
      log.warn(s"[PahoStreamDataWriter] Mqtt client not connected.")
      None

    }

  }
}
