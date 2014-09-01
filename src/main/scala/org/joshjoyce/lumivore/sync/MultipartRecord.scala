package org.joshjoyce.lumivore.sync

case class MultipartRecord(bytes: Array[Byte], contentRange: String, uploadId: String, vaultName: String)