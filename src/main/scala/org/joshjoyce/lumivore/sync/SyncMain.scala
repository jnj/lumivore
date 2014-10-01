package org.joshjoyce.lumivore.sync

import java.nio.file.Paths
import java.sql.SQLException
import java.util.concurrent.{Callable, Executors, Future}

import org.jetlang.channels.MemoryChannel
import org.jetlang.core.SynchronousDisposingExecutor
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

    val executor1 = new SynchronousDisposingExecutor
    val fiber = new ThreadFiber(executor1, "SyncDBWriter", false)
    fiber.start()

    val syncChannel = new MemoryChannel[SyncCheckResult]
    syncChannel.subscribe(fiber) {
      case Unseen(path) => {
        val sha1 = HashUtils.hashContents(path)
        try {
          database.insertSync(path.toString, sha1)
          log.info("inserted " + path + " -> " + sha1)
        } catch {
          case (e: SQLException) if isConstraintViolation(e) => {
            log.warn("DUPLICATED FILE FOUND: " + path)
            try {
              database.insertDup(path.toString)
            } catch {
              case (e: Exception) => log.warn("exception while inserting duplicate", e)
            }
          }
          case e => log.error("Error when attempting to insert unseen path " + path, e)
        }
      }
      case ContentsChanged(path, oldHash, hash) => {
        log.info("Contents changed %s %s -> %s".format(path, oldHash, hash))
        try {
          database.insertContentChange(path.toString, oldHash, hash)
          database.updateSync(path.toString, hash)
        } catch {
          case (e: Exception) => log.error("Error attempting to update path " + path + " old hash " + oldHash + " new hash " + hash, e)
        }
      }
    }

    val executor = Executors.newFixedThreadPool(2)
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

  def isConstraintViolation(e: SQLException) = e.getMessage.toUpperCase.contains("CONSTRAINT")
}
