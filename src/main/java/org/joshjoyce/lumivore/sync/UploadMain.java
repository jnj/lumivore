package org.joshjoyce.lumivore.sync;

import org.jetlang.channels.Channel;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.ThreadFiber;
import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.util.concurrent.CountDownLatch;

public class UploadMain {

    public static void main(String[] args) {
        final var database = new SqliteDatabase();
        database.connect();

        final var resultFiber = newFiber();
        final var runnerFiber = newFiber();
        final var latch = new CountDownLatch(1);
        final var channel = new MemoryChannel<UploadAttemptResult>();
        final var workflow = new ResultsWorkflow(channel, resultFiber, latch);
        final var uploader = new GlacierUploader(channel);
        uploader.init();

        final var upload = new UploadProcess("photos", database, uploader, channel, resultFiber);
        runnerFiber.execute(upload);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        resultFiber.dispose();
        runnerFiber.dispose();
    }

    private static Fiber newFiber() {
        var fiber = new ThreadFiber();
        fiber.start();
        return fiber;
    }

    static class ResultsWorkflow {
        ResultsWorkflow(Channel<UploadAttemptResult> channel, Fiber fiber, CountDownLatch latch) {
            channel.subscribe(fiber, result -> {
                if (result.status == UploadAttemptResult.Status.Done) {
                    latch.countDown();
                }
           });
        }
    }
}
