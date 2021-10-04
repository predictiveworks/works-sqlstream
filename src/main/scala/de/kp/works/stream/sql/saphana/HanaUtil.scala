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

import de.kp.works.stream.sql.jdbc.JdbcUtil

import java.sql.Connection
import java.util.Properties

object HanaUtil extends JdbcUtil {

  override def getDriverClassName(jdbcDriverName:String): String = {
    jdbcDriverName match {
      case "com.sap.db.jdbc.Driver" =>
        classForName(jdbcDriverName).getName
      case _ =>
        classForName(HANA_STREAM_SETTINGS.DEFAULT_JDBC_DRIVER_NAME).getName

    }
  }

  def getConnection(options:HanaOptions): Connection = {

    val driver = getDriver(options.getJdbcDriver)
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

}
