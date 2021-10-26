package de.kp.works.stream.sql.transform.stix

object StixObjects extends Enumeration {

  type StixObject = Value

  val ATTACK_PATTERN: StixObject     = Value(1, "attack-pattern")
  val CAMPAIGN: StixObject           = Value(2, "campaign")
  val COURSE_OF_ACTION: StixObject   = Value(3, "course-of-action")
  val GROUPING:StixObject            = Value(4, "grouping")
  val INDICATOR: StixObject          = Value(5, "indicator")
  val IDENTITY: StixObject           = Value(6, "identity")
  val INFRASTRUCTURE:StixObject      = Value(7, "infrastructure")
  val INTRUSION_SET: StixObject      = Value(8, "intrusion-set")
  val LOCATION: StixObject           = Value(9, "location")
  val MALWARE: StixObject            = Value(10, "malware")
  val MALWARE_ANALYSIS: StixObject   = Value(11, "malware-analysis")
  val NOTE: StixObject               = Value(12, "note")
  val OBSERVED_DATA: StixObject      = Value(13, "observed-data")
  val OPINION: StixObject            = Value(14, "opinion")
  val REPORT: StixObject             = Value(15, "report")
  val THREAT_ACTOR: StixObject       = Value(16, "threat-actor")
  val TOOL: StixObject               = Value(17, "tool")
  val VULNERABILITY: StixObject      = Value(18, "vulnerability")
  /*
   * Relationship objects
   */
  val RELATIONSHIP: StixObject       = Value(19, "relationship")
  val SIGHTING: StixObject           = Value(20, "sighting")
  /*
   * Meta objects
   */
  val MARKING_DEFINITION: StixObject = Value(21, "marking-definition")
  val LANGUAGE_CONTENT: StixObject   = Value(22, "language-content")

}

