package org.joshjoyce.lumivore.db

import java.sql.{DriverManager, Connection}

class SqliteDatabase {
  Class.forName("org.sqlite.JDBC")
  var conn: Connection = _

  def connect() {
    conn = DriverManager.getConnection("jdbc:sqlite:photos.db")
  }

  def createTables() {
    val sql =
      """
        |CREATE TABLE PHOTOS (ID INTEGER PRIMARY KEY, FILE_PATH TEXT UNIQUE, HASH TEXT);
      """.stripMargin
    val statement = conn.createStatement()
    statement.executeUpdate(sql)
  }
}
