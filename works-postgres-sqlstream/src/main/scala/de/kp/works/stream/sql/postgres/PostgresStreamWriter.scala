package de.kp.works.stream.sql.postgres

/*
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
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.writer.{DataWriter, DataWriterFactory, WriterCommitMessage}
import org.apache.spark.sql.sources.v2.writer.streaming.StreamWriter
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.StructType

import java.sql.{Connection, PreparedStatement}
import scala.collection.mutable.ArrayBuffer

/**
 * Dummy commit message. The DataSourceV2 framework requires
 * a commit message implementation but we don't need to really
 * send one.
 */
case object PostgresWriterCommitMessage extends WriterCommitMessage

class PostgresStreamWriter(
  options:PostgresOptions,
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
    log.info(s"epoch $epochId of PostgresStreamWriter aborted.")
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
    log.info(s"epoch $epochId of PostgresStreamWriter committed.")
  }

  override def createWriterFactory(): DataWriterFactory[InternalRow] =
    PostgresStreamWriterFactory(options, outputMode, schema)

}

/**
 * A [DataWriterFactory] for Postgres writing. This factory
 * will be serialized and sent to executors to generate the
 * per-task data writers.
 */
case class PostgresStreamWriterFactory(
  options:PostgresOptions,
  outputMode:OutputMode,
  schema: StructType) extends DataWriterFactory[InternalRow] with Logging {

  override def createDataWriter(
    partitionId: Int,
    taskId: Long,
    epochId: Long): DataWriter[InternalRow] = {

    log.info(s"Create date writer for epochId=$epochId, taskId=$taskId, and partitionId=$partitionId.")
    PostgresStreamDataWriter(options, outputMode, schema)

  }

}

/**
 * A [DataWriter] for Postgres writing. A data writer will be created
 * in each partition to process incoming rows.
 */
case class PostgresStreamDataWriter(
  options:PostgresOptions,
  outputMode:OutputMode,
  schema: StructType) extends DataWriter[InternalRow] with Logging {

  /* Use a local cache for batch write to Exasol */

  private val bufferSize = options.getBatchSize
  private val buffer = new ArrayBuffer[Row](bufferSize)

  /* Use for batch writing */

  private var conn: Connection = _
  private var stmt: PreparedStatement = _

  /*
   * Check whether the configured Postgres database
   * table exists and validate whether the provided
   * schema is compliant with the database schema
   */
  validate()

  override def abort(): Unit =
    log.info(s"Abort writing with ${buffer.size} records in local buffer.")

  override def commit(): WriterCommitMessage = {
    doWriteAndClose()
    PostgresWriterCommitMessage
  }

  override def write(record: InternalRow): Unit = {

    buffer.append(Row.fromSeq(record.copy().toSeq(schema)))
    if (buffer.size == bufferSize) {

      log.debug(s"Local buffer is full with size $bufferSize, do write and reset local buffer.")
      doWriteAndResetBuffer()

    }

  }

  /** POSTGRES HELPER METHOD **/

  /**
   * This method makes sure that the Postgres instance
   * contains the specified table and schema
   */
  private def validate(): Unit = {
    // TODO
  }

  /**
   * This method performs a bath write to JDBC
   * and a retry in case of a SQLException
   */
  private def doWriteAndResetBuffer(): Unit = {
    // TODO
  }

  /**
   * This method write remaining buffer entries
   * to the Postgres database and then clears
   * the buffer and closes the JDBC connection
   */
  private def doWriteAndClose():Unit = {

    if (buffer.nonEmpty) {
      doWriteAndResetBuffer()
    }

    if (conn != null) {
      try {
        conn.close()

      } catch {
        case t: Throwable =>
          log.error("Close connection with exception", t)
      }
    }

  }

}

