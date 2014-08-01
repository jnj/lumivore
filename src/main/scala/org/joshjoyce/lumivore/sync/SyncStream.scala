package org.joshjoyce.lumivore.sync

import java.nio.file.{Paths, Path}
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.io.{HashUtils, DirectoryPathStream}
import org.jetlang.channels.{MemoryChannel, Channel}
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.util.{LumivoreLogging, Implicits}

sealed trait SyncCheckResult
case class Unseen(path: Path) extends SyncCheckResult
case class ContentsChanged(path: Path, hash: String) extends SyncCheckResult

object Test {
  import Implicits._

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

    DirectoryPathStream.recurse(root.toFile) {
      path => {
        log.info("got path " + path)
        val pathString = path.toString
        val normPath = Paths.get(pathString)
        val loweredPath = pathString.toLowerCase

        if (validExtensions.isEmpty || validExtensions.exists(loweredPath.endsWith)) {
          val syncs = database.getSync(pathString)

          if (syncs.isEmpty) {
            val unseen = Unseen(normPath)
            log.info("Path is unseen " + path)
            notifyObservers(unseen)
          } else {
            val (_, syncHash, _) = syncs.head
            val hash = HashUtils.hashContents(normPath)

            if (hash != syncHash) {
              val changed = ContentsChanged(normPath, hash)
              log.info("Path changed: " + path)
              notifyObservers(changed)
            } else {
              log.info("Syncs are unchanged for " + path)
            }
          }
        } else {
          log.info("nothing to do for path")
        }
      }
    }
  }
}
