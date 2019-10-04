package org.joshjoyce.lumivore.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String hashContents(Path path) {
        try {
            byte[] sha1Bytes;

            try (var in = new FileInputStream(path.toFile());
                 var bis = new BufferedInputStream(in);
                 var dis = new DigestInputStream(bis, digest)) {
                var buffer = new byte[8192];

                while (dis.read(buffer) != -1) {
                    // nothing to do
                }

                sha1Bytes = HashUtils.digest.digest();
            }
            var hash = bytes2Hex(sha1Bytes);
            HashUtils.digest.reset();
            return hash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytes2Hex(byte[] hashInBytes) {
        var sb = new StringBuilder();

        for (var aByte : hashInBytes) {
            var hex = Integer.toHexString(0xff & aByte);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }

        return sb.toString();

    }
}
