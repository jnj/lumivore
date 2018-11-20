package org.joshjoyce.lumivore.sync;

import org.apache.log4j.Logger;
import org.jetlang.channels.Channel;
import org.jetlang.fibers.Fiber;
import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class UploadProcess {
    private static final Logger log = Logger.getLogger(UploadProcess.class);

    private final Fiber runnerFiber;
    private final String vaultName;
    private final SqliteDatabase database;
    private final GlacierUploader uploader;

    private AtomicBoolean running = new AtomicBoolean(false);
    private Map<String, String> hashByPath = new HashMap<>();

    public UploadProcess(String vaultName, SqliteDatabase database, GlacierUploader uploader,
                         Channel<GlacierUploadAttempt> resultsChannel,
                         Fiber runnerFiber, Fiber resultsFiber) {
        this.vaultName = vaultName;
        this.database = database;
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

    public void start() {
        runnerFiber.execute(() -> {
            running.set(true);
            var indexed = database.getWatchedDirectories();
            var uploads = database.getGlacierUploads().stream().collect(Collectors.groupingBy(u -> u.hash));
            log.info(uploads.size() + " uploads found");
            var filteredSyncs = database.getSyncs().stream().filter(s -> !uploads.containsKey(s.hash)).collect(Collectors.toList());
            hashByPath.putAll(filteredSyncs.stream().collect(Collectors.toMap(t -> t.path, t -> t.hash)));

            for (int j = 0; j < filteredSyncs.size(); j++) {
                SqliteDatabase.Sync sync = filteredSyncs.get(j);

                if (running.get() && indexed.stream().anyMatch(sync.path::contains)) {
                    log.info("Uploading " + sync.path + " (" + (j + 1) + " / " + filteredSyncs.size() + ")");
                    var percent = (int) Math.round(100.0 * (j + 1) / filteredSyncs.size());
                    uploader.upload(new File(sync.path), vaultName, percent);
                }
            }

            running.set(false);
        });
    }

    public void stop() {
        running.set(false);
    }
}
