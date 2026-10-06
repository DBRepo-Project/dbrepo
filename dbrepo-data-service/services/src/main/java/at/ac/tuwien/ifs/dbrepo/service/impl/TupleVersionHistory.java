package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Version identities refer to native rows; only versions missing from native history need stored values. */
public final class TupleVersionHistory {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private TupleVersionHistory() { }

    public static void prepare(Connection c, Table table) throws SQLException {
        if (!c.getAutoCommit()) throw new SQLException("Prepare version history before the data transaction");
        new ReplicationTimestampServiceMariaDbImpl().ensureTableExists(c);
        try (var ddl = c.createStatement()) {
            ddl.execute("""
                    CREATE TABLE IF NOT EXISTS tuple_replication_versions (
                        table_id VARCHAR(36) NOT NULL, replication_id VARCHAR(255) NOT NULL,
                        master_site_ts TIMESTAMP(6) NULL, native_start TIMESTAMP(6) NOT NULL,
                        PRIMARY KEY(table_id, replication_id, native_start),
                        UNIQUE KEY master_version(table_id, replication_id, master_site_ts)
                    ) ENGINE=InnoDB
                    """);
            if (hasColumn(c, "tuple_replication_versions", "version_id")
                    && !hasColumn(c, "tuple_replication_versions", "master_site_ts")) {
                ddl.execute("ALTER TABLE tuple_replication_versions ADD COLUMN IF NOT EXISTS master_site_ts TIMESTAMP(6) NULL,"
                        + " MODIFY version_id VARCHAR(36) NULL, DROP PRIMARY KEY,"
                        + " ADD PRIMARY KEY(table_id,replication_id,native_start),"
                        + " ADD UNIQUE INDEX IF NOT EXISTS master_version(table_id,replication_id,master_site_ts)");
            }
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
                    + " ADD COLUMN IF NOT EXISTS _master_site_ts TIMESTAMP(6) NULL,"
                    + " ADD UNIQUE INDEX IF NOT EXISTS imported_master_version (replication_key,_master_site_ts)");
        }
    }

    private static boolean hasColumn(Connection c, String table, String column) throws SQLException {
        try (var columns = c.getMetaData().getColumns(c.getCatalog(), null, table, column)) { return columns.next(); }
    }

    public static Instant requireMasterTimestamp(Instant value) throws SQLException {
        if (value == null || !value.equals(value.truncatedTo(java.time.temporal.ChronoUnit.MICROS))) {
            throw new SQLException("Master TS_added must be present with UTC microsecond precision");
        }
        return value;
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

    public static Instant findVersion(Connection c, UUID tableId, String key, Instant nativeStart) throws SQLException {
        try (var s = c.prepareStatement("SELECT master_site_ts FROM tuple_replication_versions"
                + " WHERE table_id=? AND replication_id=? AND native_start=?")) {
            s.setString(1, tableId.toString()); s.setString(2, key); s.setTimestamp(3, Timestamp.from(nativeStart), utc());
            try (var r = s.executeQuery()) {
                return r.next() && r.getTimestamp(1, utc()) != null ? r.getTimestamp(1, utc()).toInstant() : null;
            }
        }
    }

    public static void bindNative(Connection c, UUID tableId, TupleWithTimestampsDto tuple) throws SQLException {
        requireMasterTimestamp(tuple.getMasterSiteTs());
        final Instant existing = findVersion(c, tableId, tuple.getReplicationKey(), tuple.getInsertedAt());
        if (existing != null && !existing.equals(tuple.getMasterSiteTs())) throw new SQLException("Native version identity conflict");
        try (var s = c.prepareStatement("SELECT replication_id,native_start FROM tuple_replication_versions WHERE table_id=? AND master_site_ts=? AND replication_id=?")) {
            s.setString(1, tableId.toString()); s.setTimestamp(2, Timestamp.from(tuple.getMasterSiteTs()), utc());
            s.setString(3, tuple.getReplicationKey());
            try (var r = s.executeQuery()) {
                if (r.next() && (!tuple.getReplicationKey().equals(r.getString(1))
                        || !tuple.getInsertedAt().equals(r.getTimestamp(2, utc()).toInstant()))) {
                    throw new SQLException("Values version is already bound to a different native row");
                }
            }
        }
        try (var s = c.prepareStatement("INSERT IGNORE INTO tuple_replication_versions"
                + " (table_id,replication_id,master_site_ts,native_start) VALUES(?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE master_site_ts=COALESCE(master_site_ts,VALUES(master_site_ts))")) {
            s.setString(1, tableId.toString()); s.setString(2, tuple.getReplicationKey());
            s.setTimestamp(3, Timestamp.from(tuple.getMasterSiteTs()), utc()); s.setTimestamp(4, Timestamp.from(tuple.getInsertedAt()), utc());
            s.executeUpdate();
        }
        if (!tuple.getMasterSiteTs().equals(findVersion(c, tableId, tuple.getReplicationKey(), tuple.getInsertedAt()))) {
            throw new SQLException("Conflicting master timestamp for native values version");
        }
    }

    public static void record(Connection c, String site, UUID databaseId, UUID tableId,
                              TupleWithTimestampsDto tuple, HttpMethod method, Instant masterSiteTs) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException("Version visibility requires the data transaction");
        if (tuple.getInsertedAt() == null) throw new SQLException("Native version start is required");
        final Instant mapped = findVersion(c, tableId, tuple.getReplicationKey(), tuple.getInsertedAt());
        final Instant version = HttpMethod.DELETE.equals(method) && mapped != null ? mapped : masterSiteTs;
        if (version != null || !HttpMethod.DELETE.equals(method)) requireMasterTimestamp(version);
        tuple.setMasterSiteTs(version);
        // Legacy deletes may close known local visibility without proving a source version.
        if (version != null) bindNative(c, tableId, tuple);
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
                    + " WHERE site_url=? AND database_id=? AND table_id=? AND replication_id=? AND row_start=?")) {
                s.setString(1, site); s.setString(2, databaseId.toString()); s.setString(3, tableId.toString());
                s.setString(4, tuple.getReplicationKey()); s.setTimestamp(5, Timestamp.from(tuple.getInsertedAt()), utc());
                try (var r = s.executeQuery()) { start = r.next() ? (Long) r.getObject(1) : null; }
            }
        }
        tuple.setVisibilityStart(start);
        tuple.setVisibilityEnd(HttpMethod.DELETE.equals(method) ? sequence : null);
        ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, TupleReplicationTimestampDto.builder()
                .siteUrl(site).databaseId(databaseId).tableId(tableId).replicationId(tuple.getReplicationKey())
                .masterSiteTs(version).rowStart(tuple.getInsertedAt()).rowEnd(tuple.getDeletedAt())
                .visibilityStart(start).visibilityEnd(tuple.getVisibilityEnd()).build());
    }

    /** Only source-native periods can be recovered without guessing a replica's processing time. */
    public static void backfillSource(Connection c, String site, UUID databaseId, Table table) throws SQLException {
        backfillLocal(c, site, databaseId, table, true);
    }

    public static void backfillLocal(Connection c, String site, UUID databaseId, Table table, boolean source) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException("History backfill requires a transaction");
        recoverEventBindings(c, table, source);
        final String name = quote(table.getInternalName());
        try (var s = c.prepareStatement("SELECT h.replication_key,h.ROW_START,h.ROW_END,c.ROW_START IS NOT NULL"
                + " FROM " + name + " FOR SYSTEM_TIME ALL h LEFT JOIN " + name
                + " c ON h.replication_key=c.replication_key AND h.ROW_START=c.ROW_START"
                + " ORDER BY h.replication_key,h.ROW_START"); var rows = s.executeQuery()) {
            while (rows.next()) {
                final String key = rows.getString(1);
                if (key == null) continue;
                final Instant start = rows.getTimestamp(2, utc()).toInstant();
                Instant version = findVersion(c, table.getId(), key, start);
                if (version == null && source) version = start;
                if (version == null) continue;
                final var tuple = TupleWithTimestampsDto.builder().replicationKey(key).masterSiteTs(version)
                        .insertedAt(start).deletedAt(rows.getBoolean(4) ? null : rows.getTimestamp(3, utc()).toInstant()).build();
                bindNative(c, table.getId(), tuple);
                ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, TupleReplicationTimestampDto.builder()
                        .siteUrl(site).databaseId(databaseId).tableId(table.getId()).replicationId(key).masterSiteTs(version)
                        .rowStart(start).rowEnd(tuple.getDeletedAt()).build());
            }
        }
    }

    /** Retained events and applied receipts are evidence; matching values alone are not. */
    private static void recoverEventBindings(Connection c, Table table, boolean source) throws SQLException {
        final String journal = source ? "tuple_replication_notification_outbox" : "tuple_replication_inbox";
        try (var tables = c.getMetaData().getTables(c.getCatalog(), null, journal, new String[]{"TABLE"})) {
            if (!tables.next()) return;
        }
        final String sql = source ? "SELECT id,http_method,payload,NULL AS receipt FROM " + journal + " WHERE table_id=?"
                : "SELECT event_id,JSON_UNQUOTE(JSON_EXTRACT(payload,'$.method')),payload,receipt FROM " + journal + " WHERE table_id=?";
        try (var s = c.prepareStatement(sql)) {
            s.setString(1, table.getId().toString());
            try (var rows = s.executeQuery()) {
                while (rows.next()) {
                    if (!List.of("POST", "PUT").contains(rows.getString(2))) continue;
                    try {
                        final var payload = JSON.readTree(rows.getString(3));
                        final var tuple = source ? JSON.treeToValue(payload.get("tuple"), TupleWithTimestampsDto.class)
                                : JSON.readValue(rows.getString(4), TupleWithTimestampsDto.class);
                        if (tuple == null || Boolean.FALSE.equals(tuple.getApplied()) || tuple.getInsertedAt() == null
                                || tuple.getReplicationKey() == null) continue;
                        final var sent = JSON.treeToValue(payload.get("tuple"), TupleWithTimestampsDto.class);
                        final Instant master = sent.getMasterSiteTs() != null ? sent.getMasterSiteTs() : sent.getInsertedAt();
                        requireMasterTimestamp(master);
                        if (tuple.getMasterSiteTs() != null && !tuple.getMasterSiteTs().equals(master)) {
                            throw new SQLException("Retained receipt has a conflicting master timestamp");
                        }
                        tuple.setMasterSiteTs(master);
                        bindNative(c, table.getId(), tuple);
                    } catch (java.io.IOException | IllegalArgumentException e) {
                        throw new SQLException("Retained event cannot establish a historical version identity", e);
                    }
                }
            }
        }
    }

    public static List<TupleReplicationTimestampDto> visibility(Connection c, Table table, String key, Instant version) throws SQLException {
        final List<TupleReplicationTimestampDto> result = new ArrayList<>();
        final var tableIds = new HashSet<UUID>();
        tableIds.add(table.getId());
        if (table.getReplicaUrls() != null) tableIds.addAll(table.getReplicaUrls().values());
        try (var s = c.prepareStatement("SELECT * FROM tuple_replication_timestamps WHERE master_site_ts=? AND replication_id=? AND table_id IN ("
                + String.join(",", Collections.nCopies(tableIds.size(), "?")) + ")"
                + " ORDER BY site_url,database_id,table_id,row_start")) {
            s.setTimestamp(1, Timestamp.from(version), utc()); s.setString(2, key);
            int index = 3;
            for (UUID id : tableIds) s.setString(index++, id.toString());
            try (var rows = s.executeQuery()) {
                while (rows.next()) {
                    final Timestamp end = rows.getTimestamp("row_end", utc());
                    result.add(TupleReplicationTimestampDto.builder().masterSiteTs(version)
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

    public static void retain(Connection c, Table table, Instant version, Map<String, Object> values) throws SQLException {
        requireMasterTimestamp(version);
        final Map<String,String> columns = columns(c, table);
        final String names = String.join(",", columns.keySet().stream().map(TupleVersionHistory::quote).toList());
        final String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        final String predicates = String.join(" AND ", columns.entrySet().stream().map(x -> {
            final boolean text = x.getValue().contains("char") || x.getValue().contains("text")
                    || x.getValue().startsWith("enum") || x.getValue().startsWith("set");
            return (text ? "BINARY " : "") + quote(x.getKey()) + " <=> " + (text ? "BINARY " : "") + "?";
        }).toList());
        try (var insert = c.prepareStatement("INSERT IGNORE INTO " + quote(importedName(table))
                + " (" + names + ",_master_site_ts) VALUES(" + placeholders + ",?)")) {
            int index = bindValues(insert, columns, values);
            insert.setTimestamp(index, Timestamp.from(version), utc()); insert.executeUpdate();
        }
        try (var check = c.prepareStatement("SELECT 1 FROM " + quote(importedName(table))
                + " WHERE " + predicates + " AND _master_site_ts=?")) {
            int index = bindValues(check, columns, values); check.setTimestamp(index, Timestamp.from(version), utc());
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
