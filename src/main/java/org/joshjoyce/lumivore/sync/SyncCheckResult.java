package org.joshjoyce.lumivore.sync;

public class SyncCheckResult {

    public enum Status {
        Unseen,
        ContentsChanged,
        SyncDone
    }

    public final String path;
    public final String oldHash;
    public final String hash;

    public SyncCheckResult(Status status, String path, String oldHash, String hash) {
        this.path = path;
        this.oldHash = oldHash;
        this.hash = hash;
    }

    public static SyncCheckResult unseen(String path) {
        return new SyncCheckResult(Status.Unseen, path, null, null);
    }

    public static SyncCheckResult contentsChanged(String path, String oldHash, String hash) {
        return new SyncCheckResult(Status.ContentsChanged, path, oldHash, hash);
    }

    public static SyncCheckResult done() {
        return new SyncCheckResult(Status.SyncDone, null, null, null);
    }
}
