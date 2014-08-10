package org.joshjoyce.lumivore.web

import org.webbitserver.WebSocketConnection
import org.jetlang.fibers.{ThreadFiber, Fiber}
import org.joshjoyce.lumivore.sync._
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.jetlang.channels.MemoryChannel
import org.joshjoyce.lumivore.util.Implicits
import org.joshjoyce.lumivore.sync.CompleteUpload
import scala.Some
import scala.util.parsing.json.JSONObject

class BackupResponder(database: SqliteDatabase) extends WebSocketResponder {
  import Implicits._
  override def msgType = "backup"
  private var uploaderFiber: Option[Fiber] = None
  private var resultsFiber: Option[Fiber] = None
  private var glacierUploader: GlacierUploader = _

  override def respond(msg: Map[String, Any], connection: WebSocketConnection, fiber: Fiber) = {
    implicit val con = connection

    msg.get("action") match {
      case Some("start") => {
        uploaderFiber = Some(new ThreadFiber)
        resultsFiber = Some(new ThreadFiber)
        Seq(uploaderFiber, resultsFiber).foreach(_.foreach(_.start()))

        val outputChannel = new MemoryChannel[GlacierUploadAttempt]
        glacierUploader = new GlacierUploader(outputChannel)
        glacierUploader.init()

        val upload = new UploadProcess("photos", database, glacierUploader, outputChannel, uploaderFiber.get, resultsFiber.get)

        outputChannel.subscribe(resultsFiber.get) {
          case (g: CompleteUpload) => {
            val json = JSONObject(Map("msgType" -> "completeUpload", "percent" -> g.percent, "file" -> g.filePath)).toString()
            executeOnConnectionThread(con.send(json))
          }
          case (p: PartialUpload) => executeOnConnectionThread {
            val json = JSONObject(Map("msgType" -> "partialUpload", "percent" -> p.percent, "file" -> p.filePath)).toString()
            executeOnConnectionThread(con.send(json))
          }
          case (f: FailedUpload) => {
            val json = JSONObject(Map("msgType" -> "completeUpload", "percent" -> f.percent, "file" -> f.filePath)).toString()
            executeOnConnectionThread(con.send(json))
          }
          case Done => stop()
        }

        upload.start()
      }
      case Some("stop") => stop()
      case _ => {}
    }
  }

  private def executeOnConnectionThread(f: => Any)(implicit con: WebSocketConnection) {
    con.execute(new Runnable {
      override def run(): Unit = f
    })
  }

  private def stop() {
    glacierUploader.stop()
    resultsFiber.foreach(_.dispose())
    uploaderFiber.foreach(_.dispose())
    resultsFiber = None
    uploaderFiber = None
  }
}
