package org.joshjoyce.lumivore.sync

import org.jetlang.fibers.ThreadFiber
import org.jetlang.channels.MemoryChannel
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.io.HashUtils
import org.joshjoyce.lumivore.util.{LumivoreLogging, Implicits}
import java.nio.file.Paths
import java.sql.SQLException

object SyncMain extends LumivoreLogging {
  import Implicits._

  def main(args: Array[String]) {
    val database = new SqliteDatabase
    database.connect()

    val fiber = new ThreadFiber
    fiber.start()

    val syncChannel = new MemoryChannel[SyncCheckResult]
    syncChannel.subscribe(fiber) {
      case Unseen(path) => {
        val sha1 = HashUtils.hashContents(path)
        try {
          database.insertSync(path.toString, sha1)
          log.info("inserted " + path + " -> " + sha1)
        } catch {
          case (e: SQLException) if e.getMessage.contains("CONSTRAINT") => {
            log.info("DUPLICATED FILE FOUND: " + path)
          }
        }
      }
      case ContentsChanged(path, hash) => {
        database.updateSync(path.toString, hash)
        log.info("Updated " + path)
      }
    }

    args.foreach {
      arg => {
        val sync = new SyncStream(database)
        sync.addObserver(syncChannel)
        sync.check(Paths.get(arg))
      }
    }

    fiber.join()
  }
}
