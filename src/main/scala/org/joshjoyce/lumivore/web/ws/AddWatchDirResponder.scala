package org.joshjoyce.lumivore.web.ws

import java.io.File
import org.jetlang.fibers.Fiber
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.util.Implicits
import org.joshjoyce.lumivore.web.WebSocketResponder
import org.webbitserver.WebSocketConnection

class AddWatchDirResponder(database: SqliteDatabase) extends WebSocketResponder {

  import Implicits._

  override def msgType = "addDir"

  override def respond(msg: Map[String, AnyRef], connection: WebSocketConnection, fiber: Fiber) = {
    //    val dirPath = msg("path").toString
    //    val f = new File(dirPath)
    //    if (f.exists && f.isDirectory) {
    //      val watched = database.getWatchedDirectories
    //      if (!watched.exists(_.contains(dirPath))) {
    //        database.addWatchedDirectory(f.toPath)
    //        val all: java.util.List[String] = (dirPath :: watched).sorted
    //        val m = new java.util.HashMap[String, AnyRef]
    //        m.put("msgType", "watchDirs")
    //        m.put("dirs", all)
    ////        val encoded = mapper.writeValueAsString(m)
    ////        connection.handlerExecutor().execute(() => connection.send(encoded))
    //      }
    //
    //    }
    //
  }
}
