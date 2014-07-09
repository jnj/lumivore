package org.joshjoyce.lumivore.web

import org.webbitserver.{WebSocketConnection, WebSocketHandler}

class IndexRequestHandler extends WebSocketHandler {
  override def onPong(connection: WebSocketConnection, msg: Array[Byte]) = ???

  override def onPing(connection: WebSocketConnection, msg: Array[Byte]) = ???

  override def onMessage(connection: WebSocketConnection, msg: Array[Byte]) = ???

  override def onMessage(connection: WebSocketConnection, msg: String) = ???

  override def onClose(connection: WebSocketConnection) = ???

  override def onOpen(connection: WebSocketConnection) = ???
}
