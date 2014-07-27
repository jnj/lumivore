package org.joshjoyce.lumivore.web

import org.webbitserver.{WebServers, WebServer}
import org.webbitserver.handler.StaticFileHandler
import org.fusesource.scalate.TemplateEngine
import org.joshjoyce.lumivore.db.SqliteDatabase

class Lumivore(port: Int, templateEngine: TemplateEngine, database: SqliteDatabase) {
  private var webserver: WebServer = _
  private var running = false
  private val routeHandler = new HttpRouteHandler(templateEngine, database)

  def start() {
    if (!running) {
      running = true
      webserver = WebServers.createWebServer(port)
      webserver.add(new StaticFileHandler("src/main/webapp"))
      webserver.add("/", new HomeHandler(templateEngine, database))
      webserver.add("/index", routeHandler)
//      webserver.add("/ws", new WebSocketMessageRouter(database))
      webserver.add("/addExtension", new AddExtensionsHandler(database))
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
