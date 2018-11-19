package org.joshjoyce.lumivore.sync;

public class MultipartRecord {
    public final byte[] bytes;
    public final String contentRange;
    public final String uploadId;
    public final String vaultName;

    public MultipartRecord(byte[] bytes, String contentRange, String uploadId, String vaultName) {
        this.bytes = bytes;
        this.contentRange = contentRange;
        this.uploadId = uploadId;
        this.vaultName = vaultName;
    }
}