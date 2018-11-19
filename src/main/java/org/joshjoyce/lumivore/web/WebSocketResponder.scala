package org.joshjoyce.lumivore.web

import org.jetlang.fibers.Fiber
import org.webbitserver.WebSocketConnection

trait WebSocketResponder {
  def msgType: String
  def respond(msg: Map[String, Any], connection: WebSocketConnection, fiber: Fiber)
}
