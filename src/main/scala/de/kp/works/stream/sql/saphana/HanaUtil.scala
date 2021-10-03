package de.kp.works.stream.sql.saphana
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

import org.apache.spark.sql.execution.datasources.jdbc.DriverRegistry

import java.sql.{Connection, Driver, DriverManager, ResultSet, Statement}
import java.util.Properties
import scala.collection.JavaConverters._
import scala.collection.mutable

object HanaUtil {

  def getDriverClassName(jdbcDriverName:String): String = {
    jdbcDriverName match {
      case "com.sap.db.jdbc.Driver" =>
        classForName(jdbcDriverName).getName
      case _ =>
        classForName(HANA_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME).getName

    }
  }

  def classForName(className: String): Class[_] = {

    val classLoader =
      Option(Thread.currentThread().getContextClassLoader).getOrElse(this.getClass.getClassLoader)

    Class.forName(className, true, classLoader)

  }

  def getConnection(options:HanaOptions): Connection = {

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
        s"Did not find registered SAP HANA driver with class $driverClassName")
    }

    var url = s"jdbc:sap://${options.getDatabaseUrl}"

    /* User authentication */

    val (user, pass) = options.getUserAndPass

    val authProps = new Properties()
    if (user.isDefined && pass.isDefined) {
      /*
       * User authentication parameters are set
       * as URL parameters. For more details:
       *
       * https://help.sap.com/viewer/0eec0d68141541d1b07893a39944924e/2.0.03/en-US/ff15928cf5594d78b841fbbe649f04b4.html
       */
      url = url + "&user=" + user.get + "&password=" + pass.get

    }

    driver.connect(url, authProps)

  }
  /**
   * This method checks whether the provided Redshift
   * table exists and if, returns the column metadata
   * representation to merge with the Spark SQL schema
   */
  def getColumnTypes(conn:Connection, tableName:String):Seq[Int] = {

    var stmt:Statement = null
    val columnTypes = mutable.ArrayBuffer.empty[Int]

    try {

      stmt = conn.createStatement()
      /*
       * Run a query against the database table that returns 0 records,
       * but returns valid ResultSetMetadata that can be used to optimize
       * write requests to the Redshift database
       */
      val rs:ResultSet = stmt.executeQuery(s"SELECT * FROM $tableName WHERE 1 = 0")
      val rsMetadata = rs.getMetaData

      val columnCount = rsMetadata.getColumnCount
      (0 until columnCount).foreach(i =>
        columnTypes += rsMetadata.getColumnType(i + 1)
      )

    } catch {
      case _:Throwable => /* Do nothing */


    } finally {

      if (stmt != null)
        try {
          stmt.close()

        } catch {
          case _:Throwable => /* Do nothing */
        }

    }

    columnTypes

  }

}
