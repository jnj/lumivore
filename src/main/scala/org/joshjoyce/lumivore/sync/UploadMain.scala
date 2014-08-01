package org.joshjoyce.lumivore.sync

import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.util.LumivoreLogging
import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber

object UploadMain extends LumivoreLogging {

  def main(args: Array[String]) {
    val database = new SqliteDatabase
    database.connect()
    val fiber = new ThreadFiber
    fiber.start()

    val uploadResultChannel = new MemoryChannel[GlacierUploadAttempt]
    val uploader = new GlacierUploader(uploadResultChannel)
    uploader.init()

    val upload = new UploadProcess("photos", database, uploader, uploadResultChannel)
    upload.start()
  }
}
