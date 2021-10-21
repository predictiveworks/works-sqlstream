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
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.{DataRegionConfiguration, DataStorageConfiguration, IgniteConfiguration}
import org.apache.ignite.logger.java.JavaLogger
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder
import org.apache.spark.sql.sources.v2.DataSourceOptions

import scala.collection.JavaConverters._

class IgniteOptions(options: DataSourceOptions) extends Logging {

  private val settings:Map[String,String] = options.asMap.asScala.toMap

  def getBatchSize:Int =
    settings.getOrElse(IGNITE_STREAM_SETTINGS.BATCH_SIZE, "1000").toInt

  def getIgniteCache:String = {

    if (!settings.contains(IGNITE_STREAM_SETTINGS.IGNITE_CACHE)) {
      throw new Exception(s"No Ignite cache name configured.")

    } else
      settings(IGNITE_STREAM_SETTINGS.IGNITE_CACHE)

  }

  def getIgniteCacheMode:CacheMode = {

    val cacheMode = settings.getOrElse(IGNITE_STREAM_SETTINGS.IGNITE_CACHE_MODE, "partitioned")
    cacheMode match {
      case "local" =>
        CacheMode.LOCAL
      case "partitioned" =>
        CacheMode.PARTITIONED
      case "replicated" =>
        CacheMode.REPLICATED
      case _ =>
        throw new Exception(s"Ignite cache mode `$cacheMode` is not supported.")
    }
  }

  def getIgniteCfg:IgniteConfiguration = {

    val igniteCfg = new IgniteConfiguration
    /*
     * Configure default java logger which leverages
     * file config/java.util.logging.properties
     */
    val logger = new JavaLogger()
    igniteCfg.setGridLogger(logger)
    /*
     * Configuring the data storage
     */
    val dataStorageCfg = new DataStorageConfiguration
    /*
     * Default memory region that grows endlessly. Any cache will
     * be bound to this memory region unless another region is set
     * in the cache's configuration.
     */
    val dataRegionCfg = new DataRegionConfiguration

    val region = settings.getOrElse(IGNITE_STREAM_SETTINGS.IGNITE_MEMORY_REGION, "Default_Region")
    dataRegionCfg.setName(region)
    /*
     * Initial or minimum memory size
     */
    val minimumSize = settings
      .getOrElse(IGNITE_STREAM_SETTINGS.IGNITE_MEMORY_MINIMUM, "100MB")

    dataRegionCfg.setInitialSize(getMemorySize(minimumSize))
    /*
     * Maximum memory size
     */
    val maximumSize = settings
      .getOrElse(IGNITE_STREAM_SETTINGS.IGNITE_MEMORY_MAXIMUM, "2GB")

    dataStorageCfg.setSystemRegionMaxSize(getMemorySize(maximumSize))

    igniteCfg.setDataStorageConfiguration(dataStorageCfg)
    /*
     * Explicitly configure TCP discovery SPI to provide
     * list of initial nodes.
     */
    val discoverySpi = new TcpDiscoverySpi
    /*
     * Ignite provides several options for automatic discovery
     * that can be used instead os static IP based discovery.
     */
    val ipFinder = new TcpDiscoveryMulticastIpFinder

    val addresses = settings.getOrElse(IGNITE_STREAM_SETTINGS.IGNITE_ADDRESSES, "")
    if (addresses.isEmpty)
      throw new Exception(s"No Ignite IP addresses configured.")

    ipFinder.setAddresses(java.util.Arrays.asList(addresses))

    discoverySpi.setIpFinder(ipFinder)
    igniteCfg.setDiscoverySpi(discoverySpi)

    igniteCfg

  }

  def getMaxRetries:Int =
    settings.getOrElse(IGNITE_STREAM_SETTINGS.IGNITE_MAX_RETRIES, "3").toInt

  private def getMemorySize(text:String):Long = {

    val upper = text.toUpperCase
    if (upper.contains("MB")) {

      val number = upper.replace("MB", "").trim.toInt
      (number * 1024 * 1024).toLong

    }
    else if (upper.contains("GB")) {

      val number = upper.replace("GB", "").trim.toInt
      (number * 1024 * 1024 * 1024).toLong

    }
    else
      throw new Exception(s"Memory size must be specified in MB or GB")

  }
}
