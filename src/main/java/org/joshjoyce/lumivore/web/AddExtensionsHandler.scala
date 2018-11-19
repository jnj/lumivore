package org.joshjoyce.lumivore.web

import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.util.LumivoreLogging
import org.webbitserver.{HttpControl, HttpHandler, HttpRequest, HttpResponse}

class AddExtensionsHandler(database: SqliteDatabase) extends HttpHandler with WebbitSupport with LumivoreLogging {
  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    Option(request.queryParam("extensionsToAdd")).map {
      s => {
        val exts = s.split( """\s+""")
        val newExts = exts.toSet.diff(database.getExtensions.toSet)
        log.debug("adding extensions: " + newExts.mkString(", "))
        newExts.foreach {
          e => database.addExtension(e)
        }
      }
    }
    redirect("/")(request, response, control)
  }
}
