package at.ac.tuwien.ifs.dbrepo.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HexFormat;

/** Durable, name-scoped receipts for replicated creation only; never called inside a tuple transaction. */
final class ReplicaDdl {
    private static final String RECEIPTS = "dbrepo_replication_ddl";

    private ReplicaDdl() {
    }

    static void createDatabase(Connection connection, String schema, String identity) throws SQLException {
        if (!schema.matches("replica_[a-f0-9]{32}")) {
            throw new SQLException("Replicated database creation requires a fixed replication name");
        }
        lock(connection, schema);
        try {
            if (!tableExists(connection, schema, RECEIPTS)) {
                try (var statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ?")) {
                    statement.setString(1, schema);
                    try (var rows = statement.executeQuery()) {
                        rows.next();
                        if (rows.getLong(1) != 0) {
                            throw new SQLException("Cannot adopt an existing database without a creation receipt");
                        }
                    }
                }
            }
            execute(connection, "CREATE DATABASE IF NOT EXISTS " + quote(schema));
            prepare(connection, schema);
            reserve(connection, schema, "", identity);
        } finally {
            unlock(connection, schema);
        }
    }

    static void createTable(Connection connection, String schema, String name, String sql) throws SQLException {
        lock(connection, schema);
        try {
            prepare(connection, schema);
            if (!hasReceipt(connection, schema, name) && tableExists(connection, schema, name)) {
                throw new SQLException("Table already exists without a replication creation receipt", "42S01", 1050);
            }
            reserve(connection, schema, name, sql);
            if (!tableExists(connection, schema, name)) {
                execute(connection, sql);
            }
        } finally {
            unlock(connection, schema);
        }
    }

    static boolean isReplicaDatabase(Connection connection, String schema) throws SQLException {
        return tableExists(connection, schema, RECEIPTS) && hasReceipt(connection, schema, "");
    }

    static void initialize(Connection connection, String sql, boolean resumable) throws SQLException {
        try {
            execute(connection, sql);
        } catch (SQLException e) {
            // Only already-created query-store tables/procedures are resumable, never other SQL failures.
            if (!resumable || (e.getErrorCode() != 1050 && e.getErrorCode() != 1304)) {
                throw e;
            }
        }
    }

    private static void prepare(Connection connection, String schema) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS " + quote(schema) + "." + RECEIPTS
                + " (object_name VARCHAR(64) COLLATE utf8mb4_bin PRIMARY KEY, payload_hash CHAR(64) NOT NULL)"
                + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private static boolean hasReceipt(Connection connection, String schema, String name) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT 1 FROM " + quote(schema)
                + "." + RECEIPTS + " WHERE object_name = ?")) {
            statement.setString(1, name);
            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static void reserve(Connection connection, String schema, String name, String payload) throws SQLException {
        final String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (var insert = connection.prepareStatement("INSERT IGNORE INTO " + quote(schema)
                + "." + RECEIPTS + " (object_name, payload_hash) VALUES (?, ?)")) {
            insert.setString(1, name);
            insert.setString(2, hash);
            insert.executeUpdate();
        }
        try (var select = connection.prepareStatement("SELECT payload_hash FROM " + quote(schema)
                + "." + RECEIPTS + " WHERE object_name = ?")) {
            select.setString(1, name);
            try (var rows = select.executeQuery()) {
                if (!rows.next() || !hash.equals(rows.getString(1))) {
                    throw new SQLException("Replication creation name was reused with a different payload");
                }
            }
        }
        connection.commit();
    }

    private static boolean tableExists(Connection connection, String schema, String name) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
            statement.setString(1, schema);
            statement.setString(2, name);
            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private static void lock(Connection connection, String schema) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT GET_LOCK(?, 30)")) {
            statement.setString(1, "dbrepo-create:" + schema);
            try (var rows = statement.executeQuery()) {
                if (!rows.next() || rows.getInt(1) != 1) {
                    throw new SQLException("Timed out locking replicated DDL");
                }
            }
        }
    }

    private static void unlock(Connection connection, String schema) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, "dbrepo-create:" + schema);
            statement.execute();
        }
    }
}
