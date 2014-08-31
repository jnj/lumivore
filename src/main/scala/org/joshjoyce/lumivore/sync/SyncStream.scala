package org.joshjoyce.lumivore.sync

import java.io.File
import java.nio.file.{NoSuchFileException, Path, Paths}

import org.jetlang.channels.{Channel, MemoryChannel}
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.io.{DirectoryPathStream, HashUtils}
import org.joshjoyce.lumivore.util.LumivoreLogging

sealed trait SyncCheckResult

case class Unseen(path: Path) extends SyncCheckResult

case class ContentsChanged(path: Path, oldHash: String, hash: String) extends SyncCheckResult

object Test {

  import org.joshjoyce.lumivore.util.Implicits._

  def main(args: Array[String]) {
    val fiber = new ThreadFiber()
    fiber.start()
    val channel = new MemoryChannel[SyncCheckResult]
    channel.subscribe(fiber) {
      println(_)
    }

    val db = new SqliteDatabase
    db.connect()

    val sync = new SyncStream(db)
    sync.addObserver(channel)

    sync.check(Paths.get("/media/josh/fantom1/pictures"))
  }
}

/**
 * Examines the filesystem for paths that have not been synced.
 */
class SyncStream(database: SqliteDatabase) extends LumivoreLogging {
  private var observers: List[Channel[SyncCheckResult]] = Nil

  def addObserver(c: Channel[SyncCheckResult]) {
    observers = c :: observers
  }

  private def notifyObservers(result: SyncCheckResult) {
    observers.foreach(_.publish(result))
  }

  /**
   * Begin at the root path and traverse recursively,
   * returning any files that don't have sync records
   * or that have changed.
   */
  def check(root: Path) = {
    val validExtensions = database.getExtensions.toSet
    log.info("Loaded extensions: " + validExtensions.mkString(", "))
    log.info("Executing from root: " + root.toString)

    val sha1ByPath = database.getSyncs.groupBy(_._1).mapValues(_.head._2)
    DirectoryPathStream.recurse(root.toFile) {
      path => {
        val pathString = path.toString
        val normPath = Paths.get(pathString)
        val loweredPath = pathString.toLowerCase

        if (validExtensions.isEmpty || validExtensions.exists(loweredPath.endsWith)) {
          val syncOpt = sha1ByPath.get(pathString)
          log.info("path: " + pathString)
          if (!syncOpt.isDefined) {
            val unseen = Unseen(normPath)
            notifyObservers(unseen)
          } else {
            val syncHash = syncOpt.get
            val hash = HashUtils.hashContents(normPath)

            if (hash != syncHash) {
              val changed = ContentsChanged(normPath, syncHash, hash)
              notifyObservers(changed)
            }
          }
        }
      }
    }
  }
}
