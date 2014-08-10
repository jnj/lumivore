package org.joshjoyce.lumivore.web

import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.util.LumivoreLogging
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.webbitserver.{WebSocketConnection, WebSocketHandler}
import scala.collection.JavaConversions
import scala.util.parsing.json.{JSONObject, JSON}

class WebSocketMessageRouter(database: SqliteDatabase, registry: Map[String, WebSocketResponder])
  extends WebSocketHandler with LumivoreLogging {
  import JavaConversions._

  private val fiber = new ThreadFiber
  fiber.start()

  override def onPong(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onPing(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onMessage(connection: WebSocketConnection, msg: Array[Byte]) = {}

  override def onMessage(connection: WebSocketConnection, msg: String) = {
    log.info("Received string msg " + msg)
    JSON.parseRaw(msg) match {
      case None => log.warn("Not a valid JSON document or list of documents: " + msg)
      case Some(jsonType) => jsonType match {
        case (json: JSONObject) => handle(json, connection)
        case _ => log.warn("Not a JSON document (looks like an array of documents): " + msg)
      }
    }
  }

  private def handle(json: JSONObject, connection: WebSocketConnection) {
    val msgType = json.obj.get("msgType").getOrElse("").toString
    println("msg type = '" + msgType + "'")
    println("registry = " + registry)
    val maybeResponder: Option[WebSocketResponder] = registry.get(msgType)

    println("got responder: " + maybeResponder)

    maybeResponder.foreach {
      responder => responder.respond(json.obj, connection, fiber)
    }
  }

  override def onClose(connection: WebSocketConnection) = {}

  override def onOpen(connection: WebSocketConnection) = {}
}
