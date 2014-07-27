package org.joshjoyce.lumivore.sync

import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.util.{LumivoreLogging, Implicits}
import org.joshjoyce.lumivore.db.SqliteDatabase
import java.io.File

class UploadProcess(vaultName: String, database: SqliteDatabase, uploader: GlacierUploader) extends LumivoreLogging {

  import Implicits._

  private val fiber = new ThreadFiber
  private val resultsChannel = new MemoryChannel[GlacierUploadAttempt]
  private var hashByPath: Map[String, String] = _

  resultsChannel.subscribe(fiber) {
    case (u: GlacierUpload) => {
      val hash = hashByPath(u.filePath)
      try {
        database.insertUpload(hash, u.vaultName, u.uploadResult.getArchiveId)
      } catch {
        case (e: Exception) => log.error("Upload file but failed to write to database: " + hash + " " + u.filePath + " " + u.uploadResult.getArchiveId)
      }
    }
    case (f: FailedUpload) => log.error("Failed upload: " + f.filePath)
  }

  def start() {
    val syncs = database.getSyncs
    val uploads = database.getGlacierUploads.groupBy(_._1)
    hashByPath ++= syncs.map {
      t => (t._1, t._2)
    }.toMap

    syncs.foreach {
      case (path, sha1, _) =>
        if (uploads.contains(sha1)) {
          // nothing to upload
        } else {
          uploader.upload(new File(path), vaultName)
        }
    }
  }

  def shutdown() {
    fiber.dispose()
  }
}
