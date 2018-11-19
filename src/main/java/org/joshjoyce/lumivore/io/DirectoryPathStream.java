package org.joshjoyce.lumivore.io;

import java.io.File;
import java.nio.file.*;
import java.util.function.Consumer;

public class DirectoryPathStream {
    private final File dir;
    private final Consumer<Path> visitor;

    public DirectoryPathStream(File dir, Consumer<Path> visitor) {
        this.dir = dir;
        this.visitor = visitor;
    }

    static void recurse(File root, Consumer<Path> f) {
        DirectoryPathStream d = new DirectoryPathStream(root, f);
        d.start();
    }

    public void start() {
        if (dir.isFile()) {
            visitor.accept(dir.toPath());
        } else {
            try {
                DirectoryStream<Path> children = Files.newDirectoryStream(dir.toPath());
                children.forEach(
                    c -> {
                        DirectoryPathStream s = new DirectoryPathStream(c.toFile(), visitor);
                        s.start();
                    }
                );
            } catch (AccessDeniedException e) {
                // skip it
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
