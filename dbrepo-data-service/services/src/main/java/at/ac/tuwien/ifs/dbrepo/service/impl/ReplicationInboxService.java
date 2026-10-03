package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Base64;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

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
            requireInnoDb(connection, table.getInternalName(), "tuple_replication_inbox", "tuple_replication_heads");
            connection.setAutoCommit(false);
            try {
                lockHead(connection, table, event);
                final TupleWithTimestampsDto receipt = receipt(connection, event, payload);
                if (receipt != null) {
                    connection.commit();
                    return receipt;
                }
                final long head = head(connection, table, event);
                if (head == event.getEventSequence()) {
                    throw new TableMalformedException("Different replication events share the same source sequence");
                }
                final TupleWithTimestampsDto result;
                if (head > event.getEventSequence()) {
                    result = absent(event);
                } else {
                    result = mutate(connection, database, table, event, method);
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
