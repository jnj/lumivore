package org.joshjoyce.lumivore.sync;

public record UploadAttemptResult(
        UploadAttemptResult.Status status,
        String filePath,
        String vaultName,
        String archiveId,
        int percent,
        Throwable exception) {

    boolean isDone() {
        return status() == UploadAttemptResult.Status.Done;
    }

    public enum Status {
        Complete,
        PartialUpload,
        FailedUpload,
        Done
    }

    public static UploadAttemptResult done() {
        return new UploadAttemptResult(UploadAttemptResult.Status.Done, null, null, null, 100, null);
    }
}
