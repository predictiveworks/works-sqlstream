package de.kp.works.stream.sql.transform.zeek
/*
 * Copyright (c) 2019 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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

import com.google.gson.{JsonArray, JsonObject}
import scala.collection.JavaConversions._

object ZeekReplace {

  def replace_capture_loss(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and transform time values;
     * as these fields are declared to be non nullable, we do not check
     * their existence
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "ts_delta")

    newObject

  }

  def replace_conn(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and transform time values;
     as these fields are declared to be non nullable, we do not check
     * their existence
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "duration")

    newObject = replace_conn_id(newObject)
    val oldNames = List(
      "orig_bytes",
      "resp_bytes",
      "local_orig",
      "local_resp",
      "orig_pkts",
      "orig_ip_bytes",
      "resp_pkts",
      "resp_ip_bytes",
      "orig_l2_addr",
      "resp_l2_addr")

    oldNames.foreach(oldName =>
      newObject = replace_name(newObject, ZeekMapper.mapping(oldName), oldName))

    newObject

  }

  def replace_dce_rpc(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "rtt")

    newObject = replace_conn_id(newObject)
    newObject
  }

  def replace_dhcp(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "lease_time")

    newObject = replace_interval(newObject, "duration")
    newObject

  }

  def replace_dnp3(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_dns(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "rtt")

    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "dns_aa", "AA")
    newObject = replace_name(newObject, "dns_tc", "TC")
    newObject = replace_name(newObject, "dns_rd", "RD")
    newObject = replace_name(newObject, "dns_ra", "RA")
    newObject = replace_name(newObject, "dns_z", "Z")

    /*
     * Rename and transform 'TTLs'
     */
    newObject = replace_name(newObject, "dns_ttls", "TTLs")
    val ttls = newObject.remove("dns_ttls").getAsJsonArray

    val new_ttls = new JsonArray()
    ttls.foreach(ttl => {

      var interval = 0L
      try {

        val ts = ttl.getAsDouble
        interval = (ts * 1000).asInstanceOf[Number].longValue

      } catch {
        case _: Throwable => /* Do nothing */
      }

      new_ttls.add(interval)

    })

    newObject.add("dns_ttls", new_ttls)
    newObject

  }

  def replace_dpd(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_files(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "duration")

    newObject = replace_name(newObject, "source_ips", "tx_hosts")
    newObject = replace_name(newObject, "destination_ips", "rx_hosts")

    newObject

  }

  def replace_ftp(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "data_channel_passive", "data_channel.passive")
    newObject = replace_name(newObject, "data_channel_source_ip", "data_channel.orig_h")

    newObject = replace_name(newObject, "data_channel_destination_ip", "data_channel.resp_h")
    newObject = replace_name(newObject, "data_channel_destination_port", "data_channel.resp_p")

    newObject

  }

  def replace_http(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_intel(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "seen_indicator", "seen.indicator")
    newObject = replace_name(newObject, "seen_indicator_type", "seen.indicator_type")

    newObject = replace_name(newObject, "seen_host", "seen.host")
    newObject = replace_name(newObject, "seen_where", "seen.where")

    newObject = replace_name(newObject, "seen_node", "seen.node")

    newObject = replace_name(newObject, "cif_tags", "cif.tags")
    newObject = replace_name(newObject, "cif_confidence", "cif.confidence")

    newObject = replace_name(newObject, "cif_source", "cif.source")
    newObject = replace_name(newObject, "cif_description", "cif.description")

    newObject = replace_name(newObject, "cif_firstseen", "cif.firstseen")
    newObject = replace_name(newObject, "cif_lastseen", "cif.lastseen")

    newObject

  }

  def replace_irc(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_kerberos(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_time(newObject, "from")
    newObject = replace_time(newObject, "till")

    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_modbus(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_mysql(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_notice(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "suppress_for")

    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "source_ip", "src")
    newObject = replace_name(newObject, "destination_ip", "dst")

    newObject = replace_name(newObject, "source_port", "p")

    newObject = replace_name(newObject, "country_code", "remote_location.country_code")
    newObject = replace_name(newObject, "region", "remote_location.region")
    newObject = replace_name(newObject, "city", "remote_location.city")

    newObject = replace_name(newObject, "latitude", "remote_location.latitude")
    newObject = replace_name(newObject, "longitude", "remote_location.longitude")

    newObject

  }

  def replace_ntlm(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_ocsp(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_time(newObject, "revoketime")

    newObject = replace_time(newObject, "thisUpdate")
    newObject = replace_time(newObject, "nextUpdate")

    newObject = replace_name(newObject, "hash_algorithm", "hashAlgorithm")
    newObject = replace_name(newObject, "issuer_name_hash", "issuerNameHash")

    newObject = replace_name(newObject, "issuer_key_hash", "issuerKeyHash")
    newObject = replace_name(newObject, "serial_number", "serialNumber")

    newObject = replace_name(newObject, "cert_status", "certStatus")
    newObject = replace_name(newObject, "revoke_time", "revoketime")


    newObject = replace_name(newObject, "revoke_reason", "revokereason")
    newObject = replace_name(newObject, "update_this", "thisUpdate")

    newObject = replace_name(newObject, "update_next", "nextUpdate")

    newObject

  }

  def replace_pe(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_time(newObject, "compile_ts")

    newObject

  }

  def replace_radius(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "ttl")

    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_rdp(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_rfb(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_sip(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_smb_cmd(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "rtt")

    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "file_ts", "referenced_file.ts")
    newObject = replace_time(newObject, "file_ts")

    newObject = replace_name(newObject, "file_uid", "referenced_file.uid")
    newObject = replace_name(newObject, "file_source_ip", "referenced_file.id.orig_h")

    newObject = replace_name(newObject, "file_source_port", "referenced_file.id.orig_p")
    newObject = replace_name(newObject, "file_destination_ip", "referenced_file.id.resp_h")

    newObject = replace_name(newObject, "file_destination_port", "referenced_file.id.resp_p")
    newObject = replace_name(newObject, "file_fuid", "referenced_file.fuid")

    newObject = replace_name(newObject, "file_action", "referenced_file.action")
    newObject = replace_name(newObject, "file_path", "referenced_file.path")

    newObject = replace_name(newObject, "file_name", "referenced_file.name")
    newObject = replace_name(newObject, "file_size", "referenced_file.size")

    newObject = replace_name(newObject, "file_prev_name", "referenced_file.prev_name")

    newObject = replace_name(newObject, "", "referenced_file.times.modified")
    newObject = replace_time(newObject, "file_times_modified")

    newObject = replace_name(newObject, "file_times_accessed", "referenced_file.times.accessed")
    newObject = replace_time(newObject, "file_times_accessed")

    newObject = replace_name(newObject, "file_times_created", "referenced_file.times.created")
    newObject = replace_time(newObject, "file_times_created")

    newObject = replace_name(newObject, "file_times_changed", "referenced_file.times.changed")
    newObject = replace_time(newObject, "file_times_changed")

    newObject

  }

  def replace_smb_files(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")

    newObject = replace_time(newObject, "times.modified")
    newObject = replace_time(newObject, "times.accessed")

    newObject = replace_time(newObject, "times.created")
    newObject = replace_time(newObject, "times.changed")

    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "times_modified", "times.modified")
    newObject = replace_name(newObject, "times_accessed", "times.accessed")

    newObject = replace_name(newObject, "times_created", "times.created")
    newObject = replace_name(newObject, "times_changed", "times.changed")

    newObject

  }

  def replace_smb_mapping(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_smtp(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_snmp(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_time(newObject, "up_since")

    newObject = replace_interval(newObject, "duration")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_socks(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "request_host", "request.host")
    newObject = replace_name(newObject, "request_name", "request.name")
    newObject = replace_name(newObject, "request_port", "request_p")

    newObject = replace_name(newObject, "bound_host", "bound.host")
    newObject = replace_name(newObject, "bound_name", "bound.name")
    newObject = replace_name(newObject, "bound_port", "bound_p")

    newObject

  }

  def replace_ssh(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "country_code", "remote_location.country_code")
    newObject = replace_name(newObject, "region", "remote_location.region")
    newObject = replace_name(newObject, "city", "remote_location.city")

    newObject = replace_name(newObject, "latitude", "remote_location.latitude")
    newObject = replace_name(newObject, "longitude", "remote_location.longitude")

    newObject

  }

  def replace_ssl(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject = replace_name(newObject, "notary_first_seen", "notary.first_seen")
    newObject = replace_name(newObject, "notary_last_seen", "notary.last_seen")

    newObject = replace_name(newObject, "notary_times_seen", "notary.times_seen")
    newObject = replace_name(newObject, "notary_valid", "notary.valid")

    newObject

  }

  def replace_stats(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_interval(newObject, "pkt_lag")

    newObject

  }

  def replace_syslog(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_traceroute(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")

    newObject = replace_name(newObject, "source_ip", "src")
    newObject = replace_name(newObject, "destination_ip", "dst")

    newObject

  }

  def replace_tunnel(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_weird(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_conn_id(newObject)

    newObject

  }

  def replace_x509(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    /*
     * Prepare JsonObject, i.e. rename fields and
     * transform time values
     */
    newObject = replace_time(newObject, "ts")
    newObject = replace_certificate(newObject)

    newObject = replace_name(newObject, "san_dns", "san.dns")
    newObject = replace_name(newObject, "san_uri", "san.uri")

    newObject = replace_name(newObject, "san_email", "san.email")
    newObject = replace_name(newObject, "san_ip", "san.ip")

    newObject = replace_name(newObject, "san_other_fields", "san.other_fields")

    newObject = replace_name(newObject, "basic_constraints_ca", "basic_constraints.ca")
    newObject = replace_name(newObject, "basic_constraints_path_len", "basic_constraints.path_len")

    newObject

  }

  def replace_certificate(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject

    newObject = replace_time(newObject, "certificate.not_valid_before")
    newObject = replace_time(newObject, "certificate.not_valid_after")

    newObject = replace_name(newObject, "cert_version", "certificate.version")
    newObject = replace_name(newObject, "cert_serial", "certificate.serial")

    newObject = replace_name(newObject, "cert_subject", "certificate.subject")
    newObject = replace_name(newObject, "cert_cn", "certificate.cn")

    newObject = replace_name(newObject, "cert_not_valid_before", "certificate.not_valid_before")
    newObject = replace_name(newObject, "cert_not_valid_after", "certificate.not_valid_after")

    newObject = replace_name(newObject, "cert_key_alg", "certificate.key_alg")
    newObject = replace_name(newObject, "cert_sig_alg", "certificate.sig_alg")

    newObject = replace_name(newObject, "cert_key_type", "certificate.key_type")
    newObject = replace_name(newObject, "cert_key_length", "certificate.key_length")

    newObject = replace_name(newObject, "cert_exponent", "certificate.exponent")
    newObject = replace_name(newObject, "cert_curve", "certificate.curve")

    newObject

  }

  /**
   * HINT: The provided connection parameters can be used
   * to build a unique (hash) connection identifier to join
   * with other data source like Osquery.
   */
  def replace_conn_id(oldObject: JsonObject): JsonObject = {

    var newObject = oldObject
    val oldNames = List(
      "id.orig_h",
      "id.orig_p",
      "id.resp_h",
      "id.resp_p")

    oldNames.foreach(oldName =>
      newObject = replace_name(newObject, ZeekMapper.mapping(oldName), oldName))

    newObject

  }

  /** HELPER METHOD **/

  /**
   * Zeek specifies intervals (relative time) as Double
   * that defines seconds; this method transforms them
   * into milliseconds
   */
  def replace_interval(jsonObject: JsonObject, intervalName: String): JsonObject = {

    if (jsonObject == null || jsonObject.get(intervalName) == null) return jsonObject

    var interval: Long = 0L
    try {

      val ts = jsonObject.get(intervalName).getAsDouble
      interval = (ts * 1000).asInstanceOf[Number].longValue()

    } catch {
      case _: Throwable => /* Do nothing */
    }

    jsonObject.remove(intervalName)
    jsonObject.addProperty(intervalName, interval)

    jsonObject

  }

  def replace_name(jsonObject: JsonObject, newName: String, oldName: String): JsonObject = {

    try {

      if (jsonObject == null || jsonObject.get(oldName) == null) return jsonObject
      val value = jsonObject.remove(oldName)

      jsonObject.add(newName, value)
      jsonObject

    } catch {
      case _: Throwable => jsonObject
    }

  }

  /**
   * Zeek specifies timestamps (absolute time) as Double
   * that defines seconds; this method transforms them
   * into milliseconds
   */
  def replace_time(jsonObject: JsonObject, timeName: String): JsonObject = {

    if (jsonObject == null || jsonObject.get(timeName) == null) return jsonObject

    var timestamp: Long = 0L
    try {

      val ts = jsonObject.get(timeName).getAsDouble
      timestamp = (ts * 1000).asInstanceOf[Number].longValue()

    } catch {
      case _: Throwable => /* Do nothing */
    }

    jsonObject.remove(timeName)
    jsonObject.addProperty(timeName, timestamp)

    jsonObject

  }

}
