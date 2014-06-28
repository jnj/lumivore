package org.joshjoyce.lumivore.web

import org.scalatra.ScalatraFilter

class Lumivore extends ScalatraFilter {

  get("/") {
    <h1>Hi there!</h1>
  }

}
