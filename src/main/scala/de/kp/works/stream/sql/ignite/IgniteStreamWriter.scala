package de.kp.works.stream.sql.ignite
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
import org.apache.ignite.binary.BinaryObject
import org.apache.ignite.cache.QueryEntity
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.{Ignite, IgniteCache, Ignition}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources.v2.writer.{DataWriter, DataWriterFactory, WriterCommitMessage}
import org.apache.spark.sql.sources.v2.writer.streaming.StreamWriter
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types._

import java.security.MessageDigest
import java.util
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
/**
 * Dummy commit message. The DataSourceV2 framework requires
 * a commit message implementation but we don't need to really
 * send one.
 */
case object IgniteWriterCommitMessage extends WriterCommitMessage

class IgniteStreamWriter(
  options:IgniteOptions,
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
    log.info(s"epoch $epochId of IgniteStreamWriter aborted.")
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
    log.info(s"epoch $epochId of IgniteStreamWriter committed.")
  }

  override def createWriterFactory(): DataWriterFactory[InternalRow] =
    IgniteStreamWriterFactory(options, outputMode, schema)

}
/**
 * A [DataWriterFactory] for Apache Ignite writing. This factory will be
 * serialized and sent to executors to generate the per-task data writers.
 */
case class IgniteStreamWriterFactory(
  options:IgniteOptions,
  outputMode:OutputMode,
  schema: StructType) extends DataWriterFactory[InternalRow] with Logging {

  override def createDataWriter(
    partitionId: Int,
    taskId: Long,
    epochId: Long): DataWriter[InternalRow] = {

    log.info(s"Create date writer for epochId=$epochId, taskId=$taskId, and partitionId=$partitionId.")
    IgniteStreamDataWriter(options, outputMode, schema)

  }

}

case class CacheEntry(key:String, value:BinaryObject)

/**
 * A [DataWriter] for Apache Ignite writing. A data writer will be created
 * in each partition to process incoming rows.
 */
case class IgniteStreamDataWriter(
  options:IgniteOptions,
  outputMode:OutputMode,
  schema: StructType) extends DataWriter[InternalRow] with Logging {

  /* Use a local cache for batch write to Apache Ignite */

  private val bufferSize = options.getBatchSize
  private val buffer = new ArrayBuffer[CacheEntry](bufferSize)

  private val maxRetries = options.getIgniteMaxRetries
  /*
   * Retrieve Ignite node instance and check
   * whether the requested cache already exists.
   */
  private val ignite = getOrStartIgnite
  private val cacheName = options.getIgniteCache

  if (outputMode == OutputMode.Complete()) {
    /*
     * Clear the respective Ignite cache and completely
     * fill from the provided streaming records
     */
    deleteCache(cacheName)
  }

  private val cache = getOrCreateCache(cacheName)

  override def abort(): Unit =
    log.info(s"Abort writing with ${buffer.size} records in local buffer.")

  override def commit(): WriterCommitMessage = {
    doWriteAndClose()
    IgniteWriterCommitMessage
  }
  /**
   * An interface method to write an internal row
   * to the Apache Ignite cache. Note, the current
   * implementation leverages a temporary buffer to
   * batch the write request.
   */
  override def write(record: InternalRow): Unit = {

    buffer.append(buildEntry(record))
    if (buffer.size == bufferSize) {

      log.debug(s"Local buffer is full with size $bufferSize, do write and reset local buffer.")
      doWriteAndResetBuffer()

    }

  }

  /** IGNITE HELPER METHOD **/

  private def doWriteAndResetBuffer(): Unit = {

    var retryNum = 0
    while (retryNum < maxRetries) {

      /* Start transaction */
      val tx = ignite.transactions.txStart
      try {

        buffer.foreach(entry => cache.put(entry.key, entry.value))
        tx.commit()

        buffer.clear
        retryNum = maxRetries

      } catch {
        case _:Throwable =>
          tx.rollback()
          retryNum += 1
      }

    }

  }

  private def doWriteAndClose(): Unit = {

    if (buffer.nonEmpty) {
      doWriteAndResetBuffer()
    }

    try {
      ignite.close()

    } catch {
      case e: Throwable => log.error("Close connection with exception", e)
    }

  }

  /**
   * This method transforms an internal row into
   * a public row and then converts this rows to
   * an Apache Ignite cache entry.
   */
  private def buildEntry(record: InternalRow):CacheEntry = {

    val row = Row.fromSeq(record.copy().toSeq(schema))

    val builder = ignite.binary().builder(cacheName)
    var values = Seq.empty[String]

    schema.fields.foreach(field => {

      val fname = field.name
      val ftype = field.dataType

      val fvalue = ftype match {
        /*
         * Primitive data types
         */
        case BooleanType =>
          row.getAs[Boolean](fname)
        case DateType =>
          row.getAs[java.sql.Date](fname)
        case DoubleType =>
          row.getAs[Double](fname)
        case FloatType =>
          row.getAs[Float](fname)
        case IntegerType =>
          row.getAs[Integer](fname)
        case LongType =>
          row.getAs[Long](fname)
        case ShortType =>
          row.getAs[Short](fname)
        case StringType =>
          row.getAs[String](fname)
        case TimestampType =>
          row.getAs[java.sql.Timestamp](fname)
        /*
         * Complex data types: the current implementation
         * supports complex data types in form of their
         * serialized representation
         */
        case ArrayType(BooleanType, true) =>
          row.getAs[mutable.WrappedArray[Boolean]](fname)
            .map(_.toString).mkString(",")
        case ArrayType(DateType, true) =>
          row.getAs[mutable.WrappedArray[java.sql.Date]](fname)
            .map(_.toString).mkString(",")
        case ArrayType(DoubleType, true) =>
          row.getAs[mutable.WrappedArray[Double]](fname)
            .map(_.toString).mkString(",")
        case ArrayType(FloatType, true) =>
          row.getAs[mutable.WrappedArray[Float]](fname)
            .map(_.toString).mkString(",")
        case ArrayType(IntegerType, true) =>
          row.getAs[mutable.WrappedArray[Int]](fname)
            .map(_.toString).mkString(",")
        case ArrayType(LongType, true) =>
          row.getAs[mutable.WrappedArray[Long]](fname)
            .map(_.toString).mkString(",")
        case ArrayType(ShortType, true) =>
          row.getAs[mutable.WrappedArray[Short]](fname)
            .map(_.toString).mkString(",")
        case ArrayType(StringType, true) =>
          row.getAs[mutable.WrappedArray[String]](fname)
            .mkString(",")
        case ArrayType(TimestampType, true) =>
          row.getAs[mutable.WrappedArray[java.sql.Timestamp]](fname)
            .map(_.toString).mkString(",")

        case _ =>
          throw new Exception(s"Data type `${ftype.simpleString}` is not supported.")
      }

      values = values ++ Seq(fvalue.toString)
      builder.setField(fname, fvalue)

    })

    val cacheValue = builder.build()
    /*
     * The cache key is built from the content
     * to enable the detection of duplicates.
     */
    val serialized = values.mkString("#")

    val cacheKey = new String(MessageDigest.getInstance("MD5")
      .digest(serialized.getBytes("UTF-8")))

    CacheEntry(cacheKey, cacheValue)

  }

  /**
   * A helper method to create or get an Apache Ignite node
   * that refers to the provided configuration
   */
  private def getOrStartIgnite:Ignite = {
    Ignition.getOrStart(options.getIgniteCfg)
  }
  /**
   * A helper method to delete an existing Ignite cache
   */
  private def deleteCache(cacheName: String): Unit = {
    val exists: Boolean = cacheExists(cacheName)
    if (exists) {
      ignite.cache(cacheName).destroy()
    }
  }

  private def cacheExists(cacheName: String): Boolean = {
    ignite.cacheNames.contains(cacheName)
  }
  /**
   * A helper method to get or create an Apache Ignite
   * cache that is compliant with provided schema
   */
  private def getOrCreateCache(cacheName: String): IgniteCache[String, BinaryObject] = {

    val exists: Boolean = cacheExists(cacheName)
    val cache = if (exists) {
      ignite.cache[String, BinaryObject](cacheName)
    }
    else {
      createCache(cacheName)
    }

    /*
     * Rebalancing is called here in case of partitioned
     * Apache Ignite caches; the default configuration,
     * however, is to use replicated caches
     */
    cache.rebalance().get()
    cache

  }
  /**
   * This method creates a new Apache Ignite with the provided
   * cache name and the field names and types that respect the
   * schema
   */
  private def createCache(cacheName:String): IgniteCache[String, BinaryObject] = {
    /*
     * Defining query entities is the Apache Ignite
     * mechanism to dynamically define a queryable
     * 'class'
     */
    val qe = buildQueryEntity(cacheName)
    val qes = new util.ArrayList[QueryEntity]
    qes.add(qe)
    /*
     * Specify Apache Ignite cache configuration; it is
     * important to leverage 'BinaryObject' as well as
     * 'setStoreKeepBinary'
     */
    val cfg = new CacheConfiguration[String, BinaryObject]
    cfg.setName(cacheName)

    cfg.setStoreKeepBinary(false)
    cfg.setIndexedTypes(classOf[String], classOf[BinaryObject])

    cfg.setCacheMode(options.getIgniteCacheMode)
    cfg.setQueryEntities(qes)

    ignite.createCache(cfg)

  }
  /**
   * This method supports the creation of a query that
   * is compliant to the provided schema
   */
  private def buildQueryEntity(cacheName: String): QueryEntity = {

    val fields = new util.LinkedHashMap[String, String]
    schema.fields.foreach(field => {

      val fname = field.name
      val ftype = convertDataType(field.dataType)

      fields.put(fname, ftype)

    })

    val qe = new QueryEntity
    /*
     * The key type of the Apache Ignite cache is set to
     * [String], i.e. an independent identity management is
     * used here
     */
    qe.setKeyType("java.lang.String")
    /*
     * The 'cacheName' is used as table name in select statement
     * as well as the name of 'ValueType'
     */
    qe.setValueType(cacheName)
    /*
     * Define fields for the Apache Ignite cache
     */
    qe.setFields(fields)
    qe
  }

  private def convertDataType(dataType:DataType):String = {

    dataType match {
      /*
       * Primitive data types
       */
      case BooleanType =>
        "java.lang.Boolean"
      case DateType =>
        "java.sql.Date"
      case DoubleType =>
        "java.lang.Double"
      case FloatType =>
        "java.lang.Float"
      case IntegerType =>
        "java.lang.Integer"
      case LongType =>
        "java.lang.Long"
      case ShortType =>
        "java.lang.Short"
      case StringType =>
        "java.lang.String"
      case TimestampType =>
        "java.sql.Timestamp"
      /*
       * Complex data types: the current implementation
       * supports complex data types in form of their
       * serialized representation
       */
      case ArrayType(BooleanType, true) =>
        "java.lang.String"
      case ArrayType(DateType, true) =>
        "java.lang.String"
      case ArrayType(DoubleType, true) =>
        "java.lang.String"
      case ArrayType(FloatType, true) =>
        "java.lang.String"
      case ArrayType(IntegerType, true) =>
        "java.lang.String"
      case ArrayType(LongType, true) =>
        "java.lang.String"
      case ArrayType(ShortType, true) =>
        "java.lang.String"
      case ArrayType(StringType, true) =>
        "java.lang.String"
      case ArrayType(TimestampType, true) =>
        "java.lang.String"
      case _ =>
        throw new Exception(s"Data type `${dataType.simpleString}` is not supported.")

    }
  }
}

