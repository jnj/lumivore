package org.joshjoyce.lumivore.sync;

public class UploadAttemptResult {

    public enum Status {
        Complete,
        PartialUpload,
        FailedUpload,
        Done
    }

    public final Status status;
    public final String filePath;
    public final String vaultName;
    public final String archiveId;
    public final int percent;
    public final Throwable exception;

    public static UploadAttemptResult done() {
        return new UploadAttemptResult(UploadAttemptResult.Status.Done, null, null, null, 100, null);
    }

    UploadAttemptResult(Status status, String filePath, String vaultName, String archiveId, int percent, Throwable exception) {
        this.status = status;
        this.filePath = filePath;
        this.vaultName = vaultName;
        this.archiveId = archiveId;
        this.percent = percent;
        this.exception = exception;
    }
}
