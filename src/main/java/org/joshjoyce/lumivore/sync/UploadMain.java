package org.joshjoyce.lumivore.sync;

import org.jetlang.channels.MemoryChannel;
import org.jetlang.fibers.ThreadFiber;
import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.util.concurrent.CountDownLatch;

public class UploadMain {

    public static void main(String[] args) {
        var database = new SqliteDatabase();
        database.connect();

        var resultFiber = new ThreadFiber();
        var runnerFiber = new ThreadFiber();

        resultFiber.start();
        runnerFiber.start();
        CountDownLatch doneLatch = new CountDownLatch(1);

        var uploadResultChannel = new MemoryChannel<GlacierUploadAttempt>();
        uploadResultChannel.subscribe(resultFiber, attempt -> {
            if (attempt.status == GlacierUploadAttempt.Status.Done) {
                new Thread(() -> {
                    runnerFiber.dispose();
                    resultFiber.dispose();
                    doneLatch.countDown();
                }).start();
            }
        });

        var uploader = new GlacierUploader(uploadResultChannel);
        uploader.init();

        var upload = new UploadProcess("photos", database, uploader, uploadResultChannel, runnerFiber, resultFiber);
        upload.start();

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
