package org.joshjoyce.lumivore.index

import java.nio.file.{Paths, Files, Path}
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import org.jetlang.channels.Channel
import org.joshjoyce.lumivore.io.{HashUtils, PathStream}
import org.joshjoyce.lumivore.util.LumivoreLogging

class Indexer(val pathStream: PathStream, output: Channel[IndexRecord], suffixes: Set[String]) extends LumivoreLogging {

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
        val hash = HashUtils.hashContents(path)
        val record = IndexRecord(Paths.get(normalizedPath), hash, index + 1, allPaths.size)
        log.info("Indexed " + record)
        output.publish(record)
      }
    }
  }

}
