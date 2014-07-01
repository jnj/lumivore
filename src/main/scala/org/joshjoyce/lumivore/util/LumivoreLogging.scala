package org.joshjoyce.lumivore.util

import org.slf4j.LoggerFactory

trait LumivoreLogging {
  lazy val log = LoggerFactory.getLogger(getClass)
}
