package org.joshjoyce.lumivore.web

import com.fasterxml.jackson.databind.ObjectMapper
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.util.{LumivoreLogging, Implicits}
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.webbitserver.{WebSocketConnection, WebSocketHandler}

class WebSocketMessageRouter(database: SqliteDatabase, registry: Map[String, WebSocketResponder])
  extends WebSocketHandler with LumivoreLogging {

  private val mapper = new ObjectMapper
  private val fiber = new ThreadFiber
  fiber.start()

  override def onPong(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onPing(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onMessage(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onMessage(connection: WebSocketConnection, msg: String) = {
    log.info("Received string msg " + msg)
    val parsed = mapper.readValue(msg, classOf[java.util.Map[String, java.lang.Object]])

    if (!parsed.containsKey("action")) {
      throw new IllegalArgumentException("JSON needs an 'action' key")
    }

//    registry.get(parsed.get("action"))
//     match {
//
//    }
  }
//
//
//    val channel = new MemoryChannel[IndexRecord]
//    val subFiber = new ThreadFiber
//    subFiber.start()
//    val sub = channel.subscribe(subFiber) {
//      r => connection.send(r.asJson)
//    }
//    val extensions = database.getExtensions
//    fiber.execute(new Runnable {
//      override def run() = {
//        val paths = new DirectoryPathStream(new File("/home/josh/Pictures"))
//        val indexer = new Indexer(paths, channel, extensions.toSet)
//        indexer.start()
//        sub.dispose()
//        subFiber.dispose()
//      }
//    })
//  }

  override def onClose(connection: WebSocketConnection) = {}

  override def onOpen(connection: WebSocketConnection) = {}
}
