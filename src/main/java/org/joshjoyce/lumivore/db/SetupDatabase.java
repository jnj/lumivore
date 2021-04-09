package org.joshjoyce.lumivore.db;

public class SetupDatabase {
    public static void main(String[] args) {
        var db = new SqliteDatabase();
        db.connect();
        db.createTables();
        db.createIndexes();
        //Set("jpg", "dng", "rw2", "orf").foreach(db.addExtension)
        System.out.println("ok");
        System.out.println("foo");
    }
}
