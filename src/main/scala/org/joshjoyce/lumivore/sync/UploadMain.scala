package org.joshjoyce.lumivore.sync

import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.util.{Implicits, LumivoreLogging}
import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber

object UploadMain extends LumivoreLogging {
  import Implicits._

  def main(args: Array[String]) {
    val database = new SqliteDatabase
    database.connect()

    val resultFiber = new ThreadFiber
    val runnerFiber = new ThreadFiber

    resultFiber.start()
    runnerFiber.start()

    val uploadResultChannel = new MemoryChannel[GlacierUploadAttempt]
    uploadResultChannel.subscribe(resultFiber) {
      case Done => new Thread(new Runnable {
        override def run(): Unit = {
          runnerFiber.dispose()
          resultFiber.dispose()
        }
      }).start()
      case _ => {}
    }

    val uploader = new GlacierUploader(uploadResultChannel)
    uploader.init()

    val upload = new UploadProcess("photos", database, uploader, uploadResultChannel, runnerFiber, resultFiber)
    upload.start()
  }
}
