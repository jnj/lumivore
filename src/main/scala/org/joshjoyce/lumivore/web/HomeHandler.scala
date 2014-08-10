package org.joshjoyce.lumivore.web

import org.webbitserver.{HttpControl, HttpResponse, HttpRequest, HttpHandler}
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.fusesource.scalate.TemplateEngine

class HomeHandler(templateEngine: TemplateEngine, database: SqliteDatabase) extends HttpHandler with WebbitSupport {

  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    //val records = database.queryPhotos().map(_.toString)
    val records = Nil
    val dupes = Nil //database.getDuplicates
    val extensions = database.getExtensions.sorted
    val content = templateEngine.layout("WEB-INF/index.scaml", Map("records" -> records, "duplicates" -> dupes, "extensions" -> extensions))
    renderOkResponse(content)(request, response, control)
  }
}
