package org.joshjoyce.lumivore.web

import org.fusesource.scalate.TemplateEngine
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.webbitserver.{HttpControl, HttpHandler, HttpRequest, HttpResponse}

class HomeHandler(templateEngine: TemplateEngine, database: SqliteDatabase) extends HttpHandler with WebbitSupport {

  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    val extensions = database.getExtensions.sorted
    val watched = database.getWatchedDirectories
    val dupes = Nil //database.getDuplicates
    val nonexistent = Nil /* database.getSyncs.filter {
      case (path, _, _) => !Files.exists(Paths.get(path))
    }.map(_._1) */
    val content = templateEngine.layout("WEB-INF/index.scaml",
      Map(
        "missing" -> nonexistent,
        "duplicates" -> dupes,
        "watched" -> watched,
        "extensions" -> extensions
      ))

    renderOkResponse(content)(request, response, control)
  }
}
