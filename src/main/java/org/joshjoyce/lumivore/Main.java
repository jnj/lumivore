package org.joshjoyce.lumivore;

import org.joshjoyce.lumivore.db.SqliteDatabase;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        var database = new SqliteDatabase();
        database.connect();

        String line = null;
        var scanner = new Scanner(System.in);
        printMenu();
        while ((line = scanner.nextLine()) != null) {
            var trimmed = line.trim();
            switch (trimmed) {
                case "1":
                    System.out.println("Performing sync...");
                    break;
                case "2":
                    System.out.println("Performing upload...");
                    break;
                default:
                    continue;
            }
        }
    }

    static void printMenu() {
        System.out.println("SELECT:\n" +
                           "-------\n" +
                           "(1) Sync\n" +
                           "(2) Upload");
    }
}
