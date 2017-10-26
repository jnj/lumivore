package org.joshjoyce.lumivore.sync

import java.io.File
import java.util.concurrent.Executors

import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.services.glacier.AmazonGlacierClient
import com.amazonaws.services.glacier.transfer.{ArchiveTransferManager, UploadResult}
import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.io.FileUtils
import org.joshjoyce.lumivore.util.LumivoreLogging

sealed trait GlacierUploadAttempt

case class CompleteUpload(filePath: String, vaultName: String, archiveId: String, percent: Int) extends GlacierUploadAttempt

case class PartialUpload(filePath: String, percent: Int) extends GlacierUploadAttempt

case class FailedUpload(filePath: String, vaultName: String, e: Throwable, percent: Int) extends GlacierUploadAttempt

case object Done extends GlacierUploadAttempt

class GlacierUploader(output: Channel[GlacierUploadAttempt]) extends LumivoreLogging {
  private var credentials: PropertiesCredentials = _
  private var client: AmazonGlacierClient = _
  private var atm: ArchiveTransferManager = _
  private val pool = Executors.newFixedThreadPool(10)

  def init() {
    credentials = new PropertiesCredentials(Thread.currentThread.getContextClassLoader.getResourceAsStream("AwsCredentials.properties"))
    client = new AmazonGlacierClient(credentials)
    client.setEndpoint("https://glacier.us-east-1.amazonaws.com/")
    atm = new ArchiveTransferManager(client, credentials)
  }

  def stop() {
    client.shutdown()
    pool.shutdownNow()
  }

  def upload(archive: File, vaultName: String, percent: Int) {
    if (!archive.exists()) {
      log.warn("Not uploading " + archive.getAbsolutePath + " because it doesn't exist")
      return
    }
    try {
      var bytesTransferred = 0L
      val totalBytes = FileUtils.getSizeInBytes(archive.getAbsolutePath)
      output.publish(PartialUpload(archive.getAbsolutePath, 0))
      val result: UploadResult = atm.upload("-", vaultName, archive.toString, archive, new ProgressListener {
        def progressChanged(progressEvent: ProgressEvent) {
          if (progressEvent.getEventType.isByteCountEvent) {
            if (progressEvent.getEventType.equals(ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT)) {

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
     } catch {
      case (e: Throwable) => output.publish(FailedUpload(archive.getPath, vaultName, e, percent))
    }
  }
}


//  def multipartUpload(archive: File, vaultName: String, percent: Int): Unit = {
//    val partSize = 1024 * 1024 * 2
//    val initiation = new InitiateMultipartUploadRequest(vaultName, "", partSize.toString)
//    val initResult = client.initiateMultipartUpload(initiation)
//
//    val filePosition = 0
//    var currentPosition = 0L
//    val buffer = Array.ofDim[Byte](partSize)
//
//    val fis = new FileInputStream(archive)
//    var contentRange: String = ""
//    var read = 0
//    var done = false
//
//    var records = List.empty[MultipartRecord]
//
//    while (!done && currentPosition < archive.length()) {
//      read = fis.read(buffer, filePosition, buffer.size)
//      if (read > -1) {
//        val bytesRead = util.Arrays.copyOf(buffer, read)
//        log.info("read %s bytes from %s".format(read, archive.toString))
//        contentRange = "bytes %s-%s/*".format(currentPosition, currentPosition + read - 1)
//        records = MultipartRecord(bytesRead, contentRange, initResult.getUploadId, vaultName) :: records
//        currentPosition = currentPosition + read
//      } else {
//        done = true
//      }
//    }
//
//    fis.close()
//
//    val callables: List[Callable[(String, Array[Byte])]] = records.map {
//      r => {
//        val c = new Callable[(String, Array[Byte])] {
//          override def call(): (String, Array[Byte]) = {
//            val checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(r.bytes))
//            val binaryChecksum = BinaryUtils.fromHex(checksum)
//            var partRequest = new UploadMultipartPartRequest
//            partRequest = partRequest.withVaultName(vaultName)
//            partRequest = partRequest.withBody(new ByteArrayInputStream(r.bytes))
//            partRequest = partRequest.withChecksum(checksum)
//            partRequest = partRequest.withRange(contentRange)
//            partRequest = partRequest.withUploadId(initResult.getUploadId)
//            client.uploadMultipartPart(partRequest)
//            (contentRange, binaryChecksum)
//          }
//        }
//        c
//      }
//    }
//    val futures = pool.invokeAll(callables)
//    val checksums = futures.map {f => f.get()}.sortBy(_._1).map(_._2)
//    val hash = TreeHashGenerator.calculateTreeHash(checksums)
//    val archiveId = completeMultipartUpload(initResult.getUploadId, hash, archive, vaultName)
//    output.publish(CompleteUpload(archive.getPath, vaultName, archiveId, percent))
//  }
//
//  def completeMultipartUpload(uploadId: String, checksum: String, archive: File, vaultName: String) = {
//    val request = new CompleteMultipartUploadRequest(vaultName, uploadId, archive.length.toString,checksum)
//    val result = client.completeMultipartUpload(request)
//    result.getLocation
//  }
//
//}

