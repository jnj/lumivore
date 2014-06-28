package org.joshjoyce.lumivore.web

import org.scalatra.ScalatraFilter
import org.scalatra.scalate.ScalateSupport

class Lumivore extends ScalatraFilter with ScalateSupport {

  get("/") {
    contentType = "text/html"
    scaml("/index.scaml")
  }

}
