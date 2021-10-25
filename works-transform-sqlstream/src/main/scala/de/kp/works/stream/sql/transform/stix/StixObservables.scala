package de.kp.works.stream.sql.transform.stix
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

object StixObservables extends Enumeration {

  type StixObservable = Value

  val ARTIFACT: StixObservable           = Value(1, "artifact")
  val AUTONOMOUS_SYSTEM: StixObservable  = Value(2, "autonomous-system")
  val DIRECTORY: StixObservable          = Value(3, "directory")
  val DOMAIN_NAME: StixObservable        = Value(4, "domain-name")
  val EMAIL_ADDRESS: StixObservable      = Value(5, "email-addr")
  val EMAIL_MESSAGE: StixObservable      = Value(6, "email-message")
  val File: StixObservable               = Value(7, "file")
  val IPv4Address: StixObservable        = Value(8, "ipv4-addr")
  val IPv6Address: StixObservable        = Value(9, "ipv6-addr")
  val MACAddress: StixObservable         = Value(10, "mac-addr")
  val Mutex: StixObservable              = Value(11, "mutex")
  val NetworkTraffic: StixObservable     = Value(12, "network-traffic")
  val Process: StixObservable            = Value(13, "process")
  val Software: StixObservable           = Value(14, "software")
  val URL: StixObservable                = Value(15, "url")
  val UserAccount: StixObservable        = Value(16, "user-account")
  val WindowsRegistryKey: StixObservable = Value(17, "windows-registry-key")
  val X509Certificate: StixObservable    = Value(18, "x509-certificate")

}
