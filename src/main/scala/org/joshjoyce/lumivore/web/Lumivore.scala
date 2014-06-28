package org.joshjoyce.lumivore.web

import org.scalatra.ScalatraFilter
import org.scalatra.scalate.ScalateSupport
import org.joshjoyce.lumivore.db.SqliteDatabase

class Lumivore extends ScalatraFilter with ScalateSupport {
  val database = new SqliteDatabase
  database.connect()

  get("/") {
    contentType = "text/html"
    val records = database.queryPhotos().map(_.toString)
    scaml("/index.scaml", "records" -> records)
  }

}
