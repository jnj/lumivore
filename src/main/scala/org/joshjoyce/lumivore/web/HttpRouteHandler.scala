package org.joshjoyce.lumivore.web

import org.webbitserver.{HttpControl, HttpResponse, HttpRequest, HttpHandler}
import org.fusesource.scalate.TemplateEngine
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.json4s.{DefaultFormats, Formats}
import org.joshjoyce.lumivore.util.Implicits

class HttpRouteHandler(templateEngine: TemplateEngine) extends HttpHandler {
  import Implicits._

  implicit protected val jsonFormats: Formats = DefaultFormats
  val database = new SqliteDatabase
  database.connect()

  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    val records = database.queryPhotos().map(_.toString)
    val dupes = database.getDuplicates()
    val content = templateEngine.layout("WEB-INF/index.scaml", Map("records" -> records, "duplicates" -> dupes))
    val r: Runnable = () => {
      response.status(200)
      response.content(content)
      response.end()
    }

    control.handlerExecutor().execute(r)
  }
}
