package org.joshjoyce.lumivore;

import java.io.File;

import org.fusesource.scalate.TemplateEngine;
import org.fusesource.scalate.layout.DefaultLayoutStrategy;
import org.joshjoyce.lumivore.db.SqliteDatabase;

public class Main {
    public static void main(String[] args) {
        var templateEngine = new TemplateEngine(Seq("src/main/webapp").map(new File(_)))
        templateEngine.layoutStrategy = new DefaultLayoutStrategy(templateEngine, "/WEB-INF/layouts/default.scaml")
        var database = new SqliteDatabase
        database.connect();
        var lum = new Lumivore(8080, templateEngine, database);
        lum.start();
    }
}
