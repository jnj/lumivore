package org.joshjoyce.lumivore.web

import org.webbitserver.WebSocketConnection
import org.jetlang.fibers.Fiber

trait WebSocketResponder {
  def msgType: String
  def respond(msg: Map[String, AnyRef], connection: WebSocketConnection, fiber: Fiber)
}
