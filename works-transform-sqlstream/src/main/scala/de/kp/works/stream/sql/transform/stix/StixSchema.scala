package de.kp.works.stream.sql.transform.stix

import org.apache.spark.sql.types._

/*
 * STIX v2.1 2019-07-26
 */
object StixSchema {

  /** DATA TYPES */

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

  /*
   * CYBER OBSERVABLES
   */
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
      StructField("hashes", MapType(StringType, StringType), nullable = true),
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
      StructField("extensions", MapType(StringType, StringType), nullable = true),
      /*
       * Specifies a dictionary of hashes for the file. (When used with the Archive
       * File Extension, this refers to the hash of the entire archive file, not its
       * contents.)
       *
       * Dictionary keys MUST come from the hash-algorithm-ov.
       */
      StructField("hashes", MapType(StringType, StringType), nullable = true),
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
}
