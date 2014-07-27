package org.joshjoyce.lumivore.sync

import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.event.ProgressEvent
import com.amazonaws.event.ProgressListener
import com.amazonaws.services.glacier.AmazonGlacierClient
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager
import com.amazonaws.services.glacier.transfer.UploadResult
import java.io.File
import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.util.LumivoreLogging

sealed trait GlacierUploadAttempt
case class GlacierUpload(filePath: String, vaultName: String, uploadResult: UploadResult) extends GlacierUploadAttempt
case class FailedUpload(filePath: String, vaultName: String, e: Throwable) extends GlacierUploadAttempt

class GlacierUploader(output: Channel[GlacierUploadAttempt]) extends LumivoreLogging {
  private var credentials: PropertiesCredentials = _
  private var client: AmazonGlacierClient = _
  private var atm: ArchiveTransferManager = _

  def init() {
    credentials = new PropertiesCredentials(Thread.currentThread.getContextClassLoader.getResourceAsStream("AwsCredentials.properties"))
    client = new AmazonGlacierClient(credentials)
    client.setEndpoint("https://glacier.us-east-1.amazonaws.com/")
    atm = new ArchiveTransferManager(client, credentials)
  }

  def upload(archive: File, vaultName: String) {
    try {
      val result: UploadResult = atm.upload("-", vaultName, archive.toString, archive, new ProgressListener {
        def progressChanged(progressEvent: ProgressEvent) {
        }
      })
      log.info(String.format("%s|%s", archive.getAbsolutePath, result.getArchiveId))
      output.publish(GlacierUpload(archive.getPath, vaultName, result))
    } catch {
      case (e: Throwable) => output.publish(FailedUpload(archive.getPath, vaultName, e))
    }
  }
}
