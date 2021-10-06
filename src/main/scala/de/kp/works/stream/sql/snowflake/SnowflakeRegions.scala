package de.kp.works.stream.sql.snowflake
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

object SnowflakeRegions extends Enumeration {

  type SnowflakeRegion = Value
    /* AWS */
    val US_WEST_2:SnowflakeRegion      = Value(1, "us-west-2")
    val US_EAST_1:SnowflakeRegion      = Value(2, "us-east-1")
    val CA_CENTRAL_1:SnowflakeRegion   = Value(3, "ca-central-1")
    val EU_WEST_1:SnowflakeRegion      = Value(4, "eu-west-1")
    val EU_CENTRAL_1:SnowflakeRegion   = Value(5, "eu-central-1")
    val AP_SOUTHEAST_1:SnowflakeRegion = Value(6, "ap-southeast-1")
    val AP_SOUTHEAST_2:SnowflakeRegion = Value(7, "ap-southeast-2")
    /* AZURE */
    val EAST_US_2_AZURE:SnowflakeRegion      = Value(8,  "east-us-2.azure")
    val CANADA_CENTRAL_AZURE:SnowflakeRegion = Value(9,  "canada-central.azure")
    val WEST_EUROPE_AZURE:SnowflakeRegion    = Value(10, "west-europe.azure")
    val AUSTRALIA_EAST_AZURE:SnowflakeRegion = Value(11, "australia-east.azure")
    val SOUTHEAST_ASIA_AZURE:SnowflakeRegion = Value(12, "southeast-asia.azure")

}

object SnowflakeRegionUtil {

  def fromString(text:String):SnowflakeRegions.Value = {

    val regions = SnowflakeRegions.values.filter(region => {
      text == region.toString
    })

    if (regions.isEmpty) return null
    regions.head

  }

}