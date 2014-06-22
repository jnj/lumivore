package org.joshjoyce.lumivore

import java.io.File
import org.joshjoyce.lumivore.index.{IndexRecord, Indexer}
import org.joshjoyce.lumivore.io.DirectoryPathStream
import org.joshjoyce.lumivore.util.Implicits
import org.jetlang.channels.MemoryChannel
import org.jetlang.fibers.ThreadFiber
import org.joshjoyce.lumivore.db.SqliteDatabase

object Main {
  import Implicits._

  def main(args: Array[String]) {
//    val dir = args(0)
//    val paths = new DirectoryPathStream(new File(dir))
//    val outputChannel = new MemoryChannel[IndexRecord]
    val database = new SqliteDatabase
    database.connect()
//    database.createTables()
//
//    val fiber = new ThreadFiber()
//    fiber.start()
//
//    outputChannel.subscribe(fiber) {
//      r => {
//        database.insert(r)
//        println(r)
//      }
//    }
//
//    val indexer = new Indexer(paths, outputChannel)

    database.queryPhotos().foreach(println)


  }
}
