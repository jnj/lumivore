package org.joshjoyce.lumivore

import java.io.File
import org.joshjoyce.lumivore.index.{IndexRecord, Indexer}
import org.joshjoyce.lumivore.io.DirectoryPathStream
import org.joshjoyce.lumivore.util.Implicits
import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber

object Main {
  import Implicits._

  def main(args: Array[String]) {
    val dir = args(0)
    val paths = new DirectoryPathStream(new File(dir))
    val outputChannel = new MemoryChannel[IndexRecord]
    val fiber = new ThreadFiber()
    fiber.start()

    outputChannel.subscribe(fiber) {
      println(_)
    }

    val indexer = new Indexer(paths, outputChannel)
  }
}
