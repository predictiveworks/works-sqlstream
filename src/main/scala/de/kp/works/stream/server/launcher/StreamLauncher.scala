package de.kp.works.stream.server.launcher
/*
 * Copyright (c) 2019 Dr. Krusche & Partner PartG. All rights reserved.
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
import org.apache.spark.launcher.{SparkAppHandle, SparkLauncher}

import scala.collection.mutable
/**
 * [StreamLauncher] is a wrapper for Apache Spark's [SparkLauncher]
 * to submit streaming applications to a Spark cluster.
 */
class StreamLauncher(options:LaunchOptions) extends Logging {

  private var appName:String = _

  private var sparkLauncher:SparkLauncher = _
  private var sparkListeners:mutable.ArrayBuffer[SparkAppHandle.Listener] = _

  init()

  def addListener(sparkListener:SparkAppHandle.Listener):Unit = {

    if (sparkListeners == null)
      sparkListeners = mutable.ArrayBuffer.empty[SparkAppHandle.Listener]

    sparkListeners += sparkListener

  }

  private def init():Unit = {

    sparkLauncher = new SparkLauncher
    /*
     * Extract the local file path to the Apache Spark
     * installation; ensure that the file path has no
     * trailing slash
     */
    var sparkHome = options.getSparkHome
    if (sparkHome.endsWith("/"))
      sparkHome = sparkHome.substring(0, sparkHome.length - 1)

    sparkLauncher.setSparkHome(sparkHome)
    /*
     * Assign application name
     */
    appName = options.getAppName
    sparkLauncher.setAppName(appName)
    /*
     * The class name of the Spark application that
     * has to be launched
     */
    val appClassName = options.getAppClassName
    sparkLauncher.setMainClass(appClassName)
    /*
     * The path to the *.jar file of the Spark application
     * that has to be launched
     */
    val appJar = options.getAppJar
    sparkLauncher.setAppResource(appJar)
    /*
     * The deploy mode of the Spark application; values are
     * `cluster` and `client`. Default is `client`.
     */
    val deployMode = options.getDeployMode
    sparkLauncher.setDeployMode(deployMode)
    /*
     * Assign Apache Spark specific configurations
     */
    val sparkConf = options.getSparkConf
    sparkConf.foreach{case(k,v) => sparkLauncher.setConf(k,v)}
    /*
     * Assign the Apache Spark master configuration
     */
    val master = options.getSparkMaster
    sparkLauncher.setMaster(master)
    /*
     * Assign extra *.jar files
     */
    val extraJars = options.getExtraJars
    extraJars.foreach(v => sparkLauncher.addJar(v))

  }

  def start(waitForCompletion:Boolean):Unit = {

    if (sparkLauncher == null)
      log.error("The Apache launcher is not initialized.")

    else {

      val handle =
        if (sparkListeners.isEmpty)
          sparkLauncher.startApplication()

        else
          sparkLauncher.startApplication(sparkListeners: _*)

      log.info(s"Started application `$appName` with handle=$handle")

      /*
       * Poll until the Spark application is submitted
       */
      while (handle.getAppId == null) {
        log.info(
          s"Waiting for `$appName`  to be submitted: Status=${handle.getState}")
        Thread.sleep(1500L)
      }

      log.info(s"`$appName` is submitted with ID: `${handle.getAppId}`.")
      if (waitForCompletion) {

        while (!handle.getState.isFinal) {
          log.info(s"${handle.getAppId}: Status=${handle.getState}")
          Thread.sleep(1500L)
        }

        log.info(s"The application finished as ${handle.getState}.")

      } else {
        /*
         * Disconnects the handle from the application, without stopping it.
         * After this method is called, the handle will not be able to communicate
         * with the application anymore.
         */
        handle.disconnect()

      }

    }

  }

}
