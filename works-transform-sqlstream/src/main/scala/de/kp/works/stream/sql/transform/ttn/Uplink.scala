package de.kp.works.stream.sql.transform.ttn
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

trait Uplink extends Serializable {

  protected def getDataType(attrVal: Any): String = {
    attrVal match {
      /*
       * Basic data types: these data type descriptions
       * are harmonized with [ValueType]
       */
      case _: BigDecimal => "BigDecimal"
      case _: Boolean => "Boolean"
      case _: Byte => "Byte"
      case _: Double => "Double"
      case _: Float => "Float"
      case _: Int => "Int"
      case _: Long => "Long"
      case _: Short => "Short"
      case _: String => "String"
      /*
       * Datetime support
       */
      case _: java.sql.Date => "java.sql.Date"
      case _: java.sql.Timestamp => "java.sql.Timestamp"
      case _: java.util.Date => "java.util.Date"
      case _: java.time.LocalDate => "java.time.LocalDate"
      case _: java.time.LocalDateTime => "java.time.LocalDateTime"
      case _: java.time.LocalTime => "java.time.LocalTime"
      case _ =>
        val now = new java.util.Date().toString
        throw new Exception(s"[ERROR] $now - Basic data type not supported.")
    }

  }

}
