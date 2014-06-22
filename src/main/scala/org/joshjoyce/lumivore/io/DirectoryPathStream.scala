package org.joshjoyce.lumivore.io

import java.io.File
import java.nio.file.Path

class DirectoryPathStream(val dir: File) extends PathStream {
  override def paths = recurPaths(dir)

  private def recurPaths(d: File): Stream[Path] = {
    if (d.isFile) {
      Stream(d.toPath)
    } else {
      val children = d.listFiles().toStream
      val x = for {
        c <- children
      } yield recurPaths(c)
      x.flatten
    }
  }
}