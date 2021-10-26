package de.kp.works.stream.sql.transform.stix

import org.apache.spark.sql.types._

/*
 * STIX v2.1 2019-07-26
 */
object StixSchema {

  /** DATA TYPES */

  /**
   * IMPORTANT: Extensions usually represent a dictionary
   * with a single key (the extension) and the value itself
   * is a dictionary as well.
   *
   * This approach flattens the provided extension data by
   * prefix the field names with the extension key.
   */
  val extensions: StructField =
    StructField("extensions", MapType(StringType, StringType), nullable = true)

  val hashes: StructField =
    StructField("hashes", MapType(StringType, StringType), nullable = true)

  val MimePartType:StructType = {

    val fields = Array(
      /*
       * Specifies the contents of the MIME part if the content_type is not
       * provided or starts with text/ (e.g., in the case of plain text or
       * HTML email).
       *
       * For inclusion in this property, the contents MUST be decoded to
       * Unicode. Note that the charset provided in content_type is for
       * informational usage and not for decoding of this property.
       */
      StructField("body", StringType, nullable = true),
      /*
       * Specifies the contents of non-textual MIME parts, that is those whose
       * content_type does not start with text/, as a reference to an Artifact
       * object or File object.
       *
       * The object referenced in this property MUST be of type artifact or file.
       * For use cases where conveying the actual data contained in the MIME part
       * is of primary importance, artifact SHOULD be used. Otherwise, for use
       * cases where conveying metadata about the file-like properties of the MIME
       * part is of primary importance, file SHOULD be used.
       */
      StructField("body_raw_ref", StringType, nullable = true),
      /*
       * Specifies the value of the “Content-Type” header field of the MIME part.
       * Any additional “Content-Type” header field parameters such as charset
       * SHOULD be included in this property.
       *
       * Example:
       * text/html; charset=UTF-8
       */
      StructField("content_type", StringType, nullable = true),
      /*
       * Specifies the value of the “Content-Disposition” header field
       * of the MIME part.
       */
      StructField("content_disposition", StringType, nullable = true)

    )

    StructType(fields)

  }

  val WindowsRegistryValueType:StructType = {

    val fields = Array(
      /*
       * Specifies the name of the registry value. For specifying the default value in a
       * registry key, an empty string MUST be used.
       */
      StructField("name", StringType, nullable = true),
      /*
       * Specifies the data contained in the registry value.
       */
      StructField("data", StringType, nullable = true),
      /*
       * Specifies the registry (REG_*) data type used in the registry value. The values of
       * this property MUST come from the windows-registry-datatype-enum enumeration.
       */
      StructField("data_type", StringType, nullable = true)
    )

    StructType(fields)

  }

  val X509V3ExtensionsType:StructType = {

    val fields = Array(
      /*
       * Specifies a multi-valued extension which indicates whether a certificate is a CA certificate.
       * The first (mandatory) name is CA followed by TRUE or FALSE. If CA is TRUE, then an optional
       * path-len name followed by a non-negative value can be included. Also equivalent to the object
       * ID (OID) value of 2.5.29.19.
       */
      StructField("basic_constraints", StringType, nullable = true),
      /*
       * Specifies a namespace within which all subject names in subsequent certificates in a certification
       * path MUST be located. Also equivalent to the object ID (OID) value of 2.5.29.30.
       */
      StructField("name_constraints", StringType, nullable = true),
      /*
       * Specifies any constraints on path validation for certificates issued to CAs. Also equivalent to the
       * object ID (OID) value of 2.5.29.36.
       */
      StructField("policy_constraints", StringType, nullable = true),
      /*
       * Specifies a multi-valued extension consisting of a list of names of the permitted key usages.
       * Also equivalent to the object ID (OID) value of 2.5.29.15.
       */
      StructField("key_usage", StringType, nullable = true),
      /*
       * Specifies a list of usages indicating purposes for which the certificate public key can be used
       * for. Also equivalent to the object ID (OID) value of 2.5.29.37.
       */
      StructField("extended_key_usage", StringType, nullable = true),
      /*
       * Specifies the identifier that provides a means of identifying certificates that contain a particular
       * public key. Also equivalent to the object ID (OID) value of 2.5.29.14.
       */
      StructField("subject_key_identifier", StringType, nullable = true),
      /*
       * Specifies the identifier that provides a means of identifying the public key corresponding to the
       * private key used to sign a certificate. Also equivalent to the object ID (OID) value of 2.5.29.35.
       */
      StructField("authority_key_identifier", StringType, nullable = true),
      /*
       * Specifies the additional identities to be bound to the subject of the certificate. Also equivalent
       * to the object ID (OID) value of 2.5.29.17.
       */
      StructField("subject_alternative_name", StringType, nullable = true),
      /*
       * Specifies the additional identities to be bound to the issuer of the certificate. Also equivalent
       * to the object ID (OID) value of 2.5.29.18.
       */
      StructField("issuer_alternative_name", StringType, nullable = true),
      /*
       * Specifies the identification attributes (e.g., nationality) of the subject. Also equivalent to the
       * object ID (OID) value of 2.5.29.9.
       */
      StructField("subject_directory_attributes", StringType, nullable = true),
      /*
       * Specifies how CRL information is obtained. Also equivalent to the object ID (OID) value of 2.5.29.31.
       */
      StructField("crl_distribution_points", StringType, nullable = true),
      /*
       * Specifies the number of additional certificates that may appear in the path before anyPolicy is no
       * longer permitted. Also equivalent to the object ID (OID) value of 2.5.29.54.
       */
      StructField("inhibit_any_policy", StringType, nullable = true),
      /*
       * Specifies the date on which the validity period begins for the private key, if it is different from
       * the validity period of the certificate.
       */
      StructField("private_key_usage_period_not_before", TimestampType, nullable = true),
      /*
       * Specifies the date on which the validity period ends for the private key, if it is different from
       * the validity period of the certificate.
       */
      StructField("private_key_usage_period_not_after", TimestampType, nullable = true),
      /*
       * Specifies a sequence of one or more policy information terms, each of which consists of an object
       * identifier (OID) and optional qualifiers. Also equivalent to the object ID (OID) value of 2.5.29.32.
       */
      StructField("certificate_policies", StringType, nullable = true),
      /*
       * Specifies one or more pairs of OIDs; each pair includes an issuerDomainPolicy and a subjectDomainPolicy.
       * The pairing indicates whether the issuing CA considers its issuerDomainPolicy equivalent to the subject
       * CA's subjectDomainPolicy. Also equivalent to the object ID (OID) value of 2.5.29.33.
       */
      StructField("policy_mappings", StringType, nullable = true)

    )

    StructType(fields)

  }
  /**
   * CYBER OBSERVABLES
   *
   * The schema definitions below do not contain the common identifier field
   * for the respective. It is assigned to the each schema before being provided.
   */

  def fromName(name:String):StructType = {

    try {

      val methods = StixSchema.getClass.getMethods

      val method = methods.filter(m => m.getName == name).head
      val schema = method.invoke(StixSchema).asInstanceOf[StructType]

      val fields = Array(StructField("id", StringType, nullable = false)) ++ schema.fields
      StructType(fields)

    } catch {
      case _:Throwable => null
    }

  }

  def artifact():StructType = {

    val fields = Array(
      /*
       * Whenever feasible, this value SHOULD be one of the values defined
       * in the Template column in the IANA media type registry [Media Types].
       *
       * Maintaining a comprehensive universal catalog of all extant file types
       * is obviously not possible. When specifying a MIME Type not included in
       * the IANA registry, implementers should use their best judgement so as
       * to facilitate interoperability.
       */
      StructField("mime_type", StringType, nullable = true),
      /*
       * Specifies the binary data contained in the artifact as a base64-encoded
       * string.
       *
       * This property MUST NOT be present if url is provided.
       */
      StructField("payload_bin", StringType, nullable = true),
      /*
       * The value of this property MUST be a valid URL that resolves to the
       * unencoded content.
       *
       * This property MUST NOT be present if payload_bin is provided.
       */
      StructField("url", StringType, nullable = true),
      /*
       * Specifies a dictionary of hashes for the contents of the url or the
       * payload_bin.
       *
       * This property MUST be present when the url property is present.
       * Dictionary keys MUST come from the hash-algorithm-ov.
       */
      hashes,
      /*
       * If the artifact is encrypted, specifies the type of encryption algorithm
       * the binary data  (either via payload_bin or url) is encoded in.
       *
       * The value of this property MUST come from the encryption-algorithm-enum
       * enumeration. If both mime_type and encryption_algorithm are included, this
       * signifies that the artifact represents an encrypted archive.
       */
      StructField("encryption_algorithm", StringType, nullable = true),
      /*
       * Specifies the decryption key for the encrypted binary data (either via
       * payload_bin or url). For example, this may be useful in cases of sharing
       * malware samples, which are often encoded in an encrypted archive.
       *
       * This property MUST NOT be present when the encryption_algorithm property is absent.
       */
      StructField("decryption_key", StringType, nullable = true)
    )

    StructType(fields)

  }

  def autonomous_system():StructType = {

    val fields = Array(
      /*
       * Specifies the number assigned to the AS. Such assignments are typically
       * performed by a Regional Internet Registry (RIR).
       */
      StructField("number", LongType, nullable = false),
      /*
       * Specifies the name of the AS.
       */
      StructField("name", StringType, nullable = true),
      /*
       * Specifies the name of the Regional Internet Registry (RIR) that assigned
       * the number to the AS.
       */
      StructField("rir", StringType, nullable = true)
    )

    StructType(fields)

  }

  def directory():StructType = {

    val fields = Array(
      /*
       * Specifies the path, as originally observed, to the directory on the file system.
       */
      StructField("path", StringType, nullable = false),
      /*
       * Specifies the observed encoding for the path. The value MUST be specified if the
       * path is stored in a non-Unicode encoding. This value MUST be specified using the
       * corresponding name from the 2013-12-20 revision of the IANA character set registry
       * [Character Sets]. If the preferred MIME name for a character set is defined, this
       * value MUST be used; if it is not defined, then the Name value from the registry
       * MUST be used instead.
       */
      StructField("path_enc", StringType, nullable = true),
      /*
       * Specifies the date/time the directory was created.
       */
      StructField("ctime", TimestampType, nullable = true),
      /*
       * Specifies the date/time the directory was last written to/modified.
       */
      StructField("mtime", TimestampType, nullable = true),
      /*
       * Specifies the date/time the directory was last accessed.
       */
      StructField("atime", TimestampType, nullable = true),
      /*
       * Specifies a list of references to other File and/or Directory objects contained
       * within the directory.
       *
       * The objects referenced in this list MUST be of type file or directory.
       */
      StructField("contains_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }
   def domain_name():StructType = {

     val fields = Array(
       /*
        * Specifies the value of the domain name. The value of this property MUST
        * conform to [RFC1034], and each domain and sub-domain contained within the
        * domain name MUST conform to [RFC5890].
        */
       StructField("value", StringType, nullable = false),
       /*
        * Specifies a list of references to one or more IP addresses or domain names
        * that the domain name resolves to.
        *
        * The objects referenced in this list MUST be of type ipv4-addr or ipv6-addr
        * or domain-name (for cases such as CNAME records).
        */
       StructField("resolves_to_refs", ArrayType(StringType, containsNull = false), nullable = true)
     )

     StructType(fields)

   }

  def email_address():StructType = {

    val fields = Array(
      /*
       * Specifies the value of the email address. This MUST NOT include the display name.
       * This property corresponds to the addr-spec construction in section 3.4 of [RFC5322],
       * for example, jane.smith@example.com.
       */
      StructField("value", StringType, nullable = false),
      /*
       * Specifies a single email display name, i.e., the name that is displayed to the human
       * user of a mail application. This property corresponds to the display-name construction
       * in section 3.4 of [RFC5322], for example, Jane Smith.
       */
      StructField("display_name", StringType, nullable = true),
      /*
       * Specifies the user account that the email address belongs to, as a reference to a User
       * Account object. The object referenced in this property MUST be of type user-account.
       */
      StructField("belongs_to_ref", StringType, nullable = true)
    )

    StructType(fields)

  }

  def email_message():StructType = {

    val fields = Array(
      /*
       * Indicates whether the email body contains multiple MIME parts.
       */
      StructField("is_multipart", BooleanType, nullable = false),
      /*
       * Specifies the date/time that the email message was sent.
       */
      StructField("date", TimestampType, nullable = true),
      /*
       * Specifies the value of the “Content-Type” header of the email
       * message.
       */
      StructField("content_type", StringType, nullable = true),
      /*
       * Specifies the value of the “From:” header of the email message.
       * The "From:" field specifies the author of the message, that is,
       * the mailbox(es) of the person or system responsible for the
       * writing of the message.
       *
       * The object referenced in this property MUST be of type email-address.
       */
      StructField("from_ref", StringType, nullable = true),
      /*
       * Specifies the value of the “Sender” field of the email message.
       * The "Sender:" field specifies the mailbox of the agent responsible
       * for the actual transmission of the message.
       *
       * The object referenced in this property MUST be of type email-address.
       */
      StructField("sender_ref", StringType, nullable = true),
      /*
       * Specifies the mailboxes that are “To:” recipients of the email message.
       * The objects referenced in this list MUST be of type email-address.
       */
      StructField("to_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies the mailboxes that are “CC:” recipients of the email message.
       * The objects referenced in this list MUST be of type email-address.
       */
      StructField("cc_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies the mailboxes that are “BCC:” recipients of the email message.
       * As per [RFC5322], the absence of this property should not be interpreted
       * as semantically equivalent to an absent BCC header on the message being
       * characterized.
       *
       * The objects referenced in this list MUST be of type email-address.
       */
      StructField("bcc_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies the Message-ID field of the email message.
       */
      StructField("message_id", StringType, nullable = true),
      /*
       * Specifies the subject of the email message.
       */
      StructField("subject", StringType, nullable = true),
      /*
       * Specifies one or more "Received" header fields that may be included
       * in the email headers.List values MUST appear in the same order as present
       * in the email message.
       */
      StructField("received_lines", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies any other header fields (except for date, received_lines, content_type,
       * from_ref, sender_ref, to_refs, cc_refs, bcc_refs, and subject) found in the email
       * message, as a dictionary.
       *
       * Each key/value pair in the dictionary represents the name/value of a single header
       * field or names/values of a header field that occurs more than once. Each dictionary
       * key SHOULD be a case-preserved version of the header field name. The corresponding
       * value for each dictionary key MUST always be a list of type string to support when
       * a header field is repeated.
       */
      StructField("additional_header_fields", MapType(StringType, StringType), nullable = true),
      /*
       * Specifies a string containing the email body. This property MUST NOT be used if
       * is_multipart is true.
       */
      StructField("body", StringType, nullable = true),
      /*
       * Specifies a list of the MIME parts that make up the email body. This property MUST
       * NOT be used if is_multipart is false.
       */
      StructField("body_multipart", ArrayType(MimePartType, containsNull = false), nullable = true),
      /*
       * Specifies the raw binary contents of the email message, including both the headers
       * and body, as a reference to an Artifact object.
       *
       * The object referenced in this property MUST be of type artifact.
       */
      StructField("raw_email_ref", StringType, nullable = true)
    )

    StructType(fields)

  }

  def files():StructType = {

    val fields = Array(
      /*
       * The File object defines the following extensions. In addition to these,
       * producers MAY create their own.
       *
       * ntfs-ext, raster-image-ext, pdf-ext, archive-ext, windows-pebinary-ext
       *
       * Dictionary keys MUST identify the extension type by name. The corresponding
       * dictionary values MUST contain the contents of the extension instance.
       */
      extensions,
      /*
       * Specifies a dictionary of hashes for the file. (When used with the Archive
       * File Extension, this refers to the hash of the entire archive file, not its
       * contents.)
       *
       * Dictionary keys MUST come from the hash-algorithm-ov.
       */
      hashes,
      /*
       * Specifies the size of the file, in bytes. The value of this property
       * MUST NOT be negative.
       */
      StructField("size", LongType, nullable = true),
      /*
       * Specifies the name of the file.
       */
      StructField("name", StringType, nullable = true),
      /*
       * Specifies the observed encoding for the name of the file. This value MUST
       * be specified using the corresponding name from the 2013-12-20 revision of
       * the IANA character set registry [Character Sets]. If the value from the
       * Preferred MIME Name column for a character set is defined, this value MUST
       * be used; if it is not defined, then the value from the Name column in the
       * registry MUST be used instead.
       *
       * This property allows for the capture of the original text encoding for the
       * file name, which may be forensically relevant; for example, a file on an NTFS
       * volume whose name was created using the windows-1251 encoding, commonly used
       * for languages based on Cyrillic script.
       */
      StructField("name_enc", StringType, nullable = true),
      /*
       * Specifies the hexadecimal constant (“magic number”) associated with a specific
       * file format that corresponds to the file, if applicable.
       */
      StructField("magic_number_hex", StringType, nullable = true),
      /*
       * Specifies the MIME type name specified for the file, e.g., application/msword.
       * Whenever feasible, this value SHOULD be one of the values defined in the Template
       * column in the IANA media type registry [Media Types].
       *
       * Maintaining a comprehensive universal catalog of all extant file types is obviously
       * not possible. When specifying a MIME Type not included in the IANA registry,
       * implementers should use their best judgement so as to facilitate interoperability.
       */
      StructField("mime_type", StringType, nullable = true),
      /*
       * Specifies the date/time the file was created.
       */
      StructField("ctime", TimestampType, nullable = true),
      /*
       * Specifies the date/time the file was last written to/modified.
       */
      StructField("mtime", TimestampType, nullable = true),
      /*
       * Specifies the date/time the file was last accessed.
       */
      StructField("atime", TimestampType, nullable = true),
      /*
       * Specifies the parent directory of the file, as a reference to
       * a Directory object. The object referenced in this property MUST
       * be of type directory.
       */
      StructField("parent_directory_ref", StringType, nullable = true),
      /*
       * Specifies a list of references to other Cyber-observable Objects
       * contained within the file, such as another file that is appended
       * to the end of the file, or an IP address that is contained somewhere
       * in the file.
       *
       * This is intended for use cases other than those targeted by the Archive
       * extension.
       */
      StructField("contains_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies the content of the file, represented as an Artifact object.
       * The object referenced in this property MUST be of type artifact.
       */
      StructField("content_ref", StringType, nullable = true)
    )

    StructType(fields)

  }

  def ipv4_addr():StructType = {

    val fields = Array(
      /*
       * Specifies one or more IPv4 addresses expressed using CIDR notation.
       * If a given IPv4 Address Object represents a single IPv4 address, the
       * CIDR /32 suffix MAY be omitted.
       *
       * Example: 10.2.4.5/24
       */
      StructField("value", StringType, nullable = false),
      /*
       * Specifies a list of references to one or more Layer 2 Media Access
       * Control (MAC) addresses that the IPv4 address resolves to.
       *
       * The objects referenced in this list MUST be of type mac-addr.
       */
      StructField("resolves_to_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies a list of reference to one or more autonomous systems (AS)
       * that the IPv4 address belongs to.
       *
       * The objects referenced in this list MUST be of type autonomous-system.
       */
      StructField("belongs_to_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def ipv6_addr():StructType = {

    val fields = Array(
      /*
       * Specifies the values of one or more IPv6 addresses expressed using CIDR notation.
       * If a given IPv6 Address object represents a single IPv6 address, the CIDR /128
       * suffix MAY be omitted.
       */
      StructField("value", StringType, nullable = false),
      /*
       * Specifies a list of references to one or more Layer 2 Media Access
       * Control (MAC) addresses that the IPv6 address resolves to.
       *
       * The objects referenced in this list MUST be of type mac-addr.
       */
      StructField("resolves_to_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies a list of reference to one or more autonomous systems (AS)
       * that the IPv6 address belongs to.
       *
       * The objects referenced in this list MUST be of type autonomous-system.
       */
      StructField("belongs_to_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def mac_addr():StructType = {

    val fields = Array(
      /*
       * Specifies the value of a single MAC address. The MAC address value MUST be
       * represented as a single colon-delimited, lowercase MAC-48 address, which MUST
       * include leading zeros for each octet.
       *
       * Example: 00:00:ab:cd:ef:01
       */
      StructField("value", StringType, nullable = false)
    )

    StructType(fields)

  }

  def mutex():StructType = {

    val fields = Array(
      /*
       * Specifies the name of the mutex object.
       */
      StructField("name", StringType, nullable = false)
    )

    StructType(fields)

  }

  def network_traffic():StructType = {

    val fields = Array(
      /*
       * The Network Traffic object defines the following extensions. In addition to these,
       * producers MAY create their own.
       *
       * http-request-ext, tcp-ext, icmp-ext, socket-ext
       *
       * Dictionary keys MUST identify the extension type by name. The corresponding dictionary
       * values MUST contain the contents of the extension instance.
       */
      extensions,
      /*
       * Specifies the date/time the network traffic was initiated, if known.
       */
      StructField("start", TimestampType, nullable = true),
      /*
       * Specifies the date/time the network traffic ended, if known. If the is_active property
       * is true, then the end property MUST NOT be included. If start and end are both defined,
       * then end MUST be later than the start value.
       */
      StructField("end", TimestampType, nullable = true),
      /*
       * Indicates whether the network traffic is still ongoing. If the end property is provided,
       * this property MUST be false.
       */
      StructField("is_active", BooleanType, nullable = true),
      /*
       * Specifies the source of the network traffic, as a reference to a Cyber-observable Object.
       * The object referenced MUST be of type ipv4-addr, ipv6-addr, mac-addr, or domain-name (for
       * cases where the IP address for a domain name is unknown).
       */
      StructField("src_ref", StringType, nullable = true),
      /*
       * Specifies the destination of the network traffic, as a reference to a Cyber-observable Object.
       * The object referenced MUST be of type ipv4-addr, ipv6-addr, mac-addr, or domain-name (for cases
       * where the IP address for a domain name is unknown).
       */
      StructField("dst_ref", StringType, nullable = true),
      /*
       * Specifies the source port used in the network traffic, as an integer. The port value MUST be
       * in the range of 0 - 65535.
       */
      StructField("src_port", IntegerType, nullable = true),
      /*
       * Specifies the destination port used in the network traffic, as an integer. The port value MUST
       *  be in the range of 0 - 65535.
       */
      StructField("dst_port", IntegerType, nullable = true),
      /*
       * Specifies the protocols observed in the network traffic, along with their corresponding state.
       * Protocols MUST be listed in low to high order, from outer to inner in terms of packet encapsulation.
       *
       * That is, the protocols in the outer level of the packet, such as IP, MUST be listed first.
       *
       * The protocol names SHOULD come from the service names defined in the Service Name column of the IANA
       * Service Name and Port Number Registry [Port Numbers]. In cases where there is variance in the name of
       * a network protocol not included in the IANA Registry, content producers should exercise their best
       * judgement, and it is recommended that lowercase names be used for consistency with the IANA registry.
       *
       * Examples:
       * ipv4, tcp, http
       * ipv4, udp
       * ipv6, tcp, http
       * ipv6, tcp, ssl, https
       */
      StructField("protocols", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies the number of bytes, as a positive integer, sent from the source to the destination.
       */
      StructField("src_byte_count", LongType, nullable = true),
      /*
       * Specifies the number of bytes, as a positive integer, sent from the destination to the source.
       */
      StructField("dst_byte_count", LongType, nullable = true),
      /*
       * Specifies the number of packets, as a positive integer, sent from the source to the destination.
       */
      StructField("src_packets", LongType, nullable = true),
      /*
       * Specifies the number of packets, as a positive integer, sent from the destination to the source.
       */
      StructField("dst_packets", LongType, nullable = true),
      /*
       * Specifies any IP Flow Information Export [IPFIX] data for the traffic, as a dictionary. Each key/value
       * pair in the dictionary represents the name/value of a single IPFIX element. Accordingly, each dictionary
       * key SHOULD be a case-preserved version of the IPFIX element name, e.g., octetDeltaCount.
       *
       * Each dictionary value MUST be either an integer or a string, as well as a valid IPFIX property.
       *
       * IMPORTANT: Different from the specification, this field unifies dictionary fields to STRING.
       */
      StructField("ipfix", MapType(StringType, StringType), nullable = true),
      /*
       * Specifies the bytes sent from the source to the destination.
       * The object referenced in this property MUST be of type artifact.
       */
      StructField("src_payload_ref", StringType, nullable = true),
      /*
       * Specifies the bytes sent from the destination to the source.
       * The object referenced in this property MUST be of type artifact.
       */
      StructField("dst_payload_ref", StringType, nullable = true),
      /*
       * Links to other network-traffic objects encapsulated by this network-traffic object.
       * The objects referenced in this property MUST be of type network-traffic.
       */
      StructField("encapsulates_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Links to another network-traffic object which encapsulates this object.
       * The object referenced in this property MUST be of type network-traffic.
       */
      StructField("encapsulated_by_ref", StringType, nullable = true)
    )

    StructType(fields)

  }

  def process():StructType = {

    val fields = Array(
      /*
       * The Process object defines the following extensions. In addition to these, producers
       * MAY create their own.
       *
       * windows-process-ext, windows-service-ext
       *
       * Dictionary keys MUST identify the extension type by name. The corresponding dictionary
       * values MUST contain the contents of the extension instance.
       */
      extensions,
      /*
       * Specifies whether the process is hidden.
       */
      StructField("is_hidden", BooleanType, nullable = true),
      /*
       * Specifies the Process ID, or PID, of the process.
       */
      StructField("pid", LongType, nullable = true),
      /*
       * Specifies the date/time at which the process was created.
       */
      StructField("created_type", TimestampType, nullable = true),
      /*
       * Specifies the current working directory of the process.
       */
      StructField("cwd", StringType, nullable = true),
      /*
       * Specifies the full command line used in executing the process, including the
       * process name (which may be specified individually via the image_ref.name property)
       * and any arguments.
       */
      StructField("command_line", StringType, nullable = true),
      /*
       * Specifies the list of environment variables associated with the process as a dictionary.
       * Each key in the dictionary MUST be a case preserved version of the name of the environment
       * variable, and each corresponding value MUST be the environment variable value as a string.
       */
      StructField("environment_variables", MapType[String,String], nullable = false),
      /*
       * Specifies the list of network connections opened by the process, as a reference to one or
       * more Network Traffic objects.
       * The objects referenced in this list MUST be of type network-traffic.
       */
      StructField("opened_connection_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies the user that created the process, as a reference to a User Account object.
       * The object referenced in this property MUST be of type user-account.
       */
      StructField("creator_user_ref", StringType, nullable = true),
      /*
       * Specifies the executable binary that was executed as the process image, as a reference to a
       * File object. The object referenced in this property MUST be of type file.
       */
      StructField("image_ref", StringType, nullable = true),
      /*
       * Specifies the other process that spawned (i.e. is the parent of) this one, as a reference to
       * a Process object. The object referenced in this property MUST be of type process.
       *
       * NOTE: The reference describes the process object identifier and is not the same as the process id.
       */
      StructField("parent_ref", StringType, nullable = true),
      /*
       * Specifies the other processes that were spawned by (i.e. children of) this process, as a reference
       * to one or more other Process objects.
       *
       * The objects referenced in this list MUST be of type process.
       */
      StructField("child_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def software():StructType = {

    val fields = Array(
      /*
       * Specifies the name of the software.
       */
      StructField("name", StringType, nullable = false),
      /*
       * Specifies the Common Platform Enumeration (CPE) entry for the software, if available.
       * The value for this property MUST be a CPE v2.3 entry from the official NVD CPE Dictionary
       * [NVD]. While the CPE dictionary does not contain entries for all software, whenever it does
       * contain an identifier for a given instance of software, this property SHOULD be present.
       */
      StructField("cpe", StringType, nullable = true),
      /*
       * Specifies the languages supported by the software. The value of each list member MUST be an
       * ISO 639-2 language code [ISO639-2].
       */
      StructField("languages", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies the name of the vendor of the software.
       */
      StructField("vendor", StringType, nullable = true),
      /*
       * Specifies the version of the software.
       */
      StructField("version", StringType, nullable = true)
    )

    StructType(fields)

  }

  def url():StructType = {

    val fields = Array(
      /*
       * Specifies the value of the URL. The value of this property MUST conform to [RFC3986],
       * more specifically section 1.1.3 with reference to the definition for "Uniform Resource
       * Locator".
       */
      StructField("value", StringType, nullable = false)
    )

    StructType(fields)

  }

  def user_account():StructType = {

    val fields = Array(
      /*
       * The User Account object defines the following extensions. In addition to these, producers
       * MAY create their own.
       *
       * unix-account-ext
       *
       * Dictionary keys MUST identify the extension type by name. The corresponding dictionary values
       * MUST contain the contents of the extension instance.
       */
      extensions,
      /*
       * Specifies the identifier of the account. The format of the identifier depends on the system the
       * user account is maintained in, and may be a numeric ID, a GUID, an account name, an email address,
       * etc. The user_id property should be populated with whatever field is the unique identifier for the
       * system the account is a member of. For example, on UNIX systems it would be populated with the UID.
       */
      StructField("user_id", StringType, nullable = true),
      /*
       * Specifies a cleartext credential. This is only intended to be used in capturing metadata from malware
       * analysis (e.g., a hard-coded domain administrator password that the malware attempts to use for lateral
       * movement) and SHOULD NOT be used for sharing of PII.
       */
      StructField("credential", StringType, nullable = true),
      /*
       * Specifies the account login string, used in cases where the user_id property specifies something other
       * than what a user would type when they login. For example, in the case of a Unix account with user_id 0,
       * the account_login might be “root”.
       */
      StructField("account_login", StringType, nullable = true),
      /*
       * Specifies the type of the account. This is an open vocabulary and values SHOULD come from the account-type-ov
       * vocabulary.
       */
      StructField("account_type", StringType, nullable = true),
      /*
       * Specifies the display name of the account, to be shown in user interfaces, if applicable. On Unix, this is
       * equivalent to the GECOS field.
       */
      StructField("display_name", StringType, nullable = true),
      /*
       * Indicates that the account is associated with a network service or system process (daemon), not a specific
       * individual.
       */
      StructField("is_service_account", BooleanType, nullable = true),
      /*
       * Specifies that the account has elevated privileges (i.e., in the case of root on Unix or the Windows
       * Administrator account).
       */
      StructField("is_privileged", BooleanType, nullable = true),
      /*
       * Specifies that the account has the ability to escalate privileges (i.e., in the case of sudo on Unix or
       * a Windows Domain Admin account)
       */
      StructField("can_escalate_privs", BooleanType, nullable = true),
      /*
       * Specifies when the account was created.
       */
      StructField("account_created", TimestampType, nullable = true),
      /*
       * Specifies the expiration date of the account.
       */
      StructField("account_expires", TimestampType, nullable = true),
      /*
       * Specifies when the account credential was last changed.
       */
      StructField("credential_last_changed", TimestampType, nullable = true),
      /*
       * Specifies when the account was first accessed.
       */
      StructField("account_first_login", TimestampType, nullable = true),
      /*
       * Specifies when the account was last accessed.
       */
      StructField("account_last_login", TimestampType, nullable = true)
    )

    StructType(fields)

  }

  def windows_registry_key():StructType = {

    val fields = Array(
      /*
       * Specifies the full registry key including the hive. The value of the key, including
       * the hive portion, SHOULD be case-preserved. The hive portion of the key MUST be fully
       * expanded and not truncated; e.g., HKEY_LOCAL_MACHINE must be used instead of HKLM.
       */
      StructField("key", StringType, nullable = true),
      /*
       * Specifies the values found under the registry key.
       */
      StructField("values", ArrayType(WindowsRegistryValueType, containsNull = false), nullable = true),
      /*
       * Specifies the last date/time that the registry key was modified.
       */
      StructField("modified_time", TimestampType, nullable = true),
      /*
       * Specifies a reference to the user account that created the registry key. The object
       * referenced in this property MUST be of type user-account.
       */
      StructField("creator_user_ref", StringType, nullable = true),
      /*
       * Specifies the number of subkeys contained under the registry key.
       */
      StructField("number_of_subkeys", IntegerType, nullable = true)
    )

    StructType(fields)

  }

  def x509_certificate():StructType = {

    val fields = Array(
      /*
       * Specifies whether the certificate is self-signed, i.e., whether it is signed by the same entity
       * whose identity it certifies.
       */
      StructField("is_self_signed", BooleanType, nullable = true),
      /*
       * Specifies any hashes that were calculated for the entire contents of the certificate.
       * Dictionary keys MUST come from the hash-algorithm-ov.
       */
      hashes,
      /*
       * Specifies the version of the encoded certificate.
       */
      StructField("version", StringType, nullable = true),
      /*
       * Specifies the unique identifier for the certificate, as issued by a specific Certificate
       * Authority.
       */
      StructField("serial_number", StringType, nullable = true),
      /*
       * Specifies the name of the algorithm used to sign the certificate.
       */
      StructField("signature_algorithm", StringType, nullable = true),
      /*
       * Specifies the name of the Certificate Authority that issued the certificate.
       */
      StructField("issuer", StringType, nullable = true),
      /*
       * Specifies the date on which the certificate validity period begins.
       */
      StructField("validity_not_before", TimestampType, nullable = true),
      /*
       * Specifies the date on which the certificate validity period ends.
       */
      StructField("validity_not_after", TimestampType, nullable = true),
      /*
       * Specifies the name of the entity associated with the public key stored in the subject
       * public key field of the certificate.
       */
      StructField("subject", StringType, nullable = true),
      /*
       * Specifies the name of the algorithm with which to encrypt data being sent to the subject.
       */
      StructField("subject_public_key_algorithm", StringType, nullable = true),
      /*
       * Specifies the modulus portion of the subject’s public RSA key.
       */
      StructField("subject_public_key_modulus", StringType, nullable = true),
      /*
       * Specifies the exponent portion of the subject’s public RSA key, as an integer.
       */
      StructField("subject_public_key_exponent", IntegerType, nullable = true),
      /*
       *
       */
      StructField("x509_v3_extensions", X509V3ExtensionsType, nullable = true)
    )

    StructType(fields)

  }

}
