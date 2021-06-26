package org.joshjoyce.lumivore.sync;

import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.io.File;
import java.nio.file.Files;

public class UploadLogImporter {
    private static final String key = "INFO  GlacierUploader - ";
    private static final int keyLen = key.length();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("No file given");
            System.exit(-1);
        }
        var filename = args[0];
        var database = new SqliteDatabase();
        database.connect();

        var lines = Files.readAllLines(new File(filename).toPath());
        lines.forEach(line -> {
            if (line.contains(key) && line.contains("/media")) {
                var pipeIndex = line.indexOf('|', keyLen);
                var pathStart = line.indexOf("/");
                if (pathStart >= 0)
                {
                    var path = line.substring(pathStart, pipeIndex);
                    var archiveId = line.substring(pipeIndex + 1);
                    var syncs = database.getSync(path);
                    //System.out.println(path);

                    if (syncs != null)
                    {
                        try
                        {
                            database.insertUpload(syncs.hash, "photos", archiveId);
                        } catch (Exception e)
                        {
                            if (!e.getMessage().contains("CONSTRAINT"))
                            {
                                e.printStackTrace();
                            }
                        }
                    } else
                    {
                        System.out.println("NOSYNC|" + path);
                    }
                }
            }
        });
    }
}

