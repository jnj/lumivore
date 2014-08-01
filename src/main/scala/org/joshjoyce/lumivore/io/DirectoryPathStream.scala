package org.joshjoyce.lumivore.io

import java.io.File
import java.nio.file.{AccessDeniedException, Files, Path}
import scala.collection.JavaConversions

object DirectoryPathStream {
  def recurse(root: File)(f: Path => Any) = {
    val d = new DirectoryPathStream(root, f)
    d.start()
  }
}

class DirectoryPathStream(val dir: File, visitor: Path => Any) {
  import JavaConversions._

  def start() {
    if (dir.isFile) {
      visitor(dir.toPath)
    } else {
      try {
        val children = Files.newDirectoryStream(dir.toPath)

        children.foreach {
          c => {
            val s = new DirectoryPathStream(c.toFile, visitor)
            s.start()
          }
        }
      } catch {
        case (e: AccessDeniedException) => {}// skip it
        case (e: Exception) => throw e
      }
    }
  }
}
