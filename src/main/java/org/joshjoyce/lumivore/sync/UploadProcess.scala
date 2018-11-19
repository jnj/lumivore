package org.joshjoyce.lumivore.sync;

import org.jetlang.channels.Channel
import org.jetlang.fibers.Fiber
import org.joshjoyce.lumivore.util.{LumivoreLogging, Implicits}
import org.joshjoyce.lumivore.db.SqliteDatabase
import java.io.File

import scala.math.round

class UploadProcess(vaultName: String, database: SqliteDatabase, uploader: GlacierUploader,
                    resultsChannel: Channel[GlacierUploadAttempt],
                    runnerFiber: Fiber, resultsFiber: Fiber) extends LumivoreLogging {

  import Implicits._

  @volatile
  private var running = false
  private var hashByPath: Map[String, String] = Map.empty

  resultsChannel.subscribe(resultsFiber) {
    case (u: CompleteUpload) => {
      val hash = hashByPath(u.filePath)
      try {
        database.insertUpload(hash, u.vaultName, u.archiveId)
      } catch {
        case (e: Exception) => log.error("Upload file but failed to write to database: " + hash + " " + u.filePath + " " + u.archiveId)
      }
    }
    case (f: FailedUpload) => {
      log.error("Failed upload: " + f.filePath)
      log.error(f.e.getMessage, f.e)
    }
    case _ => {}
  }

  def start() {
    runnerFiber.execute(new Runnable {
      override def run() {
        running = true
        val indexed = database.getWatchedDirectories
        val uploads = database.getGlacierUploads.groupBy(_._1)
        log.info(uploads.size + " uploads found")
        val filteredSyncs = database.getSyncs.filterNot { case (_, sha1, _) => uploads.contains(sha1) }

        hashByPath ++= filteredSyncs.map {
          t => (t._1, t._2)
        }.toMap

        filteredSyncs.zipWithIndex.foreach {
          case ((path, sha1, _), index) if running && indexed.exists(path.contains) =>
            log.info("Uploading " + path + " (" + (index+1) + " / " + filteredSyncs.size + ")")
            val percent = round(100.0 * (index + 1) / filteredSyncs.size).toInt
            uploader.upload(new File(path), vaultName, percent)
          case _ => {}
        }

        running = false
      }
    })
  }

  def stop() {
    running = false
  }
}
