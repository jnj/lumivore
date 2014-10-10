package org.joshjoyce.lumivore.sync

import java.nio.file.{Path, Paths}

import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.db.SqliteDatabase
import org.joshjoyce.lumivore.io.{DirectoryPathStream, HashUtils}
import org.joshjoyce.lumivore.util.LumivoreLogging

sealed trait SyncCheckResult

case class Unseen(path: Path) extends SyncCheckResult

case class ContentsChanged(path: Path, oldHash: String, hash: String) extends SyncCheckResult

case object SyncDone extends SyncCheckResult

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

          if (!syncOpt.isDefined) {
            log.info("Unseen|" + pathString)
            val unseen = Unseen(normPath)
            notifyObservers(unseen)
          } else {
            val syncHash = syncOpt.get
            val hash = HashUtils.hashContents(normPath)

            if (hash != syncHash) {
              log.info("Changed|" + pathString)
              val changed = ContentsChanged(normPath, syncHash, hash)
              notifyObservers(changed)
            } else {
              log.info("NoChange|" + pathString)
            }
          }
        }
      }
    }
    
    observers.foreach(_.publish(SyncDone))
  }
}
