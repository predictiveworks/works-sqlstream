package de.kp.works.stream.sql.server.launcher

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

import com.typesafe.config.Config

import scala.collection.JavaConversions._

class LaunchOptions(config:Config) {

  def getAppClassName:String = {
    if (!config.hasPath(LaunchSettings.SPARK_APP_CLASS))
      throw new Exception(s"No main class specified.")

    config.getString(LaunchSettings.SPARK_APP_CLASS)
  }

  def getAppJar:String = {
    if (!config.hasPath(LaunchSettings.SPARK_APP_JAR))
      throw new Exception(s"No application resource specified.")

    config.getString(LaunchSettings.SPARK_APP_JAR)
  }

  def getAppName:String = {
    if (!config.hasPath(LaunchSettings.SPARK_APP_NAME))
      throw new Exception(s"No application name specified.")

    config.getString(LaunchSettings.SPARK_APP_NAME)
  }

  def getDeployMode:String =
    getString(LaunchSettings.SPARK_DEPLOY_MODE, "client")

  def getExtraJars:List[String] = {
    if (!config.hasPath(LaunchSettings.SPARK_EXTRA_JARS))
      return List.empty[String]

    config.getStringList(LaunchSettings.SPARK_EXTRA_JARS)
      .toList
  }

  def getSparkConf:Map[String,String] = {

    val excludes = Seq(
      LaunchSettings.SPARK_APP_CLASS,
      LaunchSettings.SPARK_APP_JAR,
      LaunchSettings.SPARK_APP_NAME,
      LaunchSettings.SPARK_DEPLOY_MODE,
      LaunchSettings.SPARK_EXTRA_JARS,
      LaunchSettings.SPARK_HOME,
      LaunchSettings.SPARK_MASTER)

    config.entrySet()
      .filter(entry => !excludes.contains(entry.getKey))
      .map(entry => {
        val k = entry.getKey
        val v = entry.getValue

        (k, v.toString)
      })
      .toMap

  }

  def getSparkHome:String = {
    if (!config.hasPath(LaunchSettings.SPARK_HOME))
      throw new Exception(s"No Spark home specified.")

    config.getString(LaunchSettings.SPARK_HOME)
  }

  def getSparkMaster:String = {
    if (!config.hasPath(LaunchSettings.SPARK_MASTER))
      throw new Exception(s"No Spark master specified.")

    config.getString(LaunchSettings.SPARK_MASTER)
  }

  private def getString(key:String, default:String):String = {
    try {
      val v = config.getString(key)
      if (v.isEmpty) "client" else v

    }
    catch {
      case _:Throwable => default
    }
  }
}
