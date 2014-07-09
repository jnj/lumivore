package org.joshjoyce.lumivore.index

import java.nio.file.Path

case class IndexRecord(path: Path, digest: String, itemNumber: Int, total: Int) {

  def asJson = """
      |{"file": "%s", "percent": %s}
    """.stripMargin.format(path, (100.0 * itemNumber / total).toInt)
}
