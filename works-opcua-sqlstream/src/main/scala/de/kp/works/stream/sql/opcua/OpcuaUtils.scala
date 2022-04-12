package de.kp.works.stream.sql.opcua

/**
 * Copyright (c) 2020 - 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

import java.net.{Inet4Address, InetAddress, NetworkInterface, SocketException, UnknownHostException}
import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
import scala.collection.mutable

object OpcuaUtils extends Logging {

  final val APPLICATION_NAME = "OPC-UA SQLSTREAM@" + getHostname
  final val APPLICATION_URI  = s"urn:$getHostname:works:opcua"

  def getRootNodeIdOfName(item:String):String = {
    item match {
      case "Root"    => "i=84"
      case "Objects" => "i=85"
      case "Types"   => "i=86"
      case "Views"   => "i=87"
      case _ => item

    }
  }

  /** HOSTNAME UTILS **/

  def getHostname:String = {

    try {
      InetAddress.getLocalHost.getHostName

    } catch {
      case _:UnknownHostException => "localhost"
    }

  }

  def getHostnames(address:String): Set[String] = {
    getHostnames(address, includeLoopback = true)
  }

  def getHostnames(address:String, includeLoopback:Boolean): Set[String] = {

    val hostnames = mutable.HashSet.empty[String]
    try {

      val inetAddress = InetAddress.getByName(address)
      if (inetAddress.isAnyLocalAddress) {

        try {

          NetworkInterface.getNetworkInterfaces
            .asScala.toSeq.foreach(netInterface => {

              netInterface.getInetAddresses
                .asScala.toSeq.foreach(inetAddress => {
                  if (inetAddress.isInstanceOf[Inet4Address]) {
                    if (includeLoopback || !inetAddress.isLoopbackAddress) {
                      hostnames += inetAddress.getHostName
                      hostnames += inetAddress.getHostAddress
                      hostnames += inetAddress.getCanonicalHostName
                    }
                  }
              })

          })

        } catch {
          case e:SocketException =>
            val message = s"Retrieval of host name failed for address: $address"
            log.warn(message, e)
        }

      } else {

        if (includeLoopback || !inetAddress.isLoopbackAddress) {
          hostnames += inetAddress.getHostName
          hostnames += inetAddress.getHostAddress
          hostnames += inetAddress.getCanonicalHostName
        }

      }

    } catch {
      case e:UnknownHostException =>
        val message = s"Failed to get InetAddress for address: $address"
        log.warn(message, e)
    }

    hostnames.toSet

  }

}
