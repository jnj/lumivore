package org.joshjoyce.lumivore.io;

import java.nio.file.Path;

public class HashMain {
    public static void main(String[] args) {
        var hash = HashUtils.hashContents(Path.of(args[0]));
        System.out.println(hash);
    }
}
