package org.joshjoyce.lumivore.index

import java.nio.file.{Paths, Files, Path}
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.io.PathStream
import org.joshjoyce.lumivore.util.LumivoreLogging

class Indexer(val pathStream: PathStream, output: Channel[IndexRecord]) extends LumivoreLogging {
  private val suffixes = Set(".jpg", ".psd", ".tiff", ".orf", ".rw2", ".jpeg", ".dng")
  private val digest = MessageDigest.getInstance("SHA-1")

  private val allPaths = pathStream.paths.toSeq.filterNot(_.toFile.isDirectory)

  def start() {
    allPaths.zipWithIndex.foreach {
      case (path, i) => index(path, i)
    }
  }

  def index(path: Path, index: Int) {
    val file = path.toFile
    if (file.isDirectory) {
      // skip
    } else {
      val normalizedPath = path.toAbsolutePath.toString.toLowerCase
      if (suffixes.exists(normalizedPath.endsWith)) {
        digest.reset()
        val hash = createDigestHash(path)
        val record = IndexRecord(Paths.get(normalizedPath), hash, index + 1, allPaths.size)
        log.info("Indexed " + record)
        output.publish(record)
      }
    }
  }

  def createDigestHash(path: Path): String = {
    val bytes = Files.readAllBytes(path)
    digest.update(bytes)
    val sha1Bytes = digest.digest()
    DatatypeConverter.printHexBinary(sha1Bytes)
  }
}
