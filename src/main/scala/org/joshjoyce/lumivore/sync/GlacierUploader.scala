package org.joshjoyce.lumivore.sync

import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.event.{ProgressEventType, ProgressEvent, ProgressListener}
import com.amazonaws.services.glacier.AmazonGlacierClient
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager
import com.amazonaws.services.glacier.transfer.UploadResult
import java.io.File
import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.util.LumivoreLogging

sealed trait GlacierUploadAttempt
case class CompleteUpload(filePath: String, vaultName: String, uploadResult: UploadResult, percent: Int) extends GlacierUploadAttempt
case class PartialUpload(filePath: String, percent: Int) extends GlacierUploadAttempt
case class FailedUpload(filePath: String, vaultName: String, e: Throwable, percent: Int) extends GlacierUploadAttempt
case object Done extends GlacierUploadAttempt

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

  def upload(archive: File, vaultName: String, percent: Int) {
    try {
      var bytesTransferred = 0L
      var totalBytes = 0L

      val result: UploadResult = atm.upload("-", vaultName, archive.toString, archive, new ProgressListener {
        def progressChanged(progressEvent: ProgressEvent) {
          if (progressEvent.getEventType.isByteCountEvent) {
            if (progressEvent.getEventType.equals(ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT)) {
              totalBytes = progressEvent.getBytes
            }
            bytesTransferred += progressEvent.getBytesTransferred
          }
          if (totalBytes > 0) {
            val partial: PartialUpload = PartialUpload(archive.getAbsolutePath, scala.math.round(100.0 * bytesTransferred / totalBytes).toInt)
            output.publish(partial)
          }
        }
      })
      log.info(String.format("%s|%s", archive.getAbsolutePath, result.getArchiveId))
      output.publish(CompleteUpload(archive.getPath, vaultName, result, percent))
    } catch {
      case (e: Throwable) => output.publish(FailedUpload(archive.getPath, vaultName, e, percent))
    }
  }
}
