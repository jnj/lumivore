package org.joshjoyce.lumivore.sync;

import org.apache.log4j.Logger;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.core.RunnableExecutorImpl;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.ThreadFiber;
import org.joshjoyce.lumivore.db.SqliteDatabase;
import org.joshjoyce.lumivore.io.HashUtils;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SyncMain {
    private static Logger log = Logger.getLogger(SyncMain.class);

    public static void main(String[] args) {
        var database = new SqliteDatabase();
        database.connect();

        var syncChannel = new MemoryChannel<SyncCheckResult>();
        Fiber fiber = new ThreadFiber(new RunnableExecutorImpl(), "MainFiber", false);
        fiber.start();

        var watchedDirectories = database.getWatchedDirectories();
        var latch = new CountDownLatch(watchedDirectories.size());
        System.out.println("instantiated with latch size = " + watchedDirectories.size());

        var sub = syncChannel.subscribe(fiber, m -> {
            var path = m.path;
            var oldHash = m.oldHash;
            var hash = m.hash;

            switch (m.status) {
                case Unseen: {
                    var sha1 = HashUtils.hashContents(path);
                    try {
                        database.insertSync(path.toString(), sha1);
                        log.info("inserted " + path + " -> " + sha1);
                    } catch (Exception e) {
                        log.warn("DUPLICATED FILE FOUND: " + path.toString());
                        try {
                            database.insertDup(path.toString());
                        } catch (Exception ee) {
                            log.warn("exception while inserting duplicate", ee);
                        }
                    }
                }
                return;
                case ContentsChanged: {
                    log.info(String.format("Contents changed %s %s -> %s", path.toString(), oldHash, hash));
                    try {
                        database.insertContentChange(path.toString(), oldHash, hash);
                        database.updateSync(path.toString(), hash);
                    } catch (Exception e) {
                        log.error("Error attempting to update path " + path.toString() + " old hash " + oldHash + " new hash " + hash, e);
                    }
                }
                return;
                case SyncDone: {
                    System.out.println("SyncDone received");
                    latch.countDown();
                }
            }
        });

        var executor = Executors.newFixedThreadPool(2);
        var callables = watchedDirectories.stream().map(arg -> {
            var sync = new SyncStream(database);
            sync.addObserver(syncChannel);
            return (Callable<Object>) () -> {
                sync.check(Paths.get(arg));
                return null;
            };
        }).collect(Collectors.toList());

        List<Future<Object>> futures;
        try {
            futures = executor.invokeAll(callables);
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Done scanning");
        callables = null;
        futures = null;
        executor.shutdown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("shutting down sub");
        sub.dispose();
        System.out.println("shutting down fiber");
        fiber.dispose();
    }

    private static boolean isConstraintViolation(SQLException e) {
        return e.getMessage().toUpperCase().contains("CONSTRAINT");
    }
}
