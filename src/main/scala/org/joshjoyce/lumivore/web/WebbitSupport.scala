package org.joshjoyce.lumivore.web

import org.webbitserver.{HttpResponse, HttpRequest, HttpControl}
import org.joshjoyce.lumivore.util.Implicits

trait WebbitSupport {
  import Implicits._

  def redirect(uri: String)(implicit req: HttpRequest, res: HttpResponse, control: HttpControl) {
    val r: Runnable = () => {
      res.status(302)
      res.header("Location", uri)
      res.end()
    }
    control.handlerExecutor().execute(r)
  }

  def renderOkResponse(content: String)(implicit req: HttpRequest, res: HttpResponse, control: HttpControl) {
    val r: Runnable = () => {
      res.status(200)
      res.content(content)
      res.end()
    }

    control.handlerExecutor().execute(r)
  }
}
