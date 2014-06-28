package org.joshjoyce.lumivore.db

import java.sql._
import org.joshjoyce.lumivore.index.IndexRecord
import java.nio.file.Paths

class SqliteDatabase {

  Class.forName("org.sqlite.JDBC")
  private implicit var conn: Connection = _
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
    withQuery(sql) {
      r => mapResults(r) {
        s => IndexRecord(Paths.get(s.getString("FILE_PATH")), s.getString("HASH"))
      }
    }
  }

  def getDuplicates() = {
    ensureConnected()
    val sql =
      """
        |SELECT PATHS FROM (
        |   SELECT GROUP_CONCAT(FILE_PATH, ';') AS PATHS, COUNT(FILE_PATH) AS TOTAL FROM PHOTOS GROUP BY HASH
        |) WHERE TOTAL > 1;
      """.stripMargin
    withQuery(sql) {
      r => mapResults(r) {
        s => s.getString("PATHS")
      }
    }
  }

  def mapResults[A](r: ResultSet)(f: (ResultSet) => A): List[A] = {
    var results = List.empty[A]

    while (r.next()) {
      val x = f(r)
      results = x :: results
    }

    results.reverse
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
    withStatement {
      _.executeUpdate(sql)
    }
  }

  private def withQuery[A](sql: String)(f: ResultSet => A) = {
    var stmt: PreparedStatement = null
    try {
      stmt = conn.prepareStatement(sql)
      withResultSet(stmt.executeQuery())(f)
    } finally {
      if (stmt != null) {
        stmt.close()
      }
    }
  }

  private def withResultSet[A](r: ResultSet)(f: ResultSet => A) = {
    try {
      f(r)
    } finally {
      if (r != null) {
        r.close()
      }
    }
  }

  private def withStatement[A](f: Statement => A) = {
    var stmt: Statement = null
    try {
      stmt = conn.createStatement()
      f(stmt)
    } finally {
      if (stmt != null) {
        stmt.close()
      }
    }
  }

  private def ensureConnected() {
    if (!connected) {
      throw new IllegalStateException("not connected")
    }
  }
}
