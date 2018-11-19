package org.joshjoyce.lumivore.web

import java.nio.file.Paths

import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.util.LumivoreLogging
import org.webbitserver.{HttpControl, HttpHandler, HttpRequest, HttpResponse}

class AddWatchDirHandler(database: SqliteDatabase) extends HttpHandler with WebbitSupport with LumivoreLogging {
  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    Option(request.queryParam("watchDir")).map {
      s => {
        if (!database.getWatchedDirectories.exists {d => d.toLowerCase == s.toLowerCase}) {
          database.addWatchedDirectory(Paths.get(s))
        }
      }
    }
    redirect("/")(request, response, control)
  }
}
