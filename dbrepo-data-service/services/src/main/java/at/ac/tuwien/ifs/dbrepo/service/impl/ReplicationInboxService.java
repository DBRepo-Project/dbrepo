package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotService;
import at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/** The receipt, ordering fence and target mutation share one InnoDB transaction. */
@Service
public class ReplicationInboxService extends DataConnector {
    private final MariaDbMapper mapper;
    private final ObjectMapper json;
    private final String baseUrl;

    public ReplicationInboxService(MariaDbMapper mapper, ObjectMapper json,
                                   @Value("${dbrepo.baseUrl}") String baseUrl) {
        this.mapper = mapper;
        this.json = json.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.baseUrl = baseUrl;
    }

    public TupleWithTimestampsDto apply(Database database, Table table, DataReplicationDto event, HttpMethod method)
            throws SQLException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        validate(database, table, event, method);
        final String payload = encode(Map.of("method", method.name(), "sequence", event.getEventSequence(), "target", table.getId(),
                "database", event.getDatabase().getId(), "table", event.getTable().getId(), "tuple", event.getTuple()));
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection()) {
            prepare(connection);
            TupleVersionHistory.prepare(connection, table);
            TupleVersionHistory.prepareImported(connection, table);
            requireInnoDb(connection, table.getInternalName(), "tuple_replication_inbox", "tuple_replication_heads",
                    "tuple_replication_table_heads");
            connection.setAutoCommit(false);
            try {
                final long baseline = lockTableHead(connection, table, event.getDatabase().getId(), event.getTable().getId());
                lockHead(connection, table, event);
                final TupleWithTimestampsDto receipt = receipt(connection, event, payload);
                if (receipt != null) {
                    connection.commit();
                    return receipt;
                }
                final long head = head(connection, table, event);
                if (head == event.getEventSequence() && baseline < event.getEventSequence()) {
                    throw new TableMalformedException("Different replication events share the same source sequence");
                }
                final TupleWithTimestampsDto result;
                if (!HttpMethod.DELETE.equals(method)) {
                    final UUID version = event.getTuple().getVersionId() == null ? event.getEventId() : event.getTuple().getVersionId();
                    if (!version.equals(event.getEventId())) throw new TableMalformedException("Values version must match its source event");
                    event.getTuple().setVersionId(version);
                }
                if (head > event.getEventSequence() || baseline >= event.getEventSequence()) {
                    if (!HttpMethod.DELETE.equals(method)) retainVersion(connection, table, event.getTuple());
                    result = absent(event);
                } else {
                    result = mutate(connection, database, table, event, method);
                    if (!Boolean.FALSE.equals(result.getApplied())) {
                        TupleVersionHistory.record(connection, baseUrl, database.getId(), table.getId(), result, method,
                                event.getTuple().getVersionId());
                    }
                    advance(connection, table, event);
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO tuple_replication_inbox
                            (event_id, table_id, source_database_id, source_table_id, event_sequence, payload, receipt)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    insert.setString(1, event.getEventId().toString());
                    insert.setString(2, table.getId().toString());
                    insert.setString(3, event.getDatabase().getId().toString());
                    insert.setString(4, event.getTable().getId().toString());
                    insert.setLong(5, event.getEventSequence());
                    insert.setString(6, payload);
                    insert.setString(7, encode(result));
                    insert.executeUpdate();
                }
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | TableMalformedException | StorageUnavailableException
                     | StorageNotFoundException failure) {
                connection.rollback();
                throw failure;
            }
        } finally {
            pool.close();
        }
    }

    public HistorySnapshotDto.Receipt reconcile(Database database, Table table, UUID snapshotId,
                                                HistorySnapshotService snapshots) throws SQLException, IOException {
        try (var pool = getDataSource(database); Connection connection = pool.getConnection()) {
            prepare(connection);
            requireInnoDb(connection, table.getInternalName(), "tuple_replication_inbox", "tuple_replication_heads",
                    "tuple_replication_table_heads");
        }
        return snapshots.reconcileImport(database, table, snapshotId, (connection, targetDatabase, targetTable, envelope) -> {
            final var manifest = envelope.manifest();
            try {
                final long baseline = lockTableHead(connection, table, manifest.sourceDatabaseId(), manifest.sourceTableId());
                if (manifest.boundary() < baseline) {
                    throw new SQLException("Snapshot would regress the installed table baseline");
                }
                String after = null;
                while (true) {
                    final var keys = snapshots.readCurrentKeys(connection, snapshotId, after, HistorySnapshotCodec.MAX_CHUNK_ROWS);
                    if (keys.isEmpty()) break;
                    long chunkIndex = -1;
                    List<HistorySnapshotDto.Row> rows = List.of();
                    for (var key : keys) {
                        if (chunkIndex != key.chunkIndex()) {
                            rows = HistorySnapshotCodec.rows(snapshots.readChunk(connection, snapshotId, key.chunkIndex()), manifest.columns());
                            chunkIndex = key.chunkIndex();
                        }
                        final var row = rows.get(key.rowIndex());
                        final var event = snapshotEvent(manifest, key.replicationKey(), HistorySnapshotCodec.data(manifest, row));
                        lockHead(connection, table, event);
                        if (head(connection, table, event) <= manifest.boundary()) {
                            if (!matchesCurrent(connection, table, event.getTuple().getData())) {
                                mutate(connection, database, table, event, HttpMethod.PUT);
                            }
                            advance(connection, table, event);
                        }
                    }
                    after = keys.getLast().replicationKey();
                }
                // Keyset pages stay bounded; the table fence excludes concurrent delivery until publication.
                after = null;
                while (true) {
                    final List<String> keys = currentKeys(connection, table, after);
                    if (keys.isEmpty()) break;
                    for (String key : keys) {
                        if (!snapshots.containsCurrentKey(connection, snapshotId, key)) {
                            final var event = snapshotEvent(manifest, key, Map.of("replication_key", key));
                            lockHead(connection, table, event);
                            if (head(connection, table, event) <= manifest.boundary()) {
                                mutate(connection, database, table, event, HttpMethod.DELETE);
                                advance(connection, table, event);
                            }
                        }
                    }
                    after = keys.getLast();
                }
                try (var update = connection.prepareStatement(
                        "UPDATE tuple_replication_table_heads SET event_sequence=? WHERE table_id=?")) {
                    update.setLong(1, manifest.boundary());
                    update.setString(2, table.getId().toString());
                    update.executeUpdate();
                }
            } catch (TableMalformedException | StorageUnavailableException | StorageNotFoundException e) {
                throw new SQLException("Failed to reconcile snapshot current state", e);
            }
        });
    }

    public HistorySnapshotDto.Checkpoint checkpoint(Database database, Table table, HistorySnapshotService snapshots)
            throws SQLException {
        final var baseline = snapshots.targetCheckpoint(database, table);
        try (var pool = getDataSource(database); Connection connection = pool.getConnection()) {
            prepare(connection);
            try (var statement = connection.prepareStatement("""
                    SELECT i.event_sequence, i.event_id FROM tuple_replication_inbox i
                    JOIN tuple_replication_table_heads h ON h.table_id=i.table_id
                        AND h.source_database_id=i.source_database_id AND h.source_table_id=i.source_table_id
                    WHERE i.table_id=? ORDER BY i.event_sequence DESC LIMIT 1
                    """)) {
                statement.setString(1, table.getId().toString());
                try (var row = statement.executeQuery()) {
                    if (!row.next()) return baseline;
                    final long sequence = row.getLong(1);
                    final UUID eventId = UUID.fromString(row.getString(2));
                    if (baseline != null && baseline.boundary() == sequence && !eventId.equals(baseline.eventId())) {
                        throw new SQLException("Snapshot and received journal have conflicting restore witnesses");
                    }
                    return baseline != null && baseline.boundary() >= sequence ? baseline
                            : new HistorySnapshotDto.Checkpoint(baseline == null ? null : baseline.epoch(), sequence, eventId);
                }
            }
        }
    }

    private DataReplicationDto snapshotEvent(HistorySnapshotDto.Manifest manifest, String key, Map<String, Object> values) {
        return DataReplicationDto.builder().eventSequence(manifest.boundary())
                .database(at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto.builder().id(manifest.sourceDatabaseId()).build())
                .table(at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto.builder().id(manifest.sourceTableId()).build())
                .tuple(TupleWithTimestampsDto.builder().replicationKey(key).data(values).build()).build();
    }

    private boolean matchesCurrent(Connection connection, Table table, Map<String, Object> values)
            throws SQLException, StorageUnavailableException, StorageNotFoundException {
        final String predicates = table.getColumns().stream().map(column -> switch (column.getColumnType()) {
                    case CHAR, VARCHAR, TINYTEXT, TEXT, MEDIUMTEXT, LONGTEXT, ENUM, SET ->
                            "BINARY " + quote(column.getInternalName()) + " <=> BINARY ?";
                    default -> quote(column.getInternalName()) + " <=> ?";
                })
                .collect(java.util.stream.Collectors.joining(" AND "));
        try (var statement = connection.prepareStatement("SELECT 1 FROM " + quote(table.getInternalName()) + " WHERE " + predicates)) {
            int index = 1;
            for (Column column : table.getColumns()) {
                bind(statement, index++, column, values.get(column.getInternalName()));
            }
            try (var row = statement.executeQuery()) { return row.next(); }
        }
    }

    private void retainVersion(Connection connection, Table table, TupleWithTimestampsDto tuple)
            throws SQLException, StorageUnavailableException, StorageNotFoundException {
        final String names = String.join(",", table.getColumns().stream().map(x -> quote(x.getInternalName())).toList());
        final String placeholders = String.join(",", java.util.Collections.nCopies(table.getColumns().size(), "?"));
        try (var insert = connection.prepareStatement("INSERT IGNORE INTO " + quote(TupleVersionHistory.importedName(table))
                + " (" + names + ",_version_id) VALUES(" + placeholders + ",?)")) {
            int index = 1;
            for (Column column : table.getColumns()) bind(insert, index++, column, tuple.getData().get(column.getInternalName()));
            insert.setString(index, tuple.getVersionId().toString()); insert.executeUpdate();
        }
    }

    private List<String> currentKeys(Connection connection, Table table, String after) throws SQLException {
        final List<String> keys = new ArrayList<>();
        try (var statement = connection.prepareStatement("SELECT replication_key FROM " + quote(table.getInternalName())
                + (after == null ? "" : " WHERE BINARY replication_key > ?") + " ORDER BY BINARY replication_key LIMIT 256")) {
            if (after != null) statement.setString(1, after);
            try (var rows = statement.executeQuery()) { while (rows.next()) keys.add(rows.getString(1)); }
        }
        return keys;
    }

    private void validate(Database database, Table table, DataReplicationDto event, HttpMethod method)
            throws TableMalformedException {
        if (!HttpMethod.POST.equals(method) && !HttpMethod.PUT.equals(method) && !HttpMethod.DELETE.equals(method)) {
            throw new TableMalformedException("Unsupported replicated operation");
        }
        if (event == null || event.getEventId() == null || event.getEventSequence() == null
                || event.getEventSequence() <= 0 || event.getDatabase() == null || event.getDatabase().getId() == null
                || event.getTable() == null || event.getTable().getId() == null || event.getTuple() == null
                || event.getTuple().getData() == null || event.getTuple().getReplicationKey() == null) {
            throw new TableMalformedException("Replication requires a retained source event identity and sequence");
        }
        final String origin = table.getCreationLocation() == null ? database.getCreationLocation() : table.getCreationLocation();
        if (!ReplicationSites.isReplica(origin, baseUrl)
                || event.getDatabase().getCreationLocation() == null
                || ReplicationSites.isReplica(event.getDatabase().getCreationLocation(), origin)
                || (event.getTable().getCreationLocation() != null
                    && ReplicationSites.isReplica(event.getTable().getCreationLocation(), origin))) {
            throw new TableMalformedException("Replicated writes must originate at the configured primary site");
        }
        if (!isTarget(event.getDatabase().getReplicaUrls(), database.getId())
                || !isTarget(event.getTable().getReplicaUrls(), table.getId())) {
            throw new TableMalformedException("Replication target identity does not match the source mapping");
        }
        final String key = event.getTuple().getReplicationKey();
        if (key.isBlank() || key.length() > 255 || !key.equals(event.getTuple().getData().get("replication_key"))) {
            throw new TableMalformedException("Replication key is missing or inconsistent");
        }
        if (table.getColumns() == null || table.getColumns().stream()
                .noneMatch(column -> "replication_key".equals(column.getInternalName()))) {
            throw new TableMalformedException("Target table has no replication key");
        }
        if (!HttpMethod.DELETE.equals(method) && table.getColumns().stream()
                .anyMatch(column -> !event.getTuple().getData().containsKey(column.getInternalName()))) {
            throw new TableMalformedException("A replicated version must contain every target column");
        }
    }

    private boolean isTarget(Map<String, java.util.UUID> locations, java.util.UUID target) {
        return locations != null && locations.entrySet().stream()
                .anyMatch(entry -> !ReplicationSites.isReplica(entry.getKey(), baseUrl) && target.equals(entry.getValue()));
    }

    private void prepare(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+00:00'");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tuple_replication_inbox (
                        event_id CHAR(36) PRIMARY KEY,
                        table_id CHAR(36) NOT NULL,
                        source_database_id CHAR(36) NOT NULL,
                        source_table_id CHAR(36) NOT NULL,
                        event_sequence BIGINT NOT NULL,
                        payload LONGTEXT NOT NULL,
                        receipt LONGTEXT NOT NULL,
                        received TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        UNIQUE KEY source_event (source_database_id, event_sequence)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tuple_replication_heads (
                        table_id CHAR(36) NOT NULL,
                        replication_key VARBINARY(1024) NOT NULL,
                        source_database_id CHAR(36) NOT NULL,
                        source_table_id CHAR(36) NOT NULL,
                        event_sequence BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (table_id, replication_key)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tuple_replication_table_heads (
                        table_id CHAR(36) PRIMARY KEY,
                        source_database_id CHAR(36) NOT NULL,
                        source_table_id CHAR(36) NOT NULL,
                        event_sequence BIGINT NOT NULL DEFAULT 0
                    ) ENGINE=InnoDB
                    """);
        }
    }

    private long lockTableHead(Connection connection, Table table, UUID sourceDatabase, UUID sourceTable)
            throws SQLException, TableMalformedException {
        // A snapshot also fences keys absent from its current-row manifest.
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO tuple_replication_table_heads (table_id, source_database_id, source_table_id)
                VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE table_id = VALUES(table_id)
                """)) {
            insert.setString(1, table.getId().toString());
            insert.setString(2, sourceDatabase.toString());
            insert.setString(3, sourceTable.toString());
            insert.executeUpdate();
        }
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT event_sequence, source_database_id, source_table_id FROM tuple_replication_table_heads
                WHERE table_id = ? FOR UPDATE
                """)) {
            select.setString(1, table.getId().toString());
            try (ResultSet row = select.executeQuery()) {
                if (!row.next() || !sourceDatabase.toString().equals(row.getString(2))
                        || !sourceTable.toString().equals(row.getString(3))) {
                    throw new TableMalformedException("Snapshot and tuple delivery have different source identities");
                }
                return row.getLong(1);
            }
        }
    }

    private void lockHead(Connection connection, Table table, DataReplicationDto event) throws SQLException {
        try (PreparedStatement lock = connection.prepareStatement("""
                INSERT INTO tuple_replication_heads (table_id, replication_key, source_database_id, source_table_id)
                VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE table_id = VALUES(table_id)
                """)) {
            key(lock, table, event);
            lock.setString(3, event.getDatabase().getId().toString());
            lock.setString(4, event.getTable().getId().toString());
            lock.executeUpdate();
        }
    }

    private long head(Connection connection, Table table, DataReplicationDto event)
            throws SQLException, TableMalformedException {
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT event_sequence, source_database_id, source_table_id FROM tuple_replication_heads
                WHERE table_id = ? AND replication_key = ? FOR UPDATE
                """)) {
            key(select, table, event);
            try (ResultSet result = select.executeQuery()) {
                if (!result.next() || !event.getDatabase().getId().toString().equals(result.getString(2))
                        || !event.getTable().getId().toString().equals(result.getString(3))) {
                    throw new TableMalformedException("Replication origin identity conflicts with the existing target");
                }
                return result.getLong(1);
            }
        }
    }

    private TupleWithTimestampsDto receipt(Connection connection, DataReplicationDto event, String payload)
            throws SQLException, TableMalformedException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT payload, receipt FROM tuple_replication_inbox WHERE event_id = ?")) {
            select.setString(1, event.getEventId().toString());
            try (ResultSet result = select.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                if (!payload.equals(result.getString(1))) {
                    throw new TableMalformedException("Replication event identity was reused with different content");
                }
                try {
                    return json.readValue(result.getString(2), TupleWithTimestampsDto.class);
                } catch (JsonProcessingException failure) {
                    throw new SQLException("Stored replication receipt is invalid", failure);
                }
            }
        }
    }

    private void advance(Connection connection, Table table, DataReplicationDto event) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE tuple_replication_heads SET event_sequence = ? WHERE table_id = ? AND replication_key = ?
                """)) {
            update.setLong(1, event.getEventSequence());
            update.setString(2, table.getId().toString());
            update.setBytes(3, event.getTuple().getReplicationKey().getBytes(StandardCharsets.UTF_8));
            update.executeUpdate();
        }
    }

    private void key(PreparedStatement statement, Table table, DataReplicationDto event) throws SQLException {
        statement.setString(1, table.getId().toString());
        statement.setBytes(2, event.getTuple().getReplicationKey().getBytes(StandardCharsets.UTF_8));
    }

    private TupleWithTimestampsDto mutate(Connection connection, Database database, Table table,
                                          DataReplicationDto event, HttpMethod method)
            throws SQLException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        final String key = event.getTuple().getReplicationKey();
        final TupleWithTimestampsDto previous = select(connection, table, key, false);
        if (previous != null) {
            // A regressed SQL clock must not collapse an existing native history period.
            try (var clock = connection.prepareStatement("SELECT 1 FROM " + quote(table.getInternalName())
                    + " WHERE replication_key=? AND ROW_START >= CURRENT_TIMESTAMP(6)")) {
                clock.setString(1, key);
                try (var future = clock.executeQuery()) {
                    if (future.next()) throw new SQLException("Replica SQL clock has not passed the current row version", "40001");
                }
            }
        }
        if (HttpMethod.DELETE.equals(method)) {
            if (previous == null) {
                return absent(event);
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + quote(table.getInternalName())
                    + " WHERE replication_key = ?")) {
                delete.setString(1, key);
                delete.executeUpdate();
            }
        } else {
            final Map<String, Object> values = new LinkedHashMap<>();
            for (Column column : table.getColumns()) {
                if (previous == null || !"replication_key".equals(column.getInternalName())) {
                    values.put(column.getInternalName(), event.getTuple().getData().get(column.getInternalName()));
                }
            }
            final String sql = previous == null
                    ? mapper.tupleToRawCreateQuery(database.getInternalName(), table, TupleDto.builder().data(values).build())
                    : mapper.tupleToRawUpdateQuery(database.getInternalName(), table,
                            TupleUpdateDto.builder().data(values).keys(Map.of("replication_key", key)).build());
            if (!values.isEmpty()) {
                try (PreparedStatement write = connection.prepareStatement(sql)) {
                    int index = 1;
                    for (var value : values.entrySet()) {
                        final Column column = table.getColumns().stream()
                                .filter(candidate -> candidate.getInternalName().equals(value.getKey())).findFirst().orElseThrow();
                        bind(write, index++, column, value.getValue());
                    }
                    if (previous != null) {
                        write.setString(index, key);
                    }
                    write.executeUpdate();
                }
            }
        }
        final TupleWithTimestampsDto result = select(connection, table, key, HttpMethod.DELETE.equals(method));
        if (result == null) {
            throw new SQLException("Replicated mutation has no retained local version");
        }
        result.setApplied(true);
        return result;
    }

    private void bind(PreparedStatement statement, int index, Column column, Object value)
            throws SQLException, StorageUnavailableException, StorageNotFoundException {
        switch (column.getColumnType()) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB, BINARY, VARBINARY, BIT -> {
                if (value == null) {
                    statement.setNull(index, Types.BINARY);
                } else {
                    // Replication transports the bytes themselves, never source-local object-store keys.
                    statement.setBytes(index, value instanceof byte[] bytes ? bytes
                            : Base64.getDecoder().decode(String.valueOf(value)));
                }
            }
            case DECIMAL -> statement.setBigDecimal(index, value == null ? null : new java.math.BigDecimal(value.toString()));
            default -> mapper.prepareStatementWithColumnTypeObject(null, statement, column.getColumnType(), index,
                    column.getInternalName(), value);
        }
    }

    private TupleWithTimestampsDto select(Connection connection, Table table, String key, boolean history) throws SQLException {
        final String columns = table.getColumns().stream().map(column -> quote(column.getInternalName()))
                .collect(java.util.stream.Collectors.joining(", "));
        try (PreparedStatement select = connection.prepareStatement("SELECT " + columns
                + ", ROW_START AS replication_start, ROW_END AS replication_end FROM " + quote(table.getInternalName())
                + (history ? " FOR SYSTEM_TIME ALL" : "") + " WHERE replication_key = ? ORDER BY ROW_START DESC LIMIT 1")) {
            select.setString(1, key);
            try (ResultSet row = select.executeQuery()) {
                if (!row.next()) {
                    return null;
                }
                final Map<String, Object> values = new LinkedHashMap<>();
                for (Column column : table.getColumns()) {
                    final Object value = switch (column.getColumnType()) {
                        case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB, BINARY, VARBINARY, BIT -> row.getBytes(column.getInternalName());
                        case TIMESTAMP, DATETIME, TIME, DATE -> row.getString(column.getInternalName());
                        default -> row.getObject(column.getInternalName());
                    };
                    values.put(column.getInternalName(), value);
                }
                final Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                return TupleWithTimestampsDto.builder().data(values).replicationKey(key)
                        .insertedAt(row.getTimestamp("replication_start", utc).toInstant())
                        .deletedAt(history ? row.getTimestamp("replication_end", utc).toInstant() : null).build();
            }
        }
    }

    private TupleWithTimestampsDto absent(DataReplicationDto event) {
        return TupleWithTimestampsDto.builder().data(Map.of()).replicationKey(event.getTuple().getReplicationKey())
                .applied(false).build();
    }

    private String encode(Object value) throws SQLException {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new SQLException("Failed to encode replication receipt", failure);
        }
    }

    private String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
