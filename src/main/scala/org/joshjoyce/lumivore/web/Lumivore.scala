package org.joshjoyce.lumivore.web

import org.webbitserver.{WebServers, WebServer}
import org.webbitserver.handler.StaticFileHandler
import org.fusesource.scalate.TemplateEngine

class Lumivore(port: Int, templateEngine: TemplateEngine) {
  private var webserver: WebServer = _
  private var running = false

  def start() {
    if (!running) {
      running = true
      webserver = WebServers.createWebServer(port)
      webserver.add(new StaticFileHandler("src/main/webapp"))
      webserver.add("/index", new HttpRouteHandler(templateEngine))
      webserver.add("/index-request", new IndexRequestHandler)
      val future = webserver.start()
      future.get()
    }
  }

  def stop() {
    if (running) {
      val future = webserver.stop()
      future.get()
      running = false
    }
  }
}
