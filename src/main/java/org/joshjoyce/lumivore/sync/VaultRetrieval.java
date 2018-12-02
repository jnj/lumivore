package org.joshjoyce.lumivore.sync;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.DescribeVaultRequest;
import com.amazonaws.services.glacier.model.DescribeVaultResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;

import java.io.IOException;

public class VaultRetrieval {
    private AmazonGlacierClient client;
    private ArchiveTransferManager atm;

    public void init() {
        try {
            var credentials = new PropertiesCredentials(Thread.currentThread().getContextClassLoader().getResourceAsStream("AwsCredentials.properties"));
            client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            atm = new ArchiveTransferManager(client, credentials);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DescribeVaultResult retrieve(String vaultName) {
        var request = new DescribeVaultRequest(vaultName);
        return client.describeVault(request);
    }
}
