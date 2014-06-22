package org.joshjoyce.lumivore.index

import java.nio.file.{Paths, Files, Path}
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.io.PathStream

class Indexer(val pathStream: PathStream, output: Channel[IndexRecord]) {
  private val suffixes = Set(".jpg", ".psd", ".tiff", ".orf", ".rw2", ".jpeg", ".dng")
  private val digest = MessageDigest.getInstance("SHA-1")

  pathStream.paths.foreach(index)

  def index(path: Path) {
    val file = path.toFile
    if (file.isDirectory) {
      // skip
    } else {
      val normalizedPath = path.toAbsolutePath.toString.toLowerCase
      if (suffixes.exists(normalizedPath.endsWith)) {
        val hash = createDigestHash(path)
        val record = IndexRecord(Paths.get(normalizedPath), hash)
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
