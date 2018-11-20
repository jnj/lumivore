package org.joshjoyce.lumivore.sync;

import java.nio.file.Path;

public class SyncCheckResult {

    public enum Status {
        Unseen,
        ContentsChanged,
        SyncDone
    }

    public final Status status;
    public final Path path;
    public final String oldHash;
    public final String hash;

    public SyncCheckResult(Status status, Path path, String oldHash, String hash) {
        this.status = status;
        this.path = path;
        this.oldHash = oldHash;
        this.hash = hash;
    }

    public static SyncCheckResult unseen(Path path) {
        return new SyncCheckResult(Status.Unseen, path, null, null);
    }

    public static SyncCheckResult contentsChanged(Path path, String oldHash, String hash) {
        return new SyncCheckResult(Status.ContentsChanged, path, oldHash, hash);
    }

    public static SyncCheckResult done() {
        return new SyncCheckResult(Status.SyncDone, null, null, null);
    }
}
