package org.joshjoyce.lumivore.sync;

import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.services.glacier.AmazonGlacierClient
import com.amazonaws.services.glacier.model.DescribeVaultRequest
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager

class VaultRetrieval {
  private var credentials: PropertiesCredentials = _
  private var client: AmazonGlacierClient = _
  private var atm: ArchiveTransferManager = _

  def init() {
    credentials = new PropertiesCredentials(Thread.currentThread.getContextClassLoader.getResourceAsStream("AwsCredentials.properties"))
    client = new AmazonGlacierClient(credentials)
    client.setEndpoint("https://glacier.us-east-1.amazonaws.com/")
    atm = new ArchiveTransferManager(client, credentials)
  }

  def retrieve(vaultName: String) = {
    val request = new DescribeVaultRequest(vaultName)
    client.describeVault(request)
  }
}
