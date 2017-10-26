package org.joshjoyce.lumivore.io

import java.io.{BufferedInputStream, FileInputStream}
import java.nio.file.Path
import java.security.{DigestInputStream, MessageDigest}
import javax.xml.bind.DatatypeConverter

object HashUtils {
  private val digest = MessageDigest.getInstance("SHA-1")

  def hashContents(path: Path): String = {
    val dis = new DigestInputStream(new BufferedInputStream(new FileInputStream(path.toFile)), digest)
    val buffer = Array.ofDim[Byte](8192)

    while (dis.read(buffer) != -1) {
      // nothing to do
    }

    val sha1Bytes = digest.digest()
    dis.close()
    val hash = DatatypeConverter.printHexBinary(sha1Bytes)
    digest.reset()
    hash
  }
}
