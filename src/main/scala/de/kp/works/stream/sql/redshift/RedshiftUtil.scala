package de.kp.works.stream.sql.redshift

import org.apache.spark.sql.execution.datasources.jdbc.DriverRegistry

import java.sql.{Connection, Driver, DriverManager}
import java.util.Properties
import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter

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

object RedshiftUtil {

  def getDriverClassName(jdbcDriverName:String): String = {
    jdbcDriverName match {
      case "com.amazon.redshift.jdbc42.Driver" =>
        classForName(jdbcDriverName).getName
      case "com.amazon.redshift.jdbc41.Driver" =>
        classForName(jdbcDriverName).getName
      case "com.amazon.redshift.jdbc4.Driver" =>
        classForName(jdbcDriverName).getName
      case _ =>
        classForName(REDSHIFT_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME).getName

    }
  }

  def classForName(className: String): Class[_] = {
    val classLoader =
      Option(Thread.currentThread().getContextClassLoader).getOrElse(this.getClass.getClassLoader)

    Class.forName(className, true, classLoader)
  }

  def getConnection(options:RedshiftOptions): Connection = {

    val driverClassName = getDriverClassName(options.getJdbcDriver)
    DriverRegistry.register(driverClassName)

    val driverWrapperClass: Class[_] =
      classForName("org.apache.spark.sql.execution.datasources.jdbc.DriverWrapper")

    def getWrapped(d: Driver): Driver = {

      require(driverWrapperClass.isAssignableFrom(d.getClass))
      driverWrapperClass.getDeclaredMethod("wrapped").invoke(d).asInstanceOf[Driver]

    }
    /*
     * Note that we purposely don't call #DriverManager.getConnection() here:
     * we want to ensure that an explicitly-specified user-provided driver
     * class can take precedence over the default class, but
     *
     *                 #DriverManager.getConnection()
     *
     * might return a according to a different precedence. At the same time,
     * we don't want to create a driver-per-connection, so we use the DriverManager's
     * driver instances to handle that singleton logic for us.
     */
    val driver: Driver = DriverManager.getDrivers.asScala
      .collectFirst {
        case d if driverWrapperClass.isAssignableFrom(d.getClass)
          && getWrapped(d).getClass.getCanonicalName == driverClassName => d
        case d if d.getClass.getCanonicalName == driverClassName => d
      }.getOrElse {
        throw new IllegalArgumentException(
          s"Did not find registered Redshift driver with class $driverClassName")
      }

    val url = s"jdbc:redshift://${options.getDatabaseUrl}"

    /* User authentication */

    val (user, pass) = options.getUserAndPass

    val authProps = new Properties()
    if (user.isDefined && pass.isDefined) {

      authProps.setProperty("user", user.get)
      authProps.setProperty("password", pass.get)

    }

    driver.connect(url, authProps)

  }

}
