package org.joshjoyce.lumivore.web

import org.webbitserver.{WebSocketConnection, WebSocketHandler}
import org.joshjoyce.lumivore.io.DirectoryPathStream
import java.io.File
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.index.{Indexer, IndexRecord}
import org.jetlang.channels.MemoryChannel
import org.joshjoyce.lumivore.util.Implicits

class IndexRequestHandler extends WebSocketHandler {
  import Implicits._
  private val fiber = new ThreadFiber
  fiber.start()

  override def onPong(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onPing(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onMessage(connection: WebSocketConnection, msg: Array[Byte]) = {
    println("got byte array msg")
  }

  override def onMessage(connection: WebSocketConnection, msg: String) = {
    println("got string msg: " + msg)
    val channel = new MemoryChannel[IndexRecord]
    val subFiber = new ThreadFiber
    subFiber.start()
    val sub = channel.subscribe(subFiber) {
      record => {
        println(record)
        connection.send(record.asJson)
      }
    }
    fiber.execute(new Runnable {
      override def run() = {
        val paths = new DirectoryPathStream(new File("/home/josh/Pictures"))
        val indexer = new Indexer(paths, channel)
        indexer.start()
        sub.dispose()
        subFiber.dispose()
        connection.close()
      }
    })
  }

  override def onClose(connection: WebSocketConnection) = {}

  override def onOpen(connection: WebSocketConnection) = {}
}
