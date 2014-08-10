package org.joshjoyce.lumivore.web

import org.webbitserver.{HttpControl, HttpResponse, HttpRequest, HttpHandler}
import org.fusesource.scalate.TemplateEngine

class BackupHandler(templateEngine: TemplateEngine) extends HttpHandler with WebbitSupport {
  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    val content = templateEngine.layout("WEB-INF/backup.scaml")
    renderOkResponse(content)(request, response, control)
  }
}
