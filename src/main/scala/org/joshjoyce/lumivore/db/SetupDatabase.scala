package org.joshjoyce.lumivore.db

object SetupDatabase {
  def main(args: Array[String]) {
    val db = new SqliteDatabase
    db.connect()
    db.createTables()
  }
}
