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

  /*
   * Specifies any extensions of the object, as a dictionary. Dictionary keys MUST identify
   * the extension type by name. The corresponding dictionary values MUST contain the contents
   * of the extension instance. This property MUST NOT be used on any STIX Objects other than
   * SCOs.
   */
  val extensions: StructField =
    StructField("extensions", MapType(StringType, StringType), nullable = true)

  val hashes: StructField =
    StructField("hashes", MapType(StringType, StringType), nullable = true)

  val ExternalReferenceType:StructType = {

    val fields = Array(
      /*
       * The name of the source that the external-reference is defined within
       * (system, registry, organization, etc.).
       */
      StructField("source_name", StringType, nullable = false),
      /*
       * A human readable description.
       */
      StructField("description", StringType, nullable = false),
      /*
       * A URL reference to an external resource [RFC3986].
       */
      StructField("url", StringType, nullable = false),
      /*
       * Specifies a dictionary of hashes for the contents of the url. This SHOULD be provided
       * when the url property is present. Dictionary keys MUST come from the hash-algorithm-ov.
       *
       * As stated in Section 2.7, to ensure interoperability, a SHA-256 hash SHOULD be included
       * whenever possible.
       */
      hashes,
      /*
       * An identifier for the external reference content.
       */
      StructField("external_id", StringType, nullable = false)
    )

    StructType(fields)

  }

  val GranularMarkingType:StructType = {

    val fields = Array(
      /*
       * The lang property identifies the language of the text identified by this marking. The value
       * of the lang property, if present, MUST be an [RFC5646] language code.
       *
       * If the marking_ref property is not present, this property MUST be present. If the marking_ref
       * property is present, this property MUST NOT be present.
       */
      StructField("lang", StringType, nullable = true),
      /*
       * The marking_ref property specifies the ID of the marking-definition object that describes the
       * marking. If the lang property is not present, this property MUST be present. If the lang property
       * is present, this property MUST NOT be present.
       */
      StructField("marking_ref", StringType, nullable = true),
      /*
       * The selectors property specifies a list of selectors for content contained within the STIX Object
       * in which this property appears. Selectors MUST conform to the syntax defined below.
       *
       * The marking-definition referenced in the marking_ref property is applied to the content selected
       * by the selectors in this list.
       *
       * The [RFC5646] language code specified by the lang property is applied to the content selected by
       * the selectors in this list.
       */
      StructField("selectors", ArrayType(StringType, containsNull = false), nullable = false)
    )
    StructType(fields)

  }

  val KillChainPhaseType:StructType = {

    val fields = Array(
      /*
       * The name of the kill chain. The value of this property SHOULD be all lowercase and SHOULD
       * use hyphens instead of spaces or underscores as word separators.
       */
      StructField("kill_chain_name", StringType, nullable = false),
      /*
       * The name of the phase in the kill chain. The value of this property SHOULD be all lowercase
       * and SHOULD use hyphens instead of spaces or underscores as word separators.
       */
      StructField("phase_name", StringType, nullable = false)

    )

    StructType(fields)

  }

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

  /******/

 /*
  * The created_by_ref property specifies the id property of the identity object
  * that describes the entity that created this object. If this attribute is omitted,
  * the source of this information is undefined. This may be used by object creators
  * who wish to remain anonymous.
  */
  val created_by_ref: StructField =
    StructField("created_by_ref", StringType, nullable = true)
  /*
   * The version of the STIX specification used to represent this object. The value of
   * this property MUST be 2.1 for STIX Objects defined according to this specification.
   * If objects are found where this property is not present, the implicit value for all
   * STIX Objects other than SCOs is 2.0. Since SCOs are now top-level objects in STIX 2.1,
   * the default value for SCOs is 2.1.
   */
  val spec_version_sdo: StructField =
    StructField("spec_version", StringType, nullable = false)

  val spec_version_sro: StructField =
    StructField("spec_version", StringType, nullable = false)

  val spec_version_sco: StructField =
    StructField("spec_version", StringType, nullable = true)
  /*
   * The created property represents the time at which the object was originally created.
   * The object creator can use the time it deems most appropriate as the time the object
   * was created, but it MUST be precise to the nearest millisecond (exactly three digits
   * after the decimal place in seconds).
   *
   * The created property MUST NOT be changed when creating a new version of the object.
   * See section 3.6 for further definition of versioning.
   */
  val created: StructField =
    StructField("created", TimestampType, nullable = false)
  /*
   * The modified property is only used by STIX Objects that support versioning and represents
   * the time that this particular version of the object was last modified. The object creator
   * can use the time it deems most appropriate as the time this version of the object was modified,
   * but it must be precise to the nearest millisecond (exactly three digits after the decimal place
   * in seconds).
   *
   * If the created property is defined, then the value of the modified property for a given object
   * version MUST be later than or equal to the value of the created property.
   *
   * Object creators MUST set the modified property when creating a new version of an object if the
   * created property was set.
   *
   * See section 3.6 for further definition of versioning.
   */
  val modified: StructField =
    StructField("modified", TimestampType, nullable = false)
  /*
   * The revoked property is only used by STIX Objects that support versioning and indicates whether
   * the object has been revoked. Revoked objects are no longer considered valid by the object creator.
   * Revoking an object is permanent; future versions of the object with this id MUST NOT be created.
   *
   * The default value of this property is false. See section 3.6 for further definition of versioning.
   */
  val revoked: StructField =
    StructField("revoked", BooleanType, nullable = true)
  /*
   * The labels property specifies a set of terms used to describe this object. The terms are user-defined
   * or trust-group defined and their meaning is outside the scope of this specification and MAY be ignored.
   *
   * Where an object has a specific property defined in the specification for characterizing subtypes of that
   * object, the labels property MUST NOT be used for that purpose.
   *
   * For example, the Malware SDO has a property malware_types that contains a list of Malware subtypes
   * (dropper, RAT, etc.). In this example, the labels property cannot be used to describe these Malware
   * subtypes.
   */
  val labels: StructField =
    StructField("labels", ArrayType(StringType, containsNull = false), nullable = true)
  /*
   * The confidence property identifies the confidence that the creator has in the correctness of their data.
   * The confidence value MUST be a number in the range of 0-100. Appendix A contains a table of normative
   * mappings to other confidence scales that MUST be used when presenting the confidence value in one of
   * those scales. If the confidence property is not present, then the confidence of the content is unspecified.
   */
  val confidence: StructField =
    StructField("confidence", IntegerType, nullable = true)
  /*
   * The lang property identifies the language of the text content in this object. When present, it MUST be a
   * language code conformant to [RFC5646]. If the property is not present, then the language of the content
   * is en (English). This property SHOULD be present if the object type contains translatable text properties
   * (e.g. name, description). The language of individual fields in this object MAY be overridden by the lang
   * property in granular markings (see section 7.2.3).
   */
  val lang: StructField =
    StructField("lang", StringType, nullable = true)
  /*
   * The external_references property specifies a list of external references which refers to non-STIX information.
   * This property is used to provide one or more URLs, descriptions, or IDs to records in other systems.
   */
  val external_references: StructField =
    StructField("external_references", ArrayType(ExternalReferenceType, containsNull = false), nullable = true)
  /*
   * The object_marking_refs property specifies a list of id properties of marking-definition objects that apply
   * to this object. In some cases, though uncommon, marking definitions themselves may be marked with sharing or
   * handling guidance. In this case, this property MUST NOT contain any references to the same Marking Definition
   * object (i.e., it cannot contain any circular references). See section 7.2 for further definition of data markings.
   */
  val object_marking_refs: StructField =
    StructField("object_marking_refs", ArrayType(StringType, containsNull = false), nullable = true)
  /*
   * The granular_markings property specifies a list of granular markings applied to this object. In some cases,
   * though uncommon, marking definitions themselves may be marked with sharing or handling guidance.
   *
   * In this case, this property MUST NOT contain any references to the same Marking Definition object (i.e., it cannot
   * contain any circular references). See section 7.2 for further definition of data markings.
   */
  val granular_markings: StructField =
    StructField("granular_markings", ArrayType(GranularMarkingType, containsNull = false), nullable = true)
  /*
   * This property defines whether or not the data contained within the object has been defanged. The default value
   * for this property is false. This property MUST NOT be used on any STIX Objects other than SCOs.
   */
  val defanged: StructField =
    StructField("defanged", BooleanType, nullable = true)

//
//  extensions
//
////  labels, granular_markings
  /**
   * CYBER OBSERVABLES
   *
   * The schema definitions below do not contain the common identifier field
   * for the respective. It is assigned to the each schema before being provided.
   */

  def fromObservableName(name:String):StructType = {

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
      StructField("environment_variables", MapType(StringType,StringType, valueContainsNull = false), nullable = false),
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

  /** STIX OBJECTS **/

  def common_schema:StructType = {

    val fields = Array.empty[StructField]
    StructType(fields)

  }

  def attack_pattern:StructType = {

    val fields = Array(
      /*
       * A list of external references which refer to non-STIX information. This property MAY
       * be used to provide one or more Attack Pattern identifiers, such as a CAPEC ID.
       *
       * When specifying a CAPEC ID, the source_name property of the external reference MUST
       * be set to capec and the external_id property MUST be formatted as CAPEC-[id].
       */
      StructField("external_references", ArrayType(ExternalReferenceType, containsNull = false), nullable = true),
      /*
       * A name used to identify the Attack Pattern.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Attack Pattern,
       * potentially including its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = false),
      /*
       * Alternative names used to identify this Attack Pattern.
       */
      StructField("aliases", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       * The list of Kill Chain Phases for which this Attack Pattern is used.
       */
      StructField("kill_chain_phases", ArrayType(KillChainPhaseType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def campaign:StructType = {

    val fields = Array(
      /*
       * A name used to identify the Campaign.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Campaign,
       * potentially including its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * Alternative names used to identify this Campaign
       */
      StructField("aliases", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       * The time that this Campaign was first seen. This property is a summary property of data
       * from sightings and other data that may or may not be available in STIX. If new sightings
       * are received that are earlier than the first seen timestamp, the object may be updated
       * to account for the new data.
       */
      StructField("first_seen", TimestampType, nullable = true),
      /*
       * The time that this Campaign was last seen. This property is a summary property of data
       * from sightings and other data that may or may not be available in STIX. If new sightings
       * are received that are later than the last seen timestamp, the object may be updated to
       * account for the new data.
       *
       * This MUST be greater than or equal to the timestamp in the first_seen property.
       */
      StructField("last_seen", TimestampType, nullable = true),
      /*
       * This property defines the Campaign’s primary goal, objective, desired outcome, or intended
       * effect — what the Threat Actor or Intrusion Set hopes to accomplish with this Campaign.
       */
      StructField("objective", StringType, nullable = true)
    )

    StructType(fields)

  }

  def course_of_action:StructType = {

    val fields = Array(
      /*
       * A name used to identify the Course of Action.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Course of Action, potentially
       * including its purpose and its key characteristics. In some cases, this property may contain
       * A description that provides more details and context about the Course of Action, potentially
       * including its purpose and its key characteristics.
       *
       * In some cases, this property may contain the actual course of action in prose text. The actual
       * course of action in prose text.
       */
      StructField("description", StringType, nullable = true),
      /*
       * The type of action that is included in either the action_bin property or the de-referenced
       * content from the action_reference property. For example: textual:text/plain
       *
       * This is an open vocabulary and values SHOULD come from the course-of-action-type-ov vocabulary.
       */
      StructField("action_type", StringType, nullable = true),
      /*
       * A recommendation on the operating system(s) that this course of action can be applied to.
       * If no os_execution_envs are defined, the operating systems for the action specified by the
       * action_type property are undefined, or the specific operating system has no impact on the
       * execution of the course of action (e.g., power off system).
       *
       * Each string value for this property SHOULD be a CPE v2.3 entry from the official NVD CPE
       * Dictionary [NVD]. This property MAY include custom values including values taken from other
       * standards such as SWID [SWID].
       *
       * Example:
       *
       * [
       * cpe:2.3:o:microsoft:windows_10:*:*:*:*:*:*:x86:*,
       * cpe:2.3:o:microsoft:windows_10:*:*:*:*:*:*:x64:*
       * ]
       * This example means that any version of the Windows 10 operating system is able to process
       * and use the course of action defined in the action_bin or action_reference properties.
       */
      StructField("os_execution_envs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * This property contains the base64 encoded "commands" that represent the action for this
       * Course of Action. This property MUST NOT be present if action_reference is provided.
       */
      StructField("action_bin", StringType, nullable = true),
      /*
       * The value of this property MUST be a valid external reference that resolves to the action
       * content as defined by the action_type property. This property MUST NOT be present if action_bin
       * is provided.
       */
      StructField("action_reference", ExternalReferenceType, nullable = true)
    )

    StructType(fields)

  }

  def grouping:StructType = {

    val fields = Array(
      /*
       * A name used to identify the Grouping.
       */
      StructField("name", StringType, nullable = true),
      /*
       * A description that provides more details and context about the Grouping, potentially
       * including its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * A short descriptor of the particular context shared by the content referenced by the Grouping.
       * This is an open vocabulary and values SHOULD come from the grouping-context-ov vocabulary.
       */
      StructField("context", StringType, nullable = false),
      /*
       * Specifies the STIX Objects that are referred to by this Grouping.
       */
      StructField("object_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def identity:StructType = {

    val fields = Array(
      /*
       * The name of this Identity. When referring to a specific entity (e.g., an individual or organization),
       * this property SHOULD contain the canonical name of the specific entity.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Identity, potentially including its
       * purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * The list of roles that this Identity performs (e.g., CEO, Domain Administrators, Doctors, Hospital, or
       * Retailer). No open vocabulary is yet defined for this property.
       */
      StructField("roles", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The type of entity that this Identity describes, e.g., an individual or organization.
       * This is an open vocabulary and the values SHOULD come from the identity-class-ov vocabulary.
       */
      StructField("identity_class", StringType, nullable = false),
      /*
       * The list of industry sectors that this Identity belongs to. This is an open vocabulary and values SHOULD
       * come from the industry-sector-ov vocabulary.
       */
      StructField("sectors", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The contact information (e-mail, phone number, etc.) for this Identity. No format for this information
       * is currently defined by this specification.
       */
      StructField("contact_information", StringType, nullable = true)
    )

    StructType(fields)

  }

  def indicator:StructType = {

    val fields = Array(
      /*
       * A name used to identify the Indicator. Producers SHOULD provide this property to help products
       * and analysts understand what this Indicator actually does.
       */
      StructField("name", StringType, nullable = true),
      /*
       * A description that provides more details and context about the Indicator, potentially including
       * its purpose and its key characteristics. Producers SHOULD provide this property to help products
       * and analysts understand what this Indicator actually does.
       */
      StructField("description", StringType, nullable = true),
      /*
       * This property is an open vocabulary that specifies a set of categorizations for this indicator.
       * This is an open vocabulary and values SHOULD come from the indicator-type-ov vocabulary.
       */
      StructField("indicator_types", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       * The detection pattern for this Indicator is a STIX Pattern as specified in section 9.
       */
      StructField("pattern", StringType, nullable = false),
      /*
       * The type of pattern used in this indicator. The property is an open vocabulary and currently
       * has the values of stix, snort, and yara.
       */
      StructField("pattern_type", StringType, nullable = false),
      /*
       * The version of the pattern that is used. For patterns that do not have a formal specification,
       * the build or code version that the pattern is known to work with SHOULD be used.
       */
      StructField("pattern_version", StringType, nullable = true),
      /*
       * The time from which this Indicator is considered a valid indicator of the behaviors it is
       * related or represents.
       */
      StructField("valid_from", TimestampType, nullable = false),
      /*
       * The time at which this Indicator should no longer considered a valid indicator of the behaviors
       * it is related to or represents. If the valid_until property is omitted, then there is no constraint
       * on the latest time for which the Indicator is valid. This MUST be greater than the timestamp in the
       * valid_from property.
       */
      StructField("valid_until", TimestampType, nullable = true),
      /*
       * The kill chain phase(s) to which this Indicator corresponds.
       */
      StructField("kill_chain_phases", ArrayType(KillChainPhaseType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def infrastructure:StructType = {

    val fields = Array(
      /*
       * A name or characterizing text used to identify the Infrastructure.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Infrastructure, potentially
       * including its purpose, how it is being used, how it relates to other intelligence activities
       * captured in related objects, and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * The type of infrastructure being described. This is an open vocabulary and values SHOULD come
       * from the infrastructure-type-ov vocabulary.
       */
      StructField("infrastructure_type", StringType, nullable = false),
      /*
       * Alternative names used to identify this Infrastructure.
       */
      StructField("aliases", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The list of Kill Chain Phases for which this Infrastructure is used.
       */
      StructField("kill_chain_phases", ArrayType(KillChainPhaseType, containsNull = false), nullable = true),
      /*
       * The time that this Infrastructure was first seen performing malicious activities.
       */
      StructField("first_seen", TimestampType, nullable = true),
      /*
       * The time that this Infrastructure was last seen performing malicious activities. If this property
       * and the first_seen property are both defined, then this property MUST be greater than or equal to
       * the timestamp in the first_seen property..
       */
      StructField("last_seen", TimestampType, nullable = true)
    )

    StructType(fields)

  }

  def intrusion_set:StructType = {

    val fields = Array(
      /*
       * A name used to identify this Intrusion Set.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Intrusion Set, potentially
       * including its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * Alternative names used to identify this Intrusion Set.
       */
      StructField("aliases", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The time that this Intrusion Set was first seen. This property is a summary property of data from
       * sightings and other data that may or may not be available in STIX. If new sightings are received
       * that are earlier than the first seen timestamp, the object may be updated to account for the new
       * data.
       */
      StructField("first_seen", TimestampType, nullable = true),
      /*
       * The time that this Intrusion Set was last seen. This property is a summary property of data from
       * sightings and other data that may or may not be available in STIX. If new sightings are received
       * that are later than the last seen timestamp, the object may be updated to account for the new data.
       * This MUST be greater than or equal to the timestamp in the first_seen property.
       */
      StructField("last_seen", TimestampType, nullable = true),
      /*
       *The high-level goals of this Intrusion Set, namely, what are they trying to do. For example, they may
       * be motivated by personal gain, but their goal is to steal credit card numbers. To do this, they may
       * execute specific Campaigns that have detailed objectives like compromising point of sale systems at
       * a large retailer. Another example: to gain information about latest merger and IPO information from
       * ACME Bank.
       */
      StructField("goals", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * This defines the organizational level at which this Intrusion Set typically works, which in turn
       * determines the resources available to this Intrusion Set for use in an attack. This is an open
       * vocabulary and values SHOULD come from the attack-resource-level-ov  vocabulary.
       */
      StructField("resource_level", StringType, nullable = true),
      /*
       * The primary reason, motivation, or purpose behind this Intrusion Set. The motivation is why the
       * Intrusion Set wishes to achieve the goal (what they are trying to achieve). For example, an
       * Intrusion Set with a goal to disrupt the finance sector in a country might be motivated by
       * ideological hatred of capitalism. This is an open vocabulary and values SHOULD come from the
       * attack-motivation-ov vocabulary.
       */
      StructField("primary_motivation", StringType, nullable = true),
      /*
       * The secondary reasons, motivations, or purposes behind this Intrusion Set. These motivations
       * can exist as an equal or near-equal cause to the primary motivation. However, it does not replace
       * or necessarily magnify the primary motivation, but it might indicate additional context.
       *
       * The position in the list has no significance. This is an open vocabulary and values SHOULD come
       * from the attack-motivation-ov vocabulary.
       */
      StructField("secondary_motivations", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def location:StructType = {

    val fields = Array(
      /*
       * A name used to identify the Location.
       */
      StructField("name", StringType, nullable = true),
      /*
       * A textual description of the Location.
       */
      StructField("description", StringType, nullable = true),
      /*
       * The latitude of the Location in decimal degrees. Positive numbers describe latitudes
       * north of the equator, and negative numbers describe latitudes south of the equator.
       * The value of this property MUST be between -90.0 and 90.0, inclusive.
       *
       * If the longitude property is present, this property MUST be present.
       */
      StructField("latitude", DoubleType, nullable = true),
      /*
       * The longitude of the Location in decimal degrees. Positive numbers describe longitudes
       * east of the prime meridian and negative numbers describe longitudes west of the prime
       * meridian. The value of this property MUST be between -180.0 and 180.0, inclusive.
       *
       * If the latitude property is present, this property MUST be present.
       */
      StructField("longitude", DoubleType, nullable = true),
      /*
       * Defines the precision of the coordinates specified by the latitude and longitude properties.
       * This is measured in meters. The actual Location may be anywhere up to precision meters from
       * the defined point. If this property is not present, then the precision is unspecified.
       *
       * If this property is present, the latitude and longitude properties MUST be present.
       */
      StructField("precision", DoubleType, nullable = true),
      /*
       * The region that this Location describes. This property SHOULD contain a value from region-ov.
       */
      StructField("region", StringType, nullable = true),
      /*
       * The country that this Location describes. This property SHOULD contain a valid ISO 3166-1
       * ALPHA-2 Code [ISO3166-1].
       */
      StructField("country", StringType, nullable = true),
      /*
       * The state, province, or other sub-national administrative area that this Location describes.
       */
      StructField("administrative_area", StringType, nullable = true),
      /*
       * The city that this Location describes.
       */
      StructField("city", StringType, nullable = true),
      /*
       * The street address that this Location describes. This property includes all aspects or parts of the
       * street address. For example, some addresses may have multiple lines including a mail-stop or apartment
       * number.
       */
      StructField("street_address", StringType, nullable = true),
      /*
       * The postal code for this Location.
       */
      StructField("postal_code", StringType, nullable = true)
    )

    StructType(fields)

  }

  def malware:StructType = {

    val fields = Array(
      /*
       * A name used to identify the malware instance or family, as specified by the producer of the SDO.
       * For a malware family the name MUST be defined. If a name for a malware instance is not available,
       * the SHA-256 hash value or sample’s filename MAY be used instead.
       */
      StructField("name", StringType, nullable = true),
      /*
       * A description that provides more details and context about the malware instance or family,
       * potentially including its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * This property is an open vocabulary that specifies a set of categorizations for the malware being
       * described. This is an open vocabulary and values SHOULD come from the malware-type-ov vocabulary.
       */
      StructField("malware_types", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       * Whether the object represents a malware family (if true) or a malware instance (if false).
       */
      StructField("is_family", BooleanType, nullable = false),
      /*
       * Alternative names used to identify this Intrusion Set.
       */
      StructField("aliases", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The list of Kill Chain Phases for which this malware can be used.
       */
      StructField("kill_chain_phases", ArrayType(KillChainPhaseType, containsNull = false), nullable = true),
      /*
       * The time that the malware instance or family was first seen. This property is a summary property of
       * data from sightings and other data that may or may not be available in STIX. If new sightings are
       * received that are earlier than the first seen timestamp, the object may be updated to account for
       * the new data.
       */
      StructField("first_seen", TimestampType, nullable = true),
      /*
       * The time that the malware family or malware instance was last seen. This property is a summary property
       * of data from sightings and other data that may or may not be available in STIX. If new sightings are
       * received that are later than the last_seen timestamp, the object may be updated to account for the new data.
       *
       * This MUST be greater than or equal to the timestamp in the first_seen property.
       */
      StructField("last_seen", TimestampType, nullable = true),
      /*
       * The operating systems that the malware family or malware instance is executable on. Each string value for
       * this property SHOULD be a CPE v2.3 entry from the official NVD CPE Dictionary [NVD].
       *
       * This property MAY include custom values including values taken from other standards such as SWID [SWID].
       */
      StructField("os_execution_envs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The processor architectures (e.g., x86, ARM, etc.) that the malware instance or family is executable on.
       * This is an open vocabulary and values SHOULD come from the processor-architecture-ov vocabulary.
       */
      StructField("architecture_execution_envs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The programming language(s) used to implement the malware instance or family. This is an open vocabulary
       * and values SHOULD come from the implementation-language-ov vocabulary.
       */
      StructField("implementation_languages", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * Specifies any capabilities identified for the malware instance or family. This is an open vocabulary
       * and values SHOULD come from the malware-capabilities-ov vocabulary.
       */
      StructField("capabilities", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The sample_refs property specifies a list of identifiers of the SCO file or artifact objects associated
       * with this malware instance(s) or family. If is_family is false, then all samples listed in sample_refs
       * MUST refer to the same binary data.
       */
      StructField("sample_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def malware_analysis:StructType = {

    val fields = Array(
      /*
       * The name of the analysis engine or product that was used. Product names SHOULD be all
       * lowercase with words separated by a dash "-". For cases where the name of a product
       * cannot be specified, a value of "anonymized" MUST be used.
       */
      StructField("product", StringType, nullable = false),
      /*
       * The version of the analysis product that was used to perform the analysis.
       */
      StructField("version", StringType, nullable = true),
      /*
       * A description of the virtual machine environment used to host the guest operating
       * system (if applicable) that was used for the dynamic analysis of the malware instance
       * or family. If this value is not included in conjunction with the operating_system_ref
       * property, this means that the dynamic analysis may have been performed on bare metal
       * (i.e. without virtualization) or the information was redacted.
       *
       * The value of this property MUST be the identifier for a SCO software object.
       */
      StructField("host_vm_ref", StringType, nullable = true),
      /*
       * The operating system used for the dynamic analysis of the malware instance or family.
       * This applies to virtualized operating systems as well as those running on bare metal.
       * The value of this property MUST be the identifier for a SCO software object.
       */
      StructField("operating_system_ref", StringType, nullable = true),
      /*
       * Any non-standard software installed on the operating system (specified through the
       * operating-system value) used for the dynamic analysis of the malware instance or
       * family. The value of this property MUST be the identifier for a SCO software object.
       */
      StructField("installed_software_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * This property captures the named configuration of additional product configuration
       * parameters for this analysis run. For example, when a product is configured to do full
       * depth analysis of Window™ PE files. This configuration may have a named version and that
       * named version can be captured in this property. This will ensure additional runs can be
       * configured in the same way.
       */
      StructField("configuration_version", StringType, nullable = true),
      /*
       * This property captures the specific analysis modules that were used and configured in the
       * product during this analysis run. For example, configuring a product to support analysis of
       * Dridex.
       */
      StructField("modules", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The version of the analysis engine or product (including AV engines) that was used to
       * perform the analysis.
       */
      StructField("analysis_engine_version", StringType, nullable = true),
      /*
       * The version of the analysis definitions used by the analysis tool (including AV tools).
       */
      StructField("analysis_definition_version", StringType, nullable = true),
      /*
       * The date and time that the malware was first submitted for scanning or analysis. This value will
       * stay constant while the scanned date can change. For example, when Malware was submitted to a virus
       * analysis tool.
       */
      StructField("submitted", TimestampType, nullable = true),
      /*
       * The date and time that the malware analysis was initiated.
       */
      StructField("analysis_started", TimestampType, nullable = true),
      /*
       * The date and time that the malware analysis ended.
       */
      StructField("analysis_ended", TimestampType, nullable = true),
      /*
       * The classification result or name assigned to the malware instance by the AV scanner tool. If no resulting
       * context-specific classification value or name is provided by the AV scanner tool or cannot be specified,
       * then the result value SHOULD come from the malware-av-result-ov open vocabulary.
       */
      StructField("av_result", StringType, nullable = true),
      /*
       * This property contains the references to the STIX Cyber-observable Objects that were captured during the
       * analysis process.
       */
      StructField("analysis_sco_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def note:StructType = {

    val fields = Array(
      /*
       * A brief summary of the note content.
       */
      StructField("abstract", StringType, nullable = true),
      /*
       * The content of the note.
       */
      StructField("content", StringType, nullable = false),
      /*
       * The name of the author(s) of this note (e.g., the analyst(s) that created it).
       */
      StructField("authors", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The STIX Objects that the note is being applied to.
       */
      StructField("object_refs", ArrayType(StringType, containsNull = false), nullable = false)


    )

    StructType(fields)

  }

  def observed_data:StructType = {

    val fields = Array(
      /*
       * The beginning of the time window during which the data was seen.
       */
      StructField("first_observed", TimestampType, nullable = false),
      /*
       * The end of the time window during which the data was seen. This MUST be greater
       * than or equal to the timestamp in the first_observed property.
       */
      StructField("last_observed", TimestampType, nullable = false),
      /*
       * The number of times that each Cyber-observable object represented in the objects
       * or object_ref property was seen. If present, this MUST be an integer between 1
       * and 999,999,999 inclusive. If the number_observed property is greater than 1, the
       * data contained in the objects or object_refs property was seen multiple times.
       *
       * In these cases, object creators MAY omit properties of the SCO (such as timestamps)
       * that are specific to a single instance of that observed data.
       */
      StructField("number_observed", IntegerType, nullable = true),
      /*
       * A dictionary of SCO representing the observation. The dictionary MUST contain at least
       * one object. The cyber observable content MAY include multiple objects if those objects
       * are related as part of a single observation. Multiple objects not related to each other
       * via cyber observable Relationships MUST NOT be contained within the same Observed Data
       * instance.
       *
       * This property MUST NOT be present if object_refs is provided. For example, a Network Traffic
       * object and two IPv4 Address objects related via the src_ref and dst_ref properties can be
       * contained in the same Observed Data because they are all related and used to characterize
       * that single entity.
       *
       * NOTE: this property is now deprecated in favor of object_refs and will be removed in a future
       * version.
       *
       * IMPORTANT: This field is implemented as a Map[String,String], where the value of each key/value
       * pair represents the serialized representation of a cyber observable
       */
      StructField("objects", MapType(StringType,StringType, valueContainsNull = false), nullable = true),
      /*
       * A list of SCOs and SROs representing the observation. The object_refs MUST contain at least
       * one SCO reference if defined. The object_refs MAY include multiple SCOs and their corresponding
       * SROs, if those SCOs are related as part of a single observation. For example, a Network Traffic
       * object and two IPv4 Address objects related via the src_ref and dst_ref properties can be contained
       * in the same Observed Data because they are all related and used to characterize that single entity.
       *
       * This property MUST NOT be present if objects is provided.
       */
      StructField("object_refs", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def opinion:StructType = {

    val fields = Array(
      /*
       * An explanation of why the producer has this Opinion. For example, if an Opinion of strongly-disagree is
       * given, the explanation can contain an explanation of why the Opinion producer disagrees and what evidence
       * they have for their disagreement.
       */
      StructField("explanation", StringType, nullable = true),
      /*
       * The name of the author(s) of this Opinion (e.g., the analyst(s) that created it).
       */
      StructField("authors", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       * The opinion that the producer has about all of the STIX Object(s) listed in the object_refs property.
       * The values of this property MUST come from the opinion-enum enumeration.
       */
      StructField("opinion", StringType, nullable = false),
      /*
       * The STIX Objects that the Opinion is being applied to.
       */
      StructField("object_refs", ArrayType(StringType, containsNull = false), nullable = false)
    )

    StructType(fields)

  }

  def report:StructType = {

    val fields = Array(
      /*
       * A name used to identify the Report.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Report, potentially including its
       * purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * This property is an open vocabulary that specifies the primary subject(s) of this report. This is
       * an open vocabulary and values SHOULD come from the report-type-ov vocabulary.
       */
      StructField("report_types", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       * The date that this Report object was officially published by the creator of this report.
       * The publication date (public release, legal release, etc.) may be different than the date the
       * report was created or shared internally (the date in the created property).
       */
      StructField("published", TimestampType, nullable = false),
      /*
       * Specifies the STIX Objects that are referred to by this Report.
       */
      StructField("object_refs", ArrayType(StringType, containsNull = false), nullable = false)
    )

    StructType(fields)

  }

  def threat_actor:StructType = {

    val fields = Array(
      /*
       * A name used to identify this Threat Actor or Threat Actor group.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Threat Actor,
       * potentially including its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * This property specifies the type(s) of this threat actor. This is an open
       * vocabulary and values SHOULD come from the threat-actor-type-ov vocabulary.
       */
      StructField("threat_actor_types", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       * A list of other names that this Threat Actor is believed to use.
       */
      StructField("aliases", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The time that this Threat Actor was first seen. This property is a summary property of data
       * from sightings and other data that may or may not be available in STIX. If new sightings are
       * received that are earlier than the first seen timestamp, the object may be updated to account
       * for the new data.
       */
      StructField("first_seen", TimestampType, nullable = true),
      /*
       * The time that this Threat Actor was last seen. This property is a summary property of data from
       * sightings and other data that may or may not be available in STIX. If new sightings are received
       * that are later than the last seen timestamp, the object may be updated to account for the new data.
       * This MUST be greater than or equal to the timestamp in the first_seen property.
       */
      StructField("last_seen", TimestampType, nullable = true),
      /*
       * A list of roles the Threat Actor plays. This is an open vocabulary and the values SHOULD come from
       * the threat-actor-role-ov vocabulary.
       */
      StructField("roles", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The high-level goals of this Threat Actor, namely, what are they trying to do. For example, they may
       * be motivated by personal gain, but their goal is to steal credit card numbers. To do this, they may
       * execute specific Campaigns that have detailed objectives like compromising point of sale systems at
       * a large retailer.
       */
      StructField("goals", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The skill, specific knowledge, special training, or expertise a Threat Actor must have to perform
       * the attack. This is an open vocabulary and values SHOULD come from the threat-actor-sophistication-ov
       * vocabulary.
       */
      StructField("sophistication", StringType, nullable = true),
      /*
       * This defines the organizational level at which this Threat Actor typically works, which in turn determines
       * the resources available to this Threat Actor for use in an attack. This attribute is linked to the sophistication
       * property — a specific resource level implies that the Threat Actor has access to at least a specific sophistication
       * level. This is an open vocabulary and values SHOULD come from the attack-resource-level-ov vocabulary.
       */
      StructField("resource_level", StringType, nullable = true),
      /*
       * The primary reason, motivation, or purpose behind this Threat Actor. The motivation is why the Threat Actor wishes
       * to achieve the goal (what they are trying to achieve). For example, a Threat Actor with a goal to disrupt the finance
       * sector in a country might be motivated by ideological hatred of capitalism.
       *
       * This is an open vocabulary and values SHOULD come from the attack-motivation-ov vocabulary.
       */
      StructField("primary_motivation", StringType, nullable = true),
      /*
       * The secondary reasons, motivations, or purposes behind this Threat Actor. These motivations can exist as an
       * equal or near-equal cause to the primary motivation. However, it does not replace or necessarily magnify the
       * primary motivation, but it might indicate additional context. The position in the list has no significance.
       *
       * This is an open vocabulary and values SHOULD come from the attack-motivation-ov vocabulary.
       */
      StructField("secondary_motivations", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The personal reasons, motivations, or purposes of the Threat Actor regardless of organizational goals.
       * Personal motivation, which is independent of the organization’s goals, describes what impels an individual
       * to carry out an attack. Personal motivation may align with the organization’s motivation—as is common with
       * activists—but more often it supports personal goals. For example, an individual analyst may join a Data Miner
       * corporation because his or her skills may align with the corporation’s objectives. But the analyst most likely
       * performs his or her daily work toward those objectives for personal reward in the form of a paycheck.
       *
       * The motivation of personal reward may be even stronger for Threat Actors who commit illegal acts, as it is more
       * difficult for someone to cross that line purely for altruistic reasons. The position in the list has no significance.
       * This is an open vocabulary and values SHOULD come from the attack-motivation-ov vocabulary.
       */
      StructField("personal_motivations", ArrayType(StringType, containsNull = false), nullable = true)
    )

    StructType(fields)

  }

  def tool:StructType = {

    val fields = Array(
      /*
       * The name used to identify the Tool.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Tool, potentially including
       * its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * The kind(s) of tool(s) being described. This is an open vocabulary and values SHOULD come from
       * the tool-type-ov vocabulary.
       */
      StructField("tool_types", ArrayType(StringType, containsNull = false), nullable = false),
      /*
       *Alternative names used to identify this Tool.
       */
      StructField("aliases", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The list of kill chain phases for which this Tool can be used.
       */
      StructField("kill_chain_phases", ArrayType(KillChainPhaseType, containsNull = false), nullable = true),
      /*
       * The version identifier associated with the Tool.
       */
      StructField("tool_version", StringType, nullable = true)
    )

    StructType(fields)

  }

  def vulnerability:StructType = {

    val fields = Array(
      /*
       * A list of external references which refer to non-STIX information. This property MAY be used to provide
       * one or more Vulnerability identifiers, such as a CVE ID [CVE]. When specifying a CVE ID, the source_name
       * property of the external reference MUST be set to cve and the external_id property MUST be the exact CVE
       * identifier.
       */
      StructField("external_references", ArrayType(ExternalReferenceType, containsNull = false), nullable = true),
      /*
       * A name used to identify the Vulnerability.
       */
      StructField("name", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Vulnerability, potentially including its
       * purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true)
    )

    StructType(fields)

  }

  def relationship:StructType = {

    val fields = Array(
      /*
       * The name used to identify the type of Relationship. This value SHOULD be an exact value listed in
       * the relationships for the source and target SDO, but MAY be any string. The value of this property
       * MUST be in ASCII and is limited to characters a–z (lowercase ASCII), 0–9, and hyphen (-).
       */
      StructField("relationship_type", StringType, nullable = false),
      /*
       * A description that provides more details and context about the Relationship, potentially including
       * its purpose and its key characteristics.
       */
      StructField("description", StringType, nullable = true),
      /*
       * The id of the source (from) object. The value MUST be an ID reference to an SDO or SCO (i.e., it cannot
       * point to an SRO, Bundle, Language Content,or Marking Definition).
       */
      StructField("source_ref", StringType, nullable = false),
      /*
       * The id of the target (to) object. The value MUST be an ID reference to an SDO or SCO (i.e., it cannot
       * point to an SRO, Bundle, Language Content, or Marking Definition).
       */
      StructField("target_ref", StringType, nullable = false),
      /*
       * This optional timestamp represents the earliest time at which the Relationship between the objects exists.
       * If this property is a future timestamp, at the time the start_time property is defined, then this represents
       * an estimate by the producer of the intelligence of the earliest time at which relationship will be asserted
       * to be true.
       *
       * If it is not specified, then the earliest time at which the relationship between the objects exists is not
       * defined.
       */
      StructField("start_time", TimestampType, nullable = true),
      /*
       * The latest time at which the Relationship between the objects exists. If this property is a
       * future timestamp, at the time the stop_time property is defined, then this represents an
       * estimate by the producer of the intelligence of the latest time at which relationship will be
       *  asserted to be true.
       *
       * If start_time and stop_time are both defined, then stop_time MUST be later than the start_time
       * value. If stop_time is not specified, then the latest time at which the relationship between the
       * objects exists is either not known, not disclosed, or has no defined stop time.
       */
      StructField("stop_time", TimestampType, nullable = true)
    )

    StructType(fields)

  }

  def sighting:StructType = {

    val fields = Array(
      /*
       * A description that provides more details and context about the Sighting.
       */
      StructField("description", StringType, nullable = true),
      /*
       * The beginning of the time window during which the SDO referenced by the
       * sighting_of_ref property was sighted.
       */
      StructField("first_seen", TimestampType, nullable = true),
      /*
       * The end of the time window during which the SDO referenced by the sighting_of_ref property
       * was sighted. If first_seen and last_seen are both defined, then last_seen MUST be later
       * than the first_seen value.
       */
      StructField("last_seen", TimestampType, nullable = true),
      /*
       * If present, this MUST be an integer between 0 and 999,999,999 inclusive and represents the number
       * of times the SDO referenced by the sighting_of_ref property was sighted. Observed Data has a similar
       * property called number_observed, which refers to the number of times the data was observed.
       *
       * These counts refer to different concepts and are distinct. For example, a single sighting of a DDoS
       * bot might have many millions of observations of the network traffic that it generates. Thus, the
       * Sighting count would be 1 (the bot was observed once) but the Observed Data number_observed would
       * be much higher.  As another example, a sighting with a count of 0 can be used to express that an
       * indicator was not seen at all.
       */
      StructField("count", IntegerType, nullable = true),
      /*
       * An ID reference to the SDO that was sighted (e.g., Indicator or Malware). For example, if this is a
       * Sighting of an Indicator, that Indicator’s ID would be the value of this property. This property MUST
       * reference only an SDO or a Custom Object.
       */
      StructField("sighting_of_ref", StringType, nullable = false),
      /*
       * A list of ID references to the Observed Data objects that contain the raw cyber data for this Sighting.
       * For example, a Sighting of an Indicator with an IP address could include the Observed Data for the network
       * connection that the Indicator was used to detect. This property MUST reference only Observed Data SDOs.
       */
      StructField("observed_data_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * A list of ID references to the Identity or Location objects describing the entities or types of entities
       * that saw the sighting. Omitting the where_sighted_refs property does not imply that the sighting was seen
       * by the object creator. To indicate that the sighting was seen by the object creator, an Identity representing
       * the object creator should be listed in where_sighted_refs.
       *
       * This property MUST reference only Identity or Location SDOs.
       */
      StructField("where_sighted_refs", ArrayType(StringType, containsNull = false), nullable = true),
      /*
       * The summary property indicates whether the Sighting should be considered summary data. Summary data is an
       * aggregation of previous Sightings reports and should not be considered primary source data. Default value
       * is false.
       */
      StructField("summary", BooleanType, nullable = true)

    )

    StructType(fields)

  }

  def marking_definition:StructType = {

    val fields = Array(
      /*
       * The type property identifies the type of object. The value of this property MUST be
       * marking-definition.
       */
      StructField("name", StringType, nullable = true),
      /*
       * The definition_type property identifies the type of Marking Definition. The value of the
       * definition_type property SHOULD be one of the types defined in the subsections below:
       * statement or tlp (see sections 7.2.1.3 and 7.2.1.4)
       */
      StructField("definition_type", StringType, nullable = false),
      /*
       * The definition property contains the marking object itself (e.g., the TLP marking as defined in
       * section 7.2.1.4, the Statement marking as defined in section 7.2.1.3, or some other marking
       * definition defined elsewhere).
       */
      StructField("definition",MapType(StringType, StringType), nullable = false)
    )

    StructType(fields)

  }

  def language_content:StructType = {

    val fields = Array(
      /*
       * The object_ref property identifies the id of the object that this Language Content applies to.
       * It MUST be the identifier for a STIX Object.
       */
      StructField("object_ref", StringType, nullable = false),
      /*
       * The object_modified property identifies the modified time of the object that this Language
       * Content applies to. It MUST be an exact match for the modified time of the STIX Object being
       * referenced.
       */
      StructField("object_modified", TimestampType, nullable = true),
      /*
       * The contents property contains the actual Language Content (translation). The keys in the dictionary
       * MUST be RFC 5646 language codes for which language content is being provided [RFC5646]. The values
       * each consist of a dictionary that mirrors the properties in the target object (identified by object_ref
       * and object_modified). For example, to provide a translation of the name property on the target object
       * the key in the dictionary would be name.
       *
       * For each key in the nested dictionary:
       *
       * If the original property is a string, the corresponding property in the language content object MUST
       * contain a string with the content for that property in the language of the top-level key.
       *
       * If the original property is a list, the corresponding property in the translation object must also be
       * a list. Each item in this list recursively maps to the item at the same position in the list contained
       * in the target object. The lists MUST have the same length.
       *
       * In the event that translations are only provided for some list items, the untranslated list items MUST
       * be represented by an empty string (""). This indicates to a consumer of the Language Content object that
       * they should interpolate the translated list items in the Language Content object with the corresponding
       * (untranslated) list items from the original object as indicated by the object_ref property.
       *
       * If the original property is an object (including dictionaries), the corresponding location in the translation
       * object must also be an object. Each key/value field in this object recursively maps to the object with the same
       * key in the original.
       *
       * The translation object MAY contain only a subset of the translatable fields of the original. Keys that point
       * to non-translatable properties in the target or to properties that do not exist in the target object MUST be
       * ignored.
       */
      StructField("contents", MapType(StringType, StringType), nullable = false)
    )

    StructType(fields)

  }

}
