package org.joshjoyce.lumivore.web

import org.webbitserver.{HttpControl, HttpResponse, HttpRequest, HttpHandler}
import org.json4s.{DefaultFormats, Formats}
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.fusesource.scalate.TemplateEngine

class HomeHandler(templateEngine: TemplateEngine, database: SqliteDatabase) extends HttpHandler with WebbitSupport {
  implicit protected val jsonFormats: Formats = DefaultFormats

  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    val records = database.queryPhotos().map(_.toString)
    val dupes = database.getDuplicates
    val extensions = database.getExtensions.sorted
    val content = templateEngine.layout("WEB-INF/index.scaml", Map("records" -> records, "duplicates" -> dupes, "extensions" -> extensions))
    renderOkResponse(content)(request, response, control)
  }
}
