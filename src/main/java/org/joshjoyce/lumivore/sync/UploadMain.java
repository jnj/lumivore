package org.joshjoyce.lumivore.sync;

import org.jetlang.channels.Channel;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.ThreadFiber;
import org.joshjoyce.lumivore.Module;
import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.util.concurrent.CountDownLatch;

public class UploadMain {

    public static void main(String[] args) {
        final var database = new SqliteDatabase();
        database.connect();

        final var allModules = new Module();
        final var fibers = allModules.register(new FibersModule());
        final var channel = new MemoryChannel<UploadAttemptResult>();
        final var workflow = new ResultsWorkflow(channel, fibers.resultFiber);

        allModules.register(new UploadWorkflow(fibers.runnerFiber, database, channel));
        allModules.start();
        workflow.waitUntilDone();
        allModules.stop();
    }

    static class FibersModule extends Module {
        public final Fiber resultFiber;
        public final Fiber runnerFiber;

        FibersModule() {
            resultFiber = register(new ThreadFiber());
            runnerFiber = register(new ThreadFiber());
        }
    }

    static class UploadWorkflow extends Module {
        private final Fiber fiber;
        private final UploadProcess upload;

        UploadWorkflow(Fiber fiber, SqliteDatabase database, Channel<UploadAttemptResult> channel) {
            this.fiber = fiber;
            final var uploader = new GlacierUploader(channel);
            upload = new UploadProcess("photos", database, uploader, channel, fiber);
            uploader.init();
        }

        @Override
        public void start() {
            super.start();
            fiber.execute(upload);
        }
    }

    static class ResultsWorkflow {
        final CountDownLatch latch;

        ResultsWorkflow(Channel<UploadAttemptResult> channel, Fiber fiber) {
            latch = new CountDownLatch(1);
            channel.subscribe(fiber, result -> {
                if (result.status == UploadAttemptResult.Status.Done) {
                    latch.countDown();
                }
           });
        }

        public void waitUntilDone() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
