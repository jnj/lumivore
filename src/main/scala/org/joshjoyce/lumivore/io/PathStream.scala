package org.joshjoyce.lumivore.io

import java.nio.file.Path

trait PathStream {
  def paths: Stream[Path]
}
