package org.joshjoyce.lumivore.sync;

import org.apache.log4j.Logger;
import org.jetlang.channels.Channel;
import org.jetlang.fibers.Fiber;
import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class UploadProcess implements Runnable {
    private static final Logger log = Logger.getLogger(UploadProcess.class);

    private final Channel<UploadAttemptResult> channel;
    private final String vaultName;
    private final SqliteDatabase db;
    private final GlacierUploader uploader;
    private final Map<String, String> hashByPath = new ConcurrentHashMap<>();

    public UploadProcess(String vaultName, SqliteDatabase db, GlacierUploader uploader,
                         Channel<UploadAttemptResult> channel, Fiber resultsFiber) {

        this.db = db;
        this.channel = channel;
        this.uploader = uploader;
        this.vaultName = vaultName;
        var workflow = new ResultsWorkflow(resultsFiber);
    }

    @Override
    public void run() {
        var indexed = db.getWatchedDirectories();
        var uploads = db.getGlacierUploads().stream().collect(Collectors.groupingBy(u -> u.hash));
        log.info(uploads.size() + " uploads found");

        var filteredSyncs = db.getSyncs()
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

        channel.publish(UploadAttemptResult.done());
    }

    private class ResultsWorkflow {
        ResultsWorkflow(Fiber resultsFiber) {
            channel.subscribe(resultsFiber, msg -> {
                switch (msg.status) {
                    case Complete: {
                        var hash = hashByPath.get(msg.filePath);

                        try {
                            db.insertUpload(hash, msg.vaultName, msg.archiveId);
                        } catch (Exception e) {
                            log.error("Upload file but failed to write to db: " + hash +
                                      " " + msg.filePath + " " + msg.archiveId);
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
    }
}
