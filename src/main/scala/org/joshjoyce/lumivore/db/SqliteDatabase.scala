package org.joshjoyce.lumivore.db

import java.sql._
import java.nio.file.Path

class SqliteDatabase {

  Class.forName("org.sqlite.JDBC")
  private implicit var conn: Connection = _
  private var connected = false

  private val noop: Any => Any = _ => {}

  def connect() {
    conn = DriverManager.getConnection("jdbc:sqlite:photos.db")
    connected = true
  }

  def createTables() {
    ensureConnected()
    val sql =
      """
        |DROP TABLE IF EXISTS SYNCS;
        |CREATE TABLE SYNCS (PATH TEXT UNIQUE, SHA1 TEXT UNIQUE, SYNC_TIME INTEGER);
        |
        |DROP TABLE IF EXISTS PHOTOS;
        |
        |DROP TABLE IF EXISTS GLACIER_UPLOADS;
        |CREATE TABLE GLACIER_UPLOADS (HASH TEXT PRIMARY KEY, VAULT TEXT, ARCHIVE_ID TEXT);
        |
        |DROP TABLE IF EXISTS EXTENSIONS;
        |CREATE TABLE EXTENSIONS (EXTENSION TEXT UNIQUE);
      """.stripMargin
    executeUpdate(sql)
  }

  def insertSync(path: String, sha1: String) {
    ensureConnected()
    executeWithPreparedStatement("INSERT INTO SYNCS(PATH, SHA1, SYNC_TIME VALUES (?, ?, ?);") {
      s => {
        s.setString(1, path)
        s.setString(2, sha1)
        s.setLong(3, System.currentTimeMillis())
      }
    }
  }

  def getSync(path: String) = {
    ensureConnected()
    withQuery("SELECT PATH, SHA1, SYNC_TIME FROM SYNCS WHERE PATH = ? ;") {
      s => s.setString(1, path)
    } {
      mapResults(_) {
        r => (r.getString("PATH"), r.getString("SHA1"), r.getLong("SYNC_TIME"))
      }
    }
  }

  def getSyncs = {
    ensureConnected()
    withQuery("SELECT PATH, SHA1, SYNC_TIME FROM SYNCS;")(noop) {
      mapResults(_) {
        r => (r.getString("PATH"), r.getString("SHA1"), r.getLong("SYNC_TIME"))
      }
    }
  }

  def getGlacierUploads = {
    ensureConnected()
    withQuery("SELECT HASH, VAULT, ARCHIVE_ID FROM GLACIER_UPLOADS;")(noop) {
      mapResults(_) {
        r => (r.getString("HASH"), r.getString("VAULT"), r.getString("ARCHIVE_ID"))
      }
    }
  }

  def addExtension(ext: String) {
    ensureConnected()
    executeWithPreparedStatement("INSERT INTO EXTENSIONS(EXTENSION) VALUES (?);") {
      s => {
        s.setString(1, ext)
      }
    }
  }

  def getExtensions = {
    ensureConnected()
    withQuery("SELECT EXTENSION FROM EXTENSIONS;")(noop) {
      mapResults(_)(_.getString("EXTENSION"))
    }
  }

  def getWatchedDirectories = {
    ensureConnected()
    withQuery("SELECT PATH FROM INDEXED_PATHS;")(noop) {
      mapResults(_)(_.getString("PATH"))
    }
  }

  def addWatchedDirectory(path: Path) {
    ensureConnected()
    executeWithPreparedStatement("INSERT INTO INDEXED_PATHS(PATH) VALUES (?);") {
      s => {
        s.setString(1, path.toString)
      }
    }
  }

  def getDuplicates = {
    ensureConnected()
    val sql =
      """
        |SELECT PATHS FROM (
        |   SELECT GROUP_CONCAT(FILE_PATH, ';') AS PATHS, COUNT(FILE_PATH) AS TOTAL FROM PHOTOS GROUP BY HASH
        |) WHERE TOTAL > 1;
      """.stripMargin
    withQuery(sql)(noop) {
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

  def executeUpdate(sql: String) {
    withStatement {
      s => {
        s.executeUpdate(sql)
      }
    }
  }

  private def withQuery[A](sql: String)(g: PreparedStatement => Any)(f: ResultSet => A) = {
    var stmt: PreparedStatement = null
    try {
      stmt = conn.prepareStatement(sql)
      g(stmt)
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

  private def executeWithPreparedStatement[A](sql: String)(f: PreparedStatement => A) = {
    var stmt: PreparedStatement = null
    try {
      stmt = conn.prepareStatement(sql)
      f(stmt)
      stmt.execute()
    } finally {
      if (stmt != null) {
        stmt.close()
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
