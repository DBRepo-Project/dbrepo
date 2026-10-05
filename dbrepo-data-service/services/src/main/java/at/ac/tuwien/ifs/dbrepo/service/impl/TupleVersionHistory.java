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
        final String columns = String.join(",", columns(c, table).keySet().stream().map(TupleVersionHistory::quote).toList());
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

    /** Only source-native periods can be recovered without guessing a replica's processing time. */
    public static void backfillSource(Connection c, String site, UUID databaseId, Table table) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException("History backfill requires a transaction");
        final String name = quote(table.getInternalName());
        try (var s = c.prepareStatement("SELECT h.replication_key,h.ROW_START,h.ROW_END,c.ROW_START IS NOT NULL"
                + " FROM " + name + " FOR SYSTEM_TIME ALL h LEFT JOIN " + name
                + " c ON h.replication_key=c.replication_key AND h.ROW_START=c.ROW_START"
                + " ORDER BY h.replication_key,h.ROW_START"); var rows = s.executeQuery()) {
            while (rows.next()) {
                final String key = rows.getString(1);
                if (key == null) continue;
                final Instant start = rows.getTimestamp(2, utc()).toInstant();
                UUID version = findVersion(c, table.getId(), key, start);
                if (version == null) version = legacyVersion(site, databaseId, table.getId(), key, start);
                final var tuple = TupleWithTimestampsDto.builder().replicationKey(key).versionId(version)
                        .insertedAt(start).deletedAt(rows.getBoolean(4) ? null : rows.getTimestamp(3, utc()).toInstant()).build();
                bindNative(c, table.getId(), tuple);
                ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, TupleReplicationTimestampDto.builder()
                        .siteUrl(site).databaseId(databaseId).tableId(table.getId()).replicationId(key).versionId(version)
                        .rowStart(start).rowEnd(tuple.getDeletedAt()).build());
            }
        }
    }

    public static List<TupleReplicationTimestampDto> visibility(Connection c, UUID version) throws SQLException {
        final List<TupleReplicationTimestampDto> result = new ArrayList<>();
        try (var s = c.prepareStatement("SELECT * FROM tuple_replication_timestamps WHERE version_id=?"
                + " ORDER BY site_url,database_id,table_id,row_start")) {
            s.setString(1, version.toString());
            try (var rows = s.executeQuery()) {
                while (rows.next()) {
                    final Timestamp end = rows.getTimestamp("row_end", utc());
                    result.add(TupleReplicationTimestampDto.builder().versionId(version)
                            .siteUrl(rows.getString("site_url")).databaseId(UUID.fromString(rows.getString("database_id")))
                            .tableId(UUID.fromString(rows.getString("table_id"))).replicationId(rows.getString("replication_id"))
                            .rowStart(rows.getTimestamp("row_start", utc()).toInstant()).rowEnd(end == null ? null : end.toInstant())
                            .visibilityStart((Long) rows.getObject("visibility_start"))
                            .visibilityEnd((Long) rows.getObject("visibility_end")).build());
                }
            }
        }
        return result;
    }

    public static void retain(Connection c, Table table, UUID version, Map<String, Object> values) throws SQLException {
        final Map<String,String> columns = columns(c, table);
        final String names = String.join(",", columns.keySet().stream().map(TupleVersionHistory::quote).toList());
        final String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        final String predicates = String.join(" AND ", columns.entrySet().stream().map(x -> {
            final boolean text = x.getValue().contains("char") || x.getValue().contains("text")
                    || x.getValue().startsWith("enum") || x.getValue().startsWith("set");
            return (text ? "BINARY " : "") + quote(x.getKey()) + " <=> " + (text ? "BINARY " : "") + "?";
        }).toList());
        try (var insert = c.prepareStatement("INSERT IGNORE INTO " + quote(importedName(table))
                + " (" + names + ",_version_id) VALUES(" + placeholders + ",?)")) {
            int index = bindValues(insert, columns, values);
            insert.setString(index, version.toString()); insert.executeUpdate();
        }
        try (var check = c.prepareStatement("SELECT 1 FROM " + quote(importedName(table))
                + " WHERE " + predicates + " AND _version_id=?")) {
            int index = bindValues(check, columns, values); check.setString(index, version.toString());
            try (var r = check.executeQuery()) { if (!r.next()) throw new SQLException("Historical values version conflict"); }
        }
    }

    private static Map<String,String> columns(Connection c, Table table) throws SQLException {
        final Map<String,String> result = new LinkedHashMap<>();
        try (var s = c.prepareStatement("SELECT COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? ORDER BY ORDINAL_POSITION")) {
            s.setString(1, table.getInternalName());
            try (var rows = s.executeQuery()) {
                while (rows.next()) {
                    if (!rows.getString(1).equalsIgnoreCase("ROW_START") && !rows.getString(1).equalsIgnoreCase("ROW_END")) {
                        result.put(rows.getString(1), rows.getString(2));
                    }
                }
            }
        }
        if (result.isEmpty()) throw new SQLException("Native history relation is missing");
        return result;
    }

    private static int bindValues(PreparedStatement s, Map<String,String> columns, Map<String,Object> values) throws SQLException {
        int index = 1;
        for (var column : columns.entrySet()) {
            Object value = values.get(column.getKey());
            final String type = column.getValue();
            if (value != null && (type.contains("blob") || type.contains("binary") || type.startsWith("bit("))) {
                s.setBytes(index++, value instanceof byte[] bytes ? bytes : Base64.getDecoder().decode(value.toString()));
            } else s.setObject(index++, value);
        }
        return index;
    }

    public static Calendar utc() { return Calendar.getInstance(TimeZone.getTimeZone("UTC")); }
    public static String quote(String name) { return "`" + name.replace("`", "``") + "`"; }
}
