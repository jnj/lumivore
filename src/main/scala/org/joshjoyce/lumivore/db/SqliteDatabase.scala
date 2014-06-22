package org.joshjoyce.lumivore.db

import java.sql._
import org.joshjoyce.lumivore.index.IndexRecord
import org.joshjoyce.lumivore.index.IndexRecord
import java.nio.file.Paths

class SqliteDatabase {

  Class.forName("org.sqlite.JDBC")
  private var conn: Connection = _
  private var connected = false

  def connect() {
    conn = DriverManager.getConnection("jdbc:sqlite:photos.db")
    connected = true
  }

  def createTables() {
    ensureConnected()
    val sql =
      """
        |DROP TABLE IF EXISTS PHOTOS;
        |CREATE TABLE PHOTOS (ID INTEGER PRIMARY KEY, FILE_PATH TEXT UNIQUE, HASH TEXT);
      """.stripMargin
    executeUpdate(sql)
  }

  def queryPhotos() = {
    ensureConnected()
    val sql =
      """
        |SELECT ID, FILE_PATH, HASH FROM PHOTOS;
      """.stripMargin
    var stmt: PreparedStatement = null
    var results: ResultSet = null
    var list = List.empty[IndexRecord]
    try {
      stmt = conn.prepareStatement(sql)
      results = stmt.executeQuery()
      while (results.next()) {
        val i = IndexRecord(Paths.get(results.getString("FILE_PATH")), results.getString("HASH"))
        list = i :: list
      }
    } finally {
      if (results != null) {
        results.close()
      }
      if (stmt != null) {
        stmt.close()
      }
    }
    list
  }

  def insert(record: IndexRecord) {
    ensureConnected()
    val sql =
      """
        |INSERT INTO PHOTOS(FILE_PATH, HASH) VALUES (?, ?);
      """.stripMargin
    var preparedStatement: PreparedStatement = null
    try {
      preparedStatement = conn.prepareStatement(sql)
      preparedStatement.setString(1, record.path.toString)
      preparedStatement.setString(2, record.digest)
      preparedStatement.execute()
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
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

  private def ensureConnected() {
    if (!connected) {
      throw new IllegalStateException("not connected")
    }
  }
}
