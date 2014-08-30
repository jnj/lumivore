package org.joshjoyce.lumivore.sync

import java.nio.file.Paths
import java.sql.SQLException
import java.util.concurrent.{Callable, Executors, Future}

import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.io.HashUtils
import org.joshjoyce.lumivore.util.LumivoreLogging

object SyncMain extends LumivoreLogging {
  import org.joshjoyce.lumivore.util.Implicits._
  import scala.collection.JavaConversions._

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

    val executor = Executors.newCachedThreadPool()
    var callables = List.empty[Callable[Unit]]

    database.getWatchedDirectories.foreach {
      arg => {
        val sync = new SyncStream(database)
        sync.addObserver(syncChannel)
        callables = new Callable[Unit] {
          override def call() {
            sync.check(Paths.get(arg))
          }
        } :: callables
      }
    }

    val futures: List[Future[Unit]] = executor.invokeAll(callables).toList
    futures.foreach(_.get())
    fiber.join()
  }
}
