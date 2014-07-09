package org.joshjoyce.lumivore

import org.joshjoyce.lumivore.web.Lumivore
import org.fusesource.scalate.TemplateEngine
import java.io.File
import org.fusesource.scalate.layout.DefaultLayoutStrategy

object Main {
  def main(args: Array[String]) {
    val templateEngine = new TemplateEngine(Seq("src/main/webapp").map(new File(_)))
    templateEngine.layoutStrategy = {
      new DefaultLayoutStrategy(templateEngine, "src/main/webapp/WEB-INF/layouts/default.scaml")
    }
    val lum = new Lumivore(8080, templateEngine)
    lum.start()
  }
}
