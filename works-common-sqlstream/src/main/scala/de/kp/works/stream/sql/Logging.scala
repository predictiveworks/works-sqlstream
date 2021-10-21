package de.kp.works.stream.sql

import org.slf4j.LoggerFactory

trait Logging {
  final val log = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
}
