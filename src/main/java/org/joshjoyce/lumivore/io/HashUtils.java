package org.joshjoyce.lumivore.io;

import com.amazonaws.services.glacier.TreeHashGenerator;

import java.nio.file.Path;

public class HashUtils {
    public static String hashContents(Path path) {
        try {
            return TreeHashGenerator.calculateTreeHash(path.toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
