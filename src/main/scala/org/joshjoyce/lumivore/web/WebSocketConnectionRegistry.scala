package org.joshjoyce.lumivore.web

import org.webbitserver.WebSocketConnection

class WebSocketConnectionRegistry {
  private var connections: List[WebSocketConnection] = Nil

  def onOpen(conn: WebSocketConnection) {
    connections = conn :: connections
  }

  def onClose(conn: WebSocketConnection) {
    connections = connections.filter(conn!=)
  }
}
