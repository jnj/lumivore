package org.joshjoyce.lumivore.web

import com.amazonaws.services.glacier.model.DescribeVaultResult
import org.joshjoyce.lumivore.sync.VaultRetrieval
import org.webbitserver.{HttpControl, HttpResponse, HttpRequest, HttpHandler}
import org.fusesource.scalate.TemplateEngine
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.util.Implicits

class HttpRouteHandler(templateEngine: TemplateEngine, database: SqliteDatabase) extends HttpHandler with WebbitSupport {
  import Implicits._

  override def handleHttpRequest(request: HttpRequest, response: HttpResponse, control: HttpControl) = {
    println("uri is " + request.uri())
    request.uri() match {
      case "/index" => renderIndex(request, response, control)
      case _ => renderHome (request, response, control)
    }
  }

 def renderHome(request: HttpRequest, response: HttpResponse, control: HttpControl) {
    val records = Nil
    val content = templateEngine.layout("WEB-INF/index.scaml", Map("records" -> records))
    //val retrieval = new VaultRetrieval
    //retrieval.init()
    //val result: DescribeVaultResult = retrieval.retrieve("photos")
   renderOkResponse(content)(request, response, control)
  }

  def renderIndex(request: HttpRequest, response: HttpResponse, control: HttpControl) {
    val watchDirs = database.getWatchedDirectories
    val content = templateEngine.layout("WEB-INF/indexer.scaml", Map("watchDirs" -> watchDirs))
    renderOkResponse(content)(request, response, control)
  }
}
