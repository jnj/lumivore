package org.joshjoyce.lumivore.db

object SetupDatabase {
  def main(args: Array[String]) {
    val db = new SqliteDatabase
    db.connect()
    db.createIndexes()
    //db.createTables()
    //Set("jpg", "dng", "rw2", "orf").foreach(db.addExtension)
    println("ok")
  }
}
