package de.kp.works.stream.sql.aerospike

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

object AEROSPIKE_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.aerospike.AerospikeSinkProvider"
  /**
   * The maximum batch size of the internal cache
   * before writing to Aerospike
   */
  val BATCH_SIZE = "batch.size"
  /**
   * The Aerospike authentication mode. Values are
   * INTERNAL, EXTERNAL, EXTERNAL_INSECURE, PKI.
   *
   * Default is INTERNAL
   */
  val AEROSPIKE_AUTH_MODE   = "aerospike.auth.mode"
  /**
	 * Record expiration. Also known as ttl (time to live).
	 * Seconds record will live before being removed by the server.
	 *
	 * Expiration values:
	 *
	 *  -2: Do not change ttl when record is updated.
	 *  -1: Never expire.
	 *   0: Default to namespace configuration variable "default-ttl" on the server.
	 * > 0: Actual ttl in seconds.
   *
	 * Default: 0
	 */
  val AEROSPIKE_EXPIRATION  = "aerospike.expiration"
  /**
   * The host of the Aerospike database
   */
  val AEROSPIKE_HOST        = "aerospike.host"
  /**
   * The maximum number of retries to write
   * to Aerospike instance
   */
  val AEROSPIKE_MAX_RETRIES = "aerospike.max.retries"
  /**
   * The name of the Aerospike namespace used
   * to organize data
   */
  val AEROSPIKE_NAMESPACE   = "aerospike.namespace"
  /**
   * Password of the registered user.
   * Required for authentication
   */
  val AEROSPIKE_PASSWORD    = "aerospike.password"
  /**
   * The port of the Aerospike database
   */
  val AEROSPIKE_PORT        = "aerospike.port"
  /**
   * The name of the Aerospike set used to
   * organize data
   */
  val AEROSPIKE_SET         = "aerospike.set"
  val AEROSPIKE_TIMEOUT     = "aerospike.timeout"
  val AEROSPIKE_TLS_MODE    = "aerospike.tls.mode"
  val AEROSPIKE_TLS_NAME    = "aerospike.tls.name"
  /**
   * Name of a registered user name.
   * Required for authentication
   */
  val AEROSPIKE_USER        = "aerospike.username"
  val AEROSPIKE_WRITE       = "aerospike.write"
}
