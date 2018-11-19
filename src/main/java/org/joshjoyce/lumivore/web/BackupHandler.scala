package org.joshjoyce.lumivore.web

import org.fusesource.scalate.TemplateEngine
import org.webbitserver.{HttpControl, HttpHandler, HttpRequest, HttpResponse}

class BackupHandler(templateEngine: TemplateEngine) extends HttpHandler with WebbitSupport {
  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    val content = templateEngine.layout("WEB-INF/backup.scaml")
    renderOkResponse(content)(request, response, control)
  }
}
