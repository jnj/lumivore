package org.joshjoyce.lumivore.sync

import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.util.Implicits
import org.joshjoyce.lumivore.db.SqliteDatabase
import java.io.File

class UploadProcess(vaultName: String, database: SqliteDatabase, uploader: GlacierUploader) {
  import Implicits._

  private val fiber = new ThreadFiber
  private val resultsChannel = new MemoryChannel[GlacierUploadAttempt]

  resultsChannel.subscribe(fiber) {
    // TODO
    case (u: GlacierUpload) => {}
    case (f: FailedUpload) => {}
  }

  def start() {
    val syncs = database.getSyncs
    val uploads = database.getGlacierUploads.groupBy(_._1)

    syncs.foreach {
      case (path, sha1, _) => {
        if (uploads.contains(sha1)) {
          // nothing to upload
        } else {
          uploader.upload(new File(path), vaultName)
        }
      }
    }
  }

  def shutdown() {
    fiber.dispose()
  }
}
