package org.joshjoyce.lumivore.io

import java.security.MessageDigest
import java.nio.file.{Files, Path}
import javax.xml.bind.DatatypeConverter

object HashUtils {
  private val digest = MessageDigest.getInstance("SHA-1")

  def hashContents(path: Path): String = {
    val bytes = Files.readAllBytes(path)
    digest.update(bytes)
    val sha1Bytes = digest.digest()
    val hash = DatatypeConverter.printHexBinary(sha1Bytes)
    digest.reset()
    hash
  }
}
