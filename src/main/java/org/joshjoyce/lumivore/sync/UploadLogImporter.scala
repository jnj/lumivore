package org.joshjoyce.lumivore.sync

import org.joshjoyce.lumivore.db.SqliteDatabase
import scala.io.Source
import java.sql.SQLException

object UploadLogImporter {
  val key = "INFO  GlacierUploader - "
  val keyLen = key.size

  def main(args: Array[String]) {
    if (args.isEmpty) {
      println("No file given")
      System.exit(-1)
    }
    val filename = args(0)
    val database = new SqliteDatabase
    database.connect()

    val source = Source.fromFile(filename)

    source.getLines().foreach {
      line => {
        if (line.contains(key)) {
          val pipeIndex = line.indexOf('|', keyLen)
          val path = line.substring(keyLen, pipeIndex)
          val archiveId = line.substring(pipeIndex + 1)
          val syncs = database.getSync(path)
          println(path)
          syncs.headOption.foreach {
            case (_, sha1, _) => try {
              database.insertUpload(sha1, "photos", archiveId)
            } catch {
              case (e:SQLException) if e.getMessage.contains("CONSTRAINT") => {}
            }
          }
        }
      }
    }
  }
}
