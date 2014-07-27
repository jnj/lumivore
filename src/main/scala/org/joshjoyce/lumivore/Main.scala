package org.joshjoyce.lumivore

import java.io.File
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.joshjoyce.lumivore.web.Lumivore
import org.joshjoyce.lumivore.db.SqliteDatabase

object Main {
  def main(args: Array[String]) {
    val templateEngine = new TemplateEngine(Seq("src/main/webapp").map(new File(_)))
    templateEngine.layoutStrategy = new DefaultLayoutStrategy(templateEngine, "/WEB-INF/layouts/default.scaml")
    val database = new SqliteDatabase
    database.connect()
    val lum = new Lumivore(8080, templateEngine, database)
    lum.start()
  }
}
