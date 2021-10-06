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

object LaunchSettings {
  /**
   * The class name of the application that has to be
   * launched on an Apache Spark cluster
   */
  val SPARK_APP_CLASS = "spark.app.class"
  /**
   * The *.jar file of the Spark application
   */
  val SPARK_APP_JAR = "spark.app.jar"
  /**
   * The name of the Apache Spark application
   */
  val SPARK_APP_NAME = "spark.app.name"
  /**
   * The deploy mode determines whether to deploy the
   * Spark application (driver) on the worker nodes
   * (cluster) or locally as an external client (client.
   *
   * Default value is `client`
   */
  val SPARK_DEPLOY_MODE = "spark.deploy.mode"
  /**
   * The list of extract *.jar files that must be
   * deployed
   */
  val SPARK_EXTRA_JARS = "spark.extra.jars"
  /**
   * The (local) file system path to the Apache
   * Spark installation (where `submit` is located).
   */
  val SPARK_HOME = "spark.home"

  val SPARK_MASTER = "spark.master"

}
