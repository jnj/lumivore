package org.joshjoyce.lumivore.db;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class SqliteDatabase {

    private static final Consumer<PreparedStatement> NO_OP = s -> {};

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection conn;
    private boolean connected = false;

    private static void forEachRow(ResultSet rs, Consumer<ResultSet> consumer) {
        try {
            while (rs.next()) {
                consumer.accept(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Consumer<PreparedStatement> safeConsumer(Consumer<PreparedStatement> consumer) {
        return ps -> {
            try {
                consumer.accept(ps);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static <A> Function<ResultSet, A> safeMapper(Function<ResultSet, A> mapper) {
        return rs -> {
            try {
                return mapper.apply(rs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public void connect() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:photos.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        connected = true;
    }

    private void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException("not connected");
        }
    }

    private <A> void updateWithPreparedStatement(String sql, Function<PreparedStatement, A> f) {
        try (var stmt = conn.prepareStatement(sql)) {
            f.apply(stmt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <A> List<A> mapResults(ResultSet r, Function<ResultSet, A> f) {
        var results = new ArrayList<A>();

        try {
            while (r.next()) {
                var x = f.apply(r);
                results.add(x);
            }

            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeUpdate(String sql) {
        withStatement(s -> {
            try {
                return s.executeUpdate(sql);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <A> A withStatement(Function<Statement, A> f) {
        try (Statement stmt = conn.createStatement()) {
            return f.apply(stmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <A> void executeWithPreparedStatement(String sql, Function<PreparedStatement, A> f) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            f.apply(stmt);
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createTables() {
        ensureConnected();
        var sql =
                "DROP TABLE IF EXISTS SYNCS; " +
                "CREATE TABLE SYNCS (PATH TEXT UNIQUE, SHA1 TEXT UNIQUE, SYNC_TIME INTEGER); " +
                "DROP TABLE IF EXISTS GLACIER_UPLOADS; " +
                "CREATE TABLE GLACIER_UPLOADS (HASH TEXT PRIMARY KEY, VAULT TEXT, ARCHIVE_ID TEXT); " +
                "DROP TABLE IF EXISTS EXTENSIONS; " +
                "CREATE TABLE EXTENSIONS (EXTENSION TEXT UNIQUE); " +
                "DROP TABLE IF EXISTS INDEXED_PATHS; " +
                "CREATE TABLE INDEXED_PATHS (PATH TEXT UNIQUE); " +
                "DROP TABLE IF EXISTS CONTENT_CHANGES; " +
                "CREATE TABLE CONTENT_CHANGES (PATH TEXT, OLD_HASH TEXT, NEW_HASH TEXT, UNIQUE (OLD_HASH, NEW_HASH) ); " +
                "DROP TABLE IF EXISTS DUPLICATES; " +
                "CREATE TABLE DUPLICATES (PATH TEXT PRIMARY KEY) ; ";
        executeUpdate(sql);
    }

    public void createIndexes() {
        ensureConnected();
        var sql =
                "CREATE INDEX IF NOT EXISTS SYNC_PATH_INDEX ON SYNCS(PATH); " +
                "CREATE INDEX IF NOT EXISTS SYNC_SHA1_INDEX ON SYNCS (SHA1); " +
                "CREATE INDEX IF NOT EXISTS UPLOADS_HASH_INDEX ON GLACIER_UPLOADS (HASH); " +
                "CREATE INDEX IF NOT EXISTS CONTENT_CHG_INDEX ON CONTENT_CHANGES (OLD_HASH, NEW_HASH); ";
        executeUpdate(sql);
    }

    public void insertSync(String path, String sha1) {
        ensureConnected();
        executeWithPreparedStatement("INSERT INTO SYNCS(PATH, SHA1, SYNC_TIME) VALUES (?, ?, ?);",
                s -> {
                    try {
                        s.setString(1, path);
                        s.setString(2, sha1);
                        s.setLong(3, System.currentTimeMillis());
                        return null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void insertDup(String path) {
        ensureConnected();
        executeWithPreparedStatement("INSERT INTO DUPLICATES(PATH) VALUES (?);",
                s -> {
                    try {
                        s.setString(1, path);
                        return null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void insertContentChange(String path, String oldHash, String newHash) {
        ensureConnected();
        executeWithPreparedStatement("INSERT INTO CONTENT_CHANGES (PATH, OLD_HASH, NEW_HASH) VALUES (?,?,?);",
                s -> {
                    try {
                        s.setString(1, path);
                        s.setString(2, oldHash);
                        s.setString(3, newHash);
                        return null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void updateSync(String path, String sha1) {
        ensureConnected();
        var now = System.currentTimeMillis();
        updateWithPreparedStatement("UPDATE SYNCS SET SHA1 = ?, SYNC_TIME = ? WHERE PATH = ?;",
                ps -> {
                    try {
                        ps.setString(1, sha1);
                        ps.setLong(2, now);
                        ps.setString(3, path);
                        return null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public void insertUpload(String hash, String vault, String archiveId) {
        ensureConnected();
        executeWithPreparedStatement("INSERT INTO GLACIER_UPLOADS(HASH, VAULT, ARCHIVE_ID) VALUES (?, ?, ?);",
                s -> {
                    try {
                        s.setString(1, hash);
                        s.setString(2, vault);
                        s.setString(3, archiveId);
                        return null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public Sync getSync(final String path) {
        ensureConnected();
        Consumer<PreparedStatement> bindVals = safeConsumer(s -> {
            try {
                s.setString(1, path);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        Function<ResultSet, Sync> mapFn =
                rs -> {
                    try {
                        var sync = new Sync();
                        sync.s = rs.getString("PATH");
                        sync.t = rs.getString("SHA1");
                        sync.l = rs.getLong("SYNC_TIME");
                        return sync;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                };

        List<Sync> as = withQuery("SELECT PATH, SHA1, SYNC_TIME FROM SYNCS WHERE PATH = ? ;", bindVals, mapFn);
        return as.get(0);
    }

    private <A> List<A> withQuery(String sql, Consumer<PreparedStatement> bindVals, Function<ResultSet, A> mapper) {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            bindVals.accept(stmt);
            return withResultSet(stmt.executeQuery(), mapper);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private <A> List<A> withResultSet(ResultSet rs, Function<ResultSet, A> mapper) {
        List<A> results = new ArrayList<>();

        forEachRow(rs, r -> {
            var a = mapper.apply(rs);
            results.add(a);
        });

        return results;
    }

    public List<Sync> getSyncs() {
        ensureConnected();

        return withQuery("SELECT PATH, SHA1, SYNC_TIME FROM SYNCS;", NO_OP, r -> {
            try {
                var sync = new Sync();
                sync.s = r.getString("PATH");
                sync.t = r.getString("SHA1");
                sync.l = r.getLong("SYNC_TIME");
                return sync;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<String> getDuplicates() {
        ensureConnected();
        return withQuery("SELECT PATH FROM DUPLICATES;", NO_OP, r -> {
            try {
                return r.getString("PATH");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void getGlacierUploads() {
        ensureConnected();
        withQuery("SELECT HASH, VAULT, ARCHIVE_ID FROM GLACIER_UPLOADS;", NO_OP, rs ->
                mapResults(rs, r -> {
                    try {
                        var u = new Upload();
                        u.hash = r.getString("HASH");
                        u.vault = r.getString("VAULT");
                        u.uploadId = r.getString("ARCHIVE_ID");
                        return u;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    public void addExtension(String ext) {
        ensureConnected();
        executeWithPreparedStatement("INSERT INTO EXTENSIONS(EXTENSION) VALUES (?);", ps -> {
            try {
                ps.setString(1, ext);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Set<String> getExtensions() {
        ensureConnected();
        var exts = withQuery("SELECT EXTENSION FROM EXTENSIONS;", NO_OP, rs -> {
            try {
                return rs.getString("EXTENSION");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return new HashSet<>(exts);
    }

    public List<String> getWatchedDirectories() {
        ensureConnected();
        return withQuery("SELECT PATH FROM INDEXED_PATHS;", NO_OP, rs -> {
            try {
                return rs.getString("PATH");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addWatchedDirectory(Path path) {
        ensureConnected();
        executeWithPreparedStatement("INSERT INTO INDEXED_PATHS(PATH) VALUES (?);", ps -> {
            try {
                ps.setString(1, path.toString());
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static class Sync {
        public String path;
        public String hash;
        public long time;
    }

    public static class Upload {
        public String hash;
        public String vault;
        public String uploadId;
    }
}
