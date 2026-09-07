package at.ac.tuwien.ifs.dbrepo.service.outbox;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TupleReplicationOutboxServiceMariaDbImpl extends DataConnector implements TupleReplicationOutboxService {

    private static final String TABLE_NAME = "tuple_replication_notification_outbox";
    private static final String SELECT_COLUMNS = """
            id, database_id, table_id, http_method, payload, status, attempts, last_error,
            created, last_modified, next_attempt_at
            """;

    private final ObjectMapper objectMapper;

    @Override
    public TupleReplicationOutboxEntry enqueue(Database database, Table table, HttpMethod method,
                                               DataReplicationDto payload) throws SQLException {
        final TupleReplicationOutboxEntry entry = TupleReplicationOutboxEntry.builder()
                .id(UUID.randomUUID())
                .databaseId(database.getId())
                .tableId(table.getId())
                .httpMethod(method)
                .payloadJson(writePayload(payload))
                .status(TupleReplicationOutboxStatus.PENDING)
                .attempts(0)
                .created(Instant.now())
                .nextAttemptAt(Instant.now())
                .build();
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureTableExists(connection);
            final String statement = """
                    INSERT INTO tuple_replication_notification_outbox
                        (id, database_id, table_id, http_method, payload, status, attempts, created, next_attempt_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
                preparedStatement.setString(1, entry.getId().toString());
                preparedStatement.setString(2, entry.getDatabaseId().toString());
                preparedStatement.setString(3, entry.getTableId().toString());
                preparedStatement.setString(4, entry.getHttpMethod().name());
                preparedStatement.setString(5, entry.getPayloadJson());
                preparedStatement.setString(6, entry.getStatus().name());
                preparedStatement.setInt(7, entry.getAttempts());
                preparedStatement.setTimestamp(8, Timestamp.from(entry.getCreated()));
                preparedStatement.setTimestamp(9, Timestamp.from(entry.getNextAttemptAt()));
                preparedStatement.executeUpdate();
            }
            connection.commit();
            return entry;
        } catch (SQLException e) {
            log.error("Failed to enqueue tuple replication notification in database {}: {}",
                    database.getInternalName(), e.getMessage(), e);
            throw e;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationOutboxEntry> findAll(Database database) throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            return findAll(connection);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Optional<TupleReplicationOutboxEntry> claim(Database database, UUID id, Duration processingTimeout)
            throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureTableExists(connection);
            final Instant staleBefore = Instant.now().minus(processingTimeout);
            final Optional<TupleReplicationOutboxEntry> entry = findClaimable(connection, id, staleBefore);
            if (entry.isEmpty() || !markProcessing(connection, entry.get().getId(), staleBefore)) {
                connection.commit();
                return Optional.empty();
            }
            connection.commit();
            entry.get().setStatus(TupleReplicationOutboxStatus.PROCESSING);
            entry.get().setLastModified(Instant.now());
            return entry;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationOutboxEntry> claimDue(Database database, int limit, Duration processingTimeout)
            throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureTableExists(connection);
            final List<TupleReplicationOutboxEntry> entries = findDue(connection, limit,
                    Instant.now(), Instant.now().minus(processingTimeout));
            final List<TupleReplicationOutboxEntry> claimed = new ArrayList<>();
            for (TupleReplicationOutboxEntry entry : entries) {
                if (markProcessing(connection, entry.getId(), Instant.now().minus(processingTimeout))) {
                    entry.setStatus(TupleReplicationOutboxStatus.PROCESSING);
                    entry.setLastModified(Instant.now());
                    claimed.add(entry);
                }
            }
            connection.commit();
            return claimed;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public void markSucceeded(Database database, UUID id) throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM " + TABLE_NAME
                    + " WHERE id = ?")) {
                preparedStatement.setString(1, id.toString());
                preparedStatement.executeUpdate();
            }
        } finally {
            dataSource.close();
        }
    }

    @Override
    public void markFailed(Database database, UUID id, String error, Duration retryDelay, int maxAttempts)
            throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureTableExists(connection);
            final Optional<TupleReplicationOutboxEntry> entry = findById(connection, id);
            if (entry.isEmpty()) {
                connection.commit();
                return;
            }
            final int attempts = entry.get().getAttempts() + 1;
            final TupleReplicationOutboxStatus status = attempts >= maxAttempts
                    ? TupleReplicationOutboxStatus.FAILED
                    : TupleReplicationOutboxStatus.PENDING;
            final Instant nextAttemptAt = TupleReplicationOutboxStatus.FAILED.equals(status)
                    ? null
                    : Instant.now().plus(retryDelay);
            final String statement = """
                    UPDATE tuple_replication_notification_outbox
                    SET status = ?, attempts = ?, last_error = ?, last_modified = ?, next_attempt_at = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
                preparedStatement.setString(1, status.name());
                preparedStatement.setInt(2, attempts);
                preparedStatement.setString(3, error);
                preparedStatement.setTimestamp(4, Timestamp.from(Instant.now()));
                if (nextAttemptAt == null) {
                    preparedStatement.setTimestamp(5, null);
                } else {
                    preparedStatement.setTimestamp(5, Timestamp.from(nextAttemptAt));
                }
                preparedStatement.setString(6, id.toString());
                preparedStatement.executeUpdate();
            }
            connection.commit();
        } finally {
            dataSource.close();
        }
    }

    private Optional<TupleReplicationOutboxEntry> findClaimable(Connection connection, UUID id, Instant staleBefore)
            throws SQLException {
        final String statement = """
                SELECT %s
                FROM tuple_replication_notification_outbox
                WHERE id = ?
                  AND (status = ? OR (status = ? AND last_modified <= ?))
                """.formatted(SELECT_COLUMNS);
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setString(1, id.toString());
            preparedStatement.setString(2, TupleReplicationOutboxStatus.PENDING.name());
            preparedStatement.setString(3, TupleReplicationOutboxStatus.PROCESSING.name());
            preparedStatement.setTimestamp(4, Timestamp.from(staleBefore));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<TupleReplicationOutboxEntry> findById(Connection connection, UUID id) throws SQLException {
        final String statement = """
                SELECT %s
                FROM tuple_replication_notification_outbox
                WHERE id = ?
                """.formatted(SELECT_COLUMNS);
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setString(1, id.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    private List<TupleReplicationOutboxEntry> findAll(Connection connection) throws SQLException {
        final String statement = """
                SELECT %s
                FROM tuple_replication_notification_outbox
                ORDER BY created ASC
                """.formatted(SELECT_COLUMNS);
        final List<TupleReplicationOutboxEntry> entries = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                entries.add(map(resultSet));
            }
        }
        return entries;
    }

    private List<TupleReplicationOutboxEntry> findDue(Connection connection, int limit, Instant now,
                                                      Instant staleBefore) throws SQLException {
        final String statement = """
                SELECT %s
                FROM tuple_replication_notification_outbox
                WHERE (status = ? AND next_attempt_at <= ?)
                   OR (status = ? AND last_modified <= ?)
                ORDER BY created ASC
                LIMIT ?
                """.formatted(SELECT_COLUMNS);
        final List<TupleReplicationOutboxEntry> entries = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setString(1, TupleReplicationOutboxStatus.PENDING.name());
            preparedStatement.setTimestamp(2, Timestamp.from(now));
            preparedStatement.setString(3, TupleReplicationOutboxStatus.PROCESSING.name());
            preparedStatement.setTimestamp(4, Timestamp.from(staleBefore));
            preparedStatement.setInt(5, limit);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(map(resultSet));
                }
            }
        }
        return entries;
    }

    private boolean markProcessing(Connection connection, UUID id, Instant staleBefore) throws SQLException {
        final String statement = """
                UPDATE tuple_replication_notification_outbox
                SET status = ?, last_modified = ?
                WHERE id = ?
                  AND (status = ? OR (status = ? AND last_modified <= ?))
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setString(1, TupleReplicationOutboxStatus.PROCESSING.name());
            preparedStatement.setTimestamp(2, Timestamp.from(Instant.now()));
            preparedStatement.setString(3, id.toString());
            preparedStatement.setString(4, TupleReplicationOutboxStatus.PENDING.name());
            preparedStatement.setString(5, TupleReplicationOutboxStatus.PROCESSING.name());
            preparedStatement.setTimestamp(6, Timestamp.from(staleBefore));
            return preparedStatement.executeUpdate() == 1;
        }
    }

    private TupleReplicationOutboxEntry map(ResultSet resultSet) throws SQLException {
        return TupleReplicationOutboxEntry.builder()
                .id(UUID.fromString(resultSet.getString("id")))
                .databaseId(UUID.fromString(resultSet.getString("database_id")))
                .tableId(UUID.fromString(resultSet.getString("table_id")))
                .httpMethod(HttpMethod.valueOf(resultSet.getString("http_method")))
                .payloadJson(resultSet.getString("payload"))
                .status(TupleReplicationOutboxStatus.valueOf(resultSet.getString("status")))
                .attempts(resultSet.getInt("attempts"))
                .lastError(resultSet.getString("last_error"))
                .created(toInstant(resultSet.getTimestamp("created")))
                .lastModified(toInstant(resultSet.getTimestamp("last_modified")))
                .nextAttemptAt(toInstant(resultSet.getTimestamp("next_attempt_at")))
                .build();
    }

    private void ensureTableExists(Connection connection) throws SQLException {
        final String statement = """
                CREATE TABLE IF NOT EXISTS tuple_replication_notification_outbox (
                    id              VARCHAR(36)  NOT NULL,
                    database_id     VARCHAR(36)  NOT NULL,
                    table_id        VARCHAR(36)  NOT NULL,
                    http_method     VARCHAR(16)  NOT NULL,
                    payload         LONGTEXT     NOT NULL,
                    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
                    attempts        INT          NOT NULL DEFAULT 0,
                    last_error      TEXT,
                    created         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    last_modified   TIMESTAMP(6),
                    next_attempt_at TIMESTAMP(6),
                    PRIMARY KEY (id),
                    INDEX idx_tuple_replication_outbox_due (status, next_attempt_at),
                    INDEX idx_tuple_replication_outbox_table (table_id)
                )
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String writePayload(DataReplicationDto payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize tuple replication notification", e);
        }
    }
}
