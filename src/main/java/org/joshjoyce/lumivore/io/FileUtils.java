package org.joshjoyce.lumivore.io;

import java.io.File;

public class FileUtils {
    public static long getSizeInBytes(String path) {
        var file = new File(path);
        return file.length();
    }

    public static void main(String[] args) {
        System.out.println(getSizeInBytes("/media/josh/wdgreen1tb/backup/20170805/20170805.tar.gz"));
    }
}
