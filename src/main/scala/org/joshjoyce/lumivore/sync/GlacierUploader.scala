package org.joshjoyce.lumivore.sync

import java.io.{ByteArrayInputStream, File, FileInputStream}
import java.util

import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.glacier.model.{CompleteMultipartUploadRequest, InitiateMultipartUploadRequest, UploadMultipartPartRequest}
import com.amazonaws.services.glacier.transfer.{ArchiveTransferManager, UploadResult}
import com.amazonaws.services.glacier.{AmazonGlacierClient, TreeHashGenerator}
import com.amazonaws.util.BinaryUtils
import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.util.LumivoreLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

sealed trait GlacierUploadAttempt
case class CompleteUpload(filePath: String, vaultName: String, archiveId: String, percent: Int) extends GlacierUploadAttempt
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

  def stop() {
    client.shutdown()
  }

  def upload(archive: File, vaultName: String, percent: Int) {
    if (!archive.exists()) {
      log.warn("Not uploading " + archive.getAbsolutePath + " because it doesn't exist")
      return
    }
    try {
      if (archive.length() > 5 * 1024 * 1024) {
        multipartUpload(archive, vaultName, percent)
      } else {
        var bytesTransferred = 0L
        var totalBytes = 0L
        output.publish(PartialUpload(archive.getAbsolutePath, 0))
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
        output.publish(CompleteUpload(archive.getPath, vaultName, result.getArchiveId, percent))
      }
    } catch {
      case (e: Throwable) => output.publish(FailedUpload(archive.getPath, vaultName, e, percent))
    }
  }

  def multipartUpload(archive: File, vaultName: String, percent: Int): Unit = {
    val partSize = 1024 * 1024
    val initiation = new InitiateMultipartUploadRequest(vaultName, "", partSize.toString)
    val initResult = client.initiateMultipartUpload(initiation)

    val filePosition = 0
    var currentPosition = 0L
    val buffer = Array.ofDim[Byte](partSize)
    val checkSums = ArrayBuffer.empty[Array[Byte]]

    val fis = new FileInputStream(archive)
    var contentRange: String = ""
    var read = 0
    var done = false

    while (!done && currentPosition < archive.length()) {
      read = fis.read(buffer, filePosition, buffer.size)
      if (read > -1) {
        val bytesRead = util.Arrays.copyOf(buffer, read)
        contentRange = "bytes %s-%s/*".format(currentPosition, currentPosition + read - 1)
        val checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead))
        val binaryChecksum = BinaryUtils.fromHex(checksum)
        checkSums += binaryChecksum

        var partRequest = new UploadMultipartPartRequest
        partRequest = partRequest.withVaultName(vaultName)
        partRequest = partRequest.withBody(new ByteArrayInputStream(bytesRead))
        partRequest = partRequest.withChecksum(checksum)
        partRequest = partRequest.withRange(contentRange)
        partRequest = partRequest.withUploadId(initResult.getUploadId)

        val partResult = client.uploadMultipartPart(partRequest)
        currentPosition = currentPosition + read
      } else {
        done = true
      }
    }

    fis.close()
    val hash = TreeHashGenerator.calculateTreeHash(checkSums.toList)
    val archiveId = completeMultipartUpload(initResult.getUploadId, hash, archive, vaultName)
    output.publish(CompleteUpload(archive.getPath, vaultName, archiveId, percent))
  }

  def completeMultipartUpload(uploadId: String, checksum: String, archive: File, vaultName: String) = {
    val request = new CompleteMultipartUploadRequest(vaultName, uploadId, archive.length.toString,checksum)
    val result = client.completeMultipartUpload(request)
    result.getLocation
  }
}
