package org.joshjoyce.lumivore.io;

import javax.xml.bind.DatatypeConverter;
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
            DigestInputStream dis = new DigestInputStream(new BufferedInputStream(new FileInputStream(path.toFile())), digest);
            byte[] buffer = new byte[8192];

            while (dis.read(buffer) != -1) {
                // nothing to do
            }

            byte[] sha1Bytes = HashUtils.digest.digest();
            dis.close();
            String hash = DatatypeConverter.printHexBinary(sha1Bytes);
            HashUtils.digest.reset();
            return hash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
