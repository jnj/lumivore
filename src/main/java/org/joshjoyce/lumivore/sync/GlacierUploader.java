package org.joshjoyce.lumivore.sync;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import org.apache.log4j.Logger;
import org.jetlang.channels.Channel;
import org.joshjoyce.lumivore.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlacierUploader {
    private static final Logger log = Logger.getLogger(GlacierUploader.class);

    private AmazonGlacierClient client;
    private ArchiveTransferManager atm;
    private final ExecutorService pool;
    private final Channel<GlacierUploadAttempt> output;

    public GlacierUploader(Channel<GlacierUploadAttempt> output) {
        this.output = output;
        this.pool = Executors.newFixedThreadPool(10);
    }

    void init() {
        try {
            PropertiesCredentials credentials = new PropertiesCredentials(Thread.currentThread().getContextClassLoader().getResourceAsStream("AwsCredentials.properties"));
            client = new AmazonGlacierClient(credentials);
            client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
            atm = new ArchiveTransferManager(client, credentials);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            output.publish(new GlacierUploadAttempt(GlacierUploadAttempt.Status.PartialUpload, archive.getAbsolutePath(), null, null, 0, null));

            var progressListener = new ProgressListener() {
                private long bytesTransferred;

                @Override
                public void progressChanged(ProgressEvent event) {
                    ProgressEventType type = event.getEventType();
                    if (type.isByteCountEvent()) {
                        if (type.equals(ProgressEventType.REQUEST_CONTENT_LENGTH_EVENT)) {
                            bytesTransferred += event.getBytesTransferred();
                        }
                    }
                    if (totalBytes > 0) {
                        int percent = (int) Math.round(100.0 * ((double) bytesTransferred) / totalBytes);
                        var partial = new GlacierUploadAttempt(GlacierUploadAttempt.Status.PartialUpload,
                            archive.getAbsolutePath(), null, null, percent, null);
                        output.publish(partial);
                    }
                }
            };
            var result = atm.upload("-", vaultName, archive.toString(), archive, progressListener);
            log.info(String.format("%s|%s", archive.getAbsolutePath(), result.getArchiveId()));
            var complete = new GlacierUploadAttempt(GlacierUploadAttempt.Status.Complete, archive.getPath(),
                    vaultName, result.getArchiveId(), percent, null);
            output.publish(complete);
        } catch (Exception e) {
            output.publish(new GlacierUploadAttempt(GlacierUploadAttempt.Status.FailedUpload, archive.getPath(),
                    vaultName, null, percent, e));
        }
    }
}


// TODO multipart upload
