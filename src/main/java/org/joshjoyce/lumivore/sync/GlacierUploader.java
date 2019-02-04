package org.joshjoyce.lumivore.sync;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import org.apache.log4j.Logger;
import org.jetlang.channels.Channel;
import org.joshjoyce.lumivore.io.FileUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlacierUploader {
    private static final Logger log = Logger.getLogger(GlacierUploader.class);

    private AmazonGlacier client;
    private ArchiveTransferManager atm;
    private final ExecutorService pool;
    private final Channel<UploadAttemptResult> output;

    private final String endpoint = "https://glacier.us-east-1.amazonaws.com/";
    private final Regions region = Regions.US_EAST_1;

    public GlacierUploader(Channel<UploadAttemptResult> output) {
        this.output = output;
        this.pool = Executors.newFixedThreadPool(10);
    }

    void init() {
        var builder =
                AmazonGlacierClientBuilder.standard()
                .withCredentials(new ClasspathPropertiesFileCredentialsProvider("AwsCredentials.properties"))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region.getName()));

        client = builder.build();

        var atmBuilder = new ArchiveTransferManagerBuilder().withGlacierClient(client);
        atm = atmBuilder.build();
    }

    public void stop() {
        client.shutdown();
        pool.shutdownNow();
    }

    void upload(File archive, String vaultName, int percent) {
        if (!archive.exists()) {
            log.warn("Not uploading " + archive.getAbsolutePath() + " because it doesn't exist");
            return;
        }
        try {
            final var totalBytes = FileUtils.getSizeInBytes(archive.getAbsolutePath());
            output.publish(new UploadAttemptResult(UploadAttemptResult.Status.PartialUpload, archive.getAbsolutePath(), null, null, 0, null));

            var progressListener = new ProgressListener() {
                private long bytesTransferred;

                @Override
                public void progressChanged(ProgressEvent event) {
                    var type = event.getEventType();
                    if (type.isByteCountEvent()) {
                        if (type.equals(ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT)) {
                            bytesTransferred += event.getBytesTransferred();
                        }
                    }
                    if (totalBytes > 0) {
                        int percent = (int) Math.round(100.0 * ((double) bytesTransferred) / totalBytes);
                        var partial = new UploadAttemptResult(UploadAttemptResult.Status.PartialUpload,
                            archive.getAbsolutePath(), null, null, percent, null);
                        output.publish(partial);
                    }
                }
            };
            var result = atm.upload("-", vaultName, archive.toString(), archive, progressListener);
            log.info(String.format("%s|%s", archive.getAbsolutePath(), result.getArchiveId()));
            var complete = new UploadAttemptResult(UploadAttemptResult.Status.Complete, archive.getPath(),
                    vaultName, result.getArchiveId(), percent, null);
            output.publish(complete);
        } catch (Exception e) {
            output.publish(new UploadAttemptResult(UploadAttemptResult.Status.FailedUpload, archive.getPath(),
                    vaultName, null, percent, e));
        }
    }
}
