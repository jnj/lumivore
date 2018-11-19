package org.joshjoyce.lumivore.sync;

import org.apache.log4j.Logger;
import org.jetlang.channels.Channel;
import org.joshjoyce.lumivore.db.SqliteDatabase;
import org.joshjoyce.lumivore.io.DirectoryPathStream;
import org.joshjoyce.lumivore.io.HashUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Examines the filesystem for paths that have not been synced.
 */
public class SyncStream {
    private static final Logger log = Logger.getLogger(SyncStream.class);

    private List<Channel<SyncCheckResult>> observers;
    private SqliteDatabase db;

    SyncStream(SqliteDatabase db) {
        this.db = db;
        this.observers = new ArrayList<>();
    }

    public void addObserver(Channel<SyncCheckResult> c) {
        observers.add(0, c);
    }

    private void notifyObservers(SyncCheckResult result) {
        observers.forEach(c -> c.publish(result));
    }

    /**
     * Begin at the root path and traverse recursively,
     * returning any files that don't have sync records
     * or that have changed.
     */
    public void check(Path root) {
        var validExtensions = db.getExtensions();
        log.info("Loaded extensions: " + String.join(", ", validExtensions));
        log.info("Executing from root: " + root.toString());

        var sha1ByPath = db.getSyncs().stream()
                .collect(Collectors.groupingBy(sync -> sync.path))
                .entrySet().stream().
                        collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0).hash));

        DirectoryPathStream.recurse(root.toFile(), path -> {
            var pathString = path.toString();
            var normPath = Paths.get(pathString);
            var loweredPath = pathString.toLowerCase();

            if (validExtensions.isEmpty() || validExtensions.stream().anyMatch(loweredPath::endsWith)) {
                var syncOpt = sha1ByPath.get(pathString);

                if (syncOpt == null) {
                    log.info("Unseen|" + pathString);
                    notifyObservers(SyncCheckResult.unseen(normPath.toString()));
                } else {
                    var hash = HashUtils.hashContents(normPath);

                    if (!hash.equals(syncOpt)) {
                        log.info("Changed|" + pathString);
                        notifyObservers(SyncCheckResult.contentsChanged(normPath.toString(), syncOpt, hash));
                    } else {
                        log.info("NoChange|" + pathString);
                    }
                }
            }
        });

        observers.forEach(c -> c.publish(SyncCheckResult.done()));
    }
}

