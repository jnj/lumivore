package org.joshjoyce.lumivore.db

import java.sql.{Statement, DriverManager, Connection}

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
    executeUpdate(sql)
  }

  def executeUpdate(sql: String) {
    var statement: Statement = null
    try {
      statement = conn.createStatement()
      statement.executeUpdate(sql)
    } finally {
      if (statement != null) {
        statement.close()
      }
    }
  }
}
