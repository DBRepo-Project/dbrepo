package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Version identities refer to native rows; only versions missing from native history need stored values. */
public final class TupleVersionHistory {
    private TupleVersionHistory() { }

    public static void prepare(Connection c, Table table) throws SQLException {
        if (!c.getAutoCommit()) throw new SQLException("Prepare version history before the data transaction");
        new ReplicationTimestampServiceMariaDbImpl().ensureTableExists(c);
        try (var ddl = c.createStatement()) {
            ddl.execute("""
                    CREATE TABLE IF NOT EXISTS tuple_replication_versions (
                        table_id VARCHAR(36) NOT NULL, replication_id VARCHAR(255) NOT NULL,
                        version_id VARCHAR(36) NOT NULL, native_start TIMESTAMP(6) NOT NULL,
                        PRIMARY KEY(table_id, version_id), UNIQUE KEY native_version(table_id, replication_id, native_start)
                    ) ENGINE=InnoDB
                    """);
            ddl.execute("CREATE TABLE IF NOT EXISTS tuple_visibility_counter"
                    + " (id INT PRIMARY KEY, sequence BIGINT NOT NULL) ENGINE=InnoDB");
            ddl.execute("INSERT IGNORE INTO tuple_visibility_counter VALUES(1,0)");
            ddl.execute("SET time_zone='+00:00'");
        }
    }

    public static String importedName(Table table) {
        return "_dbrepo_versions_" + table.getId().toString().replace("-", "");
    }

    public static void prepareImported(Connection c, Table table) throws SQLException {
        final String columns = String.join(",", table.getColumns().stream().map(x -> quote(x.getInternalName())).toList());
        try (var ddl = c.createStatement()) {
            ddl.execute("CREATE TABLE IF NOT EXISTS " + quote(importedName(table))
                    + " ENGINE=InnoDB AS SELECT " + columns + " FROM " + quote(table.getInternalName()) + " WHERE 0");
            ddl.execute("ALTER TABLE " + quote(importedName(table))
                    + " ADD COLUMN IF NOT EXISTS _version_id VARCHAR(36),"
                    + " ADD UNIQUE INDEX IF NOT EXISTS imported_version (_version_id)");
        }
    }

    public static long cut(Connection c) throws SQLException {
        try (var s = c.prepareStatement("SELECT sequence FROM tuple_visibility_counter WHERE id=1"); var r = s.executeQuery()) {
            if (!r.next()) throw new SQLException("Version visibility counter is missing");
            return r.getLong(1);
        }
    }

    private static long advance(Connection c) throws SQLException {
        try (var s = c.prepareStatement("SELECT sequence FROM tuple_visibility_counter WHERE id=1 FOR UPDATE");
             var r = s.executeQuery()) {
            if (!r.next()) throw new SQLException("Version visibility counter is missing");
            final long next = Math.addExact(r.getLong(1), 1);
            try (var update = c.prepareStatement("UPDATE tuple_visibility_counter SET sequence=? WHERE id=1")) {
                update.setLong(1, next); update.executeUpdate();
            }
            return next;
        }
    }

    public static UUID findVersion(Connection c, UUID tableId, String key, Instant nativeStart) throws SQLException {
        try (var s = c.prepareStatement("SELECT version_id FROM tuple_replication_versions"
                + " WHERE table_id=? AND replication_id=? AND native_start=?")) {
            s.setString(1, tableId.toString()); s.setString(2, key); s.setTimestamp(3, Timestamp.from(nativeStart), utc());
            try (var r = s.executeQuery()) { return r.next() ? UUID.fromString(r.getString(1)) : null; }
        }
    }

    public static UUID legacyVersion(String site, UUID database, UUID table, String key, Instant start) {
        return UUID.nameUUIDFromBytes(("dbrepo:tuple-version:v1:" + site + ":" + database + ":" + table
                + ":" + key.length() + ":" + key + ":" + start).getBytes(StandardCharsets.UTF_8));
    }

    public static void bindNative(Connection c, UUID tableId, TupleWithTimestampsDto tuple) throws SQLException {
        final UUID existing = findVersion(c, tableId, tuple.getReplicationKey(), tuple.getInsertedAt());
        if (existing != null && !existing.equals(tuple.getVersionId())) throw new SQLException("Native version identity conflict");
        try (var s = c.prepareStatement("INSERT IGNORE INTO tuple_replication_versions"
                + " (table_id,replication_id,version_id,native_start) VALUES(?,?,?,?)")) {
            s.setString(1, tableId.toString()); s.setString(2, tuple.getReplicationKey());
            s.setString(3, tuple.getVersionId().toString()); s.setTimestamp(4, Timestamp.from(tuple.getInsertedAt()), utc());
            s.executeUpdate();
        }
    }

    public static void record(Connection c, String site, UUID databaseId, UUID tableId,
                              TupleWithTimestampsDto tuple, HttpMethod method, UUID eventId) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException("Version visibility requires the data transaction");
        if (tuple.getInsertedAt() == null) throw new SQLException("Native version start is required");
        UUID version = HttpMethod.DELETE.equals(method)
                ? findVersion(c, tableId, tuple.getReplicationKey(), tuple.getInsertedAt()) : eventId;
        if (version == null) {
            if (!HttpMethod.DELETE.equals(method)) throw new SQLException("Tuple version identity is missing");
            version = eventId == null ? legacyVersion(site, databaseId, tableId, tuple.getReplicationKey(), tuple.getInsertedAt()) : eventId;
        }
        tuple.setVersionId(version);
        bindNative(c, tableId, tuple);
        final long sequence = advance(c);
        final Instant boundary = HttpMethod.DELETE.equals(method) ? tuple.getDeletedAt() : tuple.getInsertedAt();
        if (boundary == null) throw new SQLException("Version visibility boundary is required");
        if (!HttpMethod.POST.equals(method)) {
            try (var close = c.prepareStatement("""
                    UPDATE tuple_replication_timestamps SET row_end=?,visibility_end=?
                    WHERE site_url=? AND database_id=? AND table_id=? AND replication_id=?
                        AND row_start <= ? AND (row_end IS NULL OR row_end > ?)
                    """)) {
                close.setTimestamp(1, Timestamp.from(boundary), utc()); close.setLong(2, sequence);
                close.setString(3, site); close.setString(4, databaseId.toString()); close.setString(5, tableId.toString());
                close.setString(6, tuple.getReplicationKey()); close.setTimestamp(7, Timestamp.from(boundary), utc());
                close.setTimestamp(8, Timestamp.from(boundary), utc()); close.executeUpdate();
            }
        }
        Long start = sequence;
        if (HttpMethod.DELETE.equals(method)) {
            try (var s = c.prepareStatement("SELECT visibility_start FROM tuple_replication_timestamps"
                    + " WHERE site_url=? AND database_id=? AND table_id=? AND version_id=?")) {
                s.setString(1, site); s.setString(2, databaseId.toString()); s.setString(3, tableId.toString());
                s.setString(4, version.toString());
                try (var r = s.executeQuery()) { start = r.next() ? (Long) r.getObject(1) : null; }
            }
        }
        tuple.setVisibilityStart(start);
        tuple.setVisibilityEnd(HttpMethod.DELETE.equals(method) ? sequence : null);
        ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, TupleReplicationTimestampDto.builder()
                .siteUrl(site).databaseId(databaseId).tableId(tableId).replicationId(tuple.getReplicationKey())
                .versionId(version).rowStart(tuple.getInsertedAt()).rowEnd(tuple.getDeletedAt())
                .visibilityStart(start).visibilityEnd(tuple.getVisibilityEnd()).build());
    }

    public static Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }
    public static String quote(String name) { return "`" + name.replace("`", "``") + "`"; }
}
