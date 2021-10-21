package de.kp.works.stream.sql.server.launcher

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
