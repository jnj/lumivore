package org.joshjoyce.lumivore.sync;

import org.apache.log4j.Logger;
import org.jetlang.channels.Channel;
import org.jetlang.fibers.Fiber;
import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class UploadProcess {
    private static final Logger log = Logger.getLogger(UploadProcess.class);

    private static final UploadAttemptResult DONE =
            new UploadAttemptResult(UploadAttemptResult.Status.Done, null, null, null, 100, null);

    private final Channel<UploadAttemptResult> channel;
    private final Fiber runnerFiber;
    private final String vaultName;
    private final SqliteDatabase database;
    private final GlacierUploader uploader;

    private Map<String, String> hashByPath = new HashMap<>();

    public UploadProcess(String vaultName, SqliteDatabase database, GlacierUploader uploader,
                         Channel<UploadAttemptResult> resultsChannel,
                         Fiber runnerFiber, Fiber resultsFiber) {

        this.vaultName = vaultName;
        this.database = database;
        this.channel = resultsChannel;
        this.runnerFiber = runnerFiber;
        this.uploader = uploader;

        resultsChannel.subscribe(resultsFiber, msg -> {
            switch (msg.status) {
                case Complete: {
                    var hash = hashByPath.get(msg.filePath);
                    try {
                        database.insertUpload(hash, msg.vaultName, msg.archiveId);
                    } catch (Exception e) {
                        log.error("Upload file but failed to write to database: " + hash + " " + msg.filePath + " " + msg.archiveId);
                    }
                }
                return;
                case FailedUpload: {
                    log.error("Failed upload: " + msg.filePath);
                    log.error(msg.exception.getMessage(), msg.exception);
                }
            }
        });
    }

    void start() {
        runnerFiber.execute(() -> {
            var indexed = database.getWatchedDirectories();
            var uploads = database.getGlacierUploads().stream().collect(Collectors.groupingBy(u -> u.hash));
            log.info(uploads.size() + " uploads found");

            var filteredSyncs = database.getSyncs()
                    .stream()
                    .filter(s -> !uploads.containsKey(s.hash))
                    .collect(Collectors.toList());
            hashByPath.putAll(filteredSyncs.stream().collect(Collectors.toMap(t -> t.path, t -> t.hash)));

            for (var i = 0; i < filteredSyncs.size(); i++) {
                var sync = filteredSyncs.get(i);

                if (indexed.stream().anyMatch(sync.path::contains)) {
                    log.info("Uploading " + sync.path + " (" + (i + 1) + " / " + filteredSyncs.size() + ")");
                    var percent = (int) Math.round(100.0 * (i + 1) / filteredSyncs.size());
                    uploader.upload(new File(sync.path), vaultName, percent);
                }
            }

            channel.publish(DONE);
        });
    }
}
