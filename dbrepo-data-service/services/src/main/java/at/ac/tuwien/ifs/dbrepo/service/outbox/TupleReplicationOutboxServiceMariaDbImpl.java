package at.ac.tuwien.ifs.dbrepo.service.outbox;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TupleReplicationOutboxServiceMariaDbImpl extends DataConnector implements TupleReplicationOutboxService {

    private static final String TABLE_NAME = "tuple_replication_notification_outbox";
    private static final String COUNTER_TABLE = "tuple_replication_journal_counter";
    // Server epochs avoid JDBC/JVM timezone conversion of operational lease and retry timestamps.
    private static final String SELECT_COLUMNS = """
            id, database_id, table_id, http_method, payload, status, attempts, last_error,
            UNIX_TIMESTAMP(created) AS created, UNIX_TIMESTAMP(last_modified) AS last_modified,
            UNIX_TIMESTAMP(next_attempt_at) AS next_attempt_at, claim_token, UNIX_TIMESTAMP(claim_until) AS claim_until
            """;
    private static final String CLAIMABLE = """
            status IN ('PENDING', 'PROCESSING', 'FAILED')
            AND ((claim_token IS NULL AND (status <> 'PROCESSING' OR last_modified IS NULL
                OR last_modified <= TIMESTAMPADD(MICROSECOND, ?, CURRENT_TIMESTAMP(6))))
                OR claim_until <= CURRENT_TIMESTAMP(6))
            AND (? OR status = 'PROCESSING' OR claim_token IS NOT NULL
                OR next_attempt_at <= CURRENT_TIMESTAMP(6))
            """;
    private static final String OWNED_CLAIM = """
            id = ? AND claim_token = ? AND claim_until > CURRENT_TIMESTAMP(6)
            AND status IN ('PROCESSING', 'FAILED')
            """;

    private final ObjectMapper objectMapper;

    @Override
    public TupleReplicationOutboxEntry enqueue(Database database, Table table, HttpMethod method,
                                               DataReplicationDto payload) throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            connection.setAutoCommit(false);
            try {
                final TupleReplicationOutboxEntry entry = enqueue(connection, database, table, method, payload);
                connection.commit();
                return entry;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } finally {
            dataSource.close();
        }
    }

    @Override
    public TupleReplicationOutboxEntry enqueue(Connection connection, Database database, Table table, HttpMethod method,
                                               DataReplicationDto payload) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new SQLException("Tuple replication must be enqueued inside the data transaction");
        }
        final UUID eventId = UUID.randomUUID();
        final long sequence = nextSequence(connection);
        final DataReplicationDto event = new DataReplicationDto(payload.getTuple(), payload.getDatabase(),
                payload.getTable(), eventId, sequence);
        final TupleReplicationOutboxEntry entry = TupleReplicationOutboxEntry.builder()
                .id(eventId)
                .databaseId(database.getId())
                .tableId(table.getId())
                .httpMethod(method)
                .payloadJson(writePayload(event))
                .status(TupleReplicationOutboxStatus.PENDING)
                .attempts(0)
                .build();
        final String statement = """
                INSERT INTO tuple_replication_notification_outbox
                    (id, database_id, table_id, http_method, payload, status, attempts, created, next_attempt_at, event_sequence)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?)
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setString(1, entry.getId().toString());
            preparedStatement.setString(2, entry.getDatabaseId().toString());
            preparedStatement.setString(3, entry.getTableId().toString());
            preparedStatement.setString(4, entry.getHttpMethod().name());
            preparedStatement.setString(5, entry.getPayloadJson());
            preparedStatement.setString(6, entry.getStatus().name());
            preparedStatement.setInt(7, entry.getAttempts());
            preparedStatement.setLong(8, sequence);
            preparedStatement.executeUpdate();
        }
        return findById(connection, entry.getId()).orElseThrow(() -> new SQLException("Enqueued event is missing"));
    }

    private long nextSequence(Connection connection) throws SQLException {
        // One database-wide lock serializes commits; replace with a commit-log coordinate if throughput requires it.
        final long previous;
        try (PreparedStatement statement = connection.prepareStatement("SELECT last_sequence, initialized FROM "
                + COUNTER_TABLE + " WHERE id = 1 FOR UPDATE"); ResultSet result = statement.executeQuery()) {
            if (!result.next() || !result.getBoolean("initialized")) {
                throw new SQLException("Prepare the source journal before starting the data transaction");
            }
            previous = result.getLong("last_sequence");
        }
        if (previous == Long.MAX_VALUE) {
            throw new SQLException("Source journal sequence exhausted");
        }
        try (PreparedStatement statement = connection.prepareStatement("UPDATE " + COUNTER_TABLE
                + " SET last_sequence = ? WHERE id = 1")) {
            statement.setLong(1, previous + 1);
            statement.executeUpdate();
        }
        return previous + 1;
    }

    public record JournalState(long committedThrough, long legacyThrough) { }

    public record JournalEntry(long sequence, UUID eventId, UUID databaseId, UUID tableId,
                               HttpMethod method, String payloadJson) { }

    /** Read on a separate consistent-snapshot connection, never the active source writer. */
    public JournalState readJournalState(Connection connection) throws SQLException {
        if (connection.getTransactionIsolation() == Connection.TRANSACTION_READ_UNCOMMITTED) {
            throw new SQLException("Source journal readers must not observe uncommitted transactions");
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT last_sequence, legacy_through, initialized FROM "
                + COUNTER_TABLE + " WHERE id = 1"); ResultSet result = statement.executeQuery()) {
            if (!result.next() || !result.getBoolean("initialized")) {
                throw new SQLException("Source journal migration is incomplete");
            }
            return new JournalState(result.getLong("last_sequence"), result.getLong("legacy_through"));
        }
    }

    /** Pages are transport chunks, not transaction boundaries. Publish only through the captured JournalState boundary. */
    public List<JournalEntry> readRange(Connection connection, long afterSequence, long throughSequence, int limit)
            throws SQLException {
        if (afterSequence < 0 || throughSequence < afterSequence || limit < 1) {
            throw new IllegalArgumentException("Invalid source journal range or page size");
        }
        final JournalState state = readJournalState(connection);
        if (throughSequence > state.committedThrough()) {
            throw new SQLException("Requested journal boundary is not visible in this read view");
        }
        final List<JournalEntry> entries = new ArrayList<>();
        long verifiedThrough = Math.max(afterSequence, state.legacyThrough());
        try (PreparedStatement statement = connection.prepareStatement("SELECT event_sequence, id, database_id, table_id, "
                + "http_method, payload FROM " + TABLE_NAME
                + " WHERE event_sequence > ? AND event_sequence <= ? ORDER BY event_sequence LIMIT ?")) {
            statement.setLong(1, afterSequence);
            statement.setLong(2, throughSequence);
            statement.setInt(3, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final long sequence = result.getLong("event_sequence");
                    if (sequence > state.legacyThrough()) {
                        if (sequence - 1 != verifiedThrough) {
                            throw new SQLException("Source journal has a gap after sequence " + verifiedThrough);
                        }
                        verifiedThrough = sequence;
                    }
                    entries.add(new JournalEntry(sequence, UUID.fromString(result.getString("id")),
                            UUID.fromString(result.getString("database_id")), UUID.fromString(result.getString("table_id")),
                            HttpMethod.valueOf(result.getString("http_method")), result.getString("payload")));
                }
            }
        }
        if (entries.size() < limit && verifiedThrough < throughSequence) {
            throw new SQLException("Source journal is missing events through sequence " + throughSequence);
        }
        return List.copyOf(entries);
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
        final long leaseMicros = leaseMicros(processingTimeout);
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            connection.setAutoCommit(false);
            try {
                final var entry = claim(connection, id, leaseMicros, true);
                connection.commit();
                return entry;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationOutboxEntry> claimDue(Database database, int limit, Duration processingTimeout)
            throws SQLException {
        final long leaseMicros = leaseMicros(processingTimeout);
        if (limit < 1) {
            throw new IllegalArgumentException("Claim limit must be positive");
        }
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            connection.setAutoCommit(false);
            try {
                final List<TupleReplicationOutboxEntry> claimed = new ArrayList<>();
                for (TupleReplicationOutboxEntry entry : findDue(connection, limit, leaseMicros)) {
                    claim(connection, entry.getId(), leaseMicros, false).ifPresent(claimed::add);
                }
                connection.commit();
                return claimed;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } finally {
            dataSource.close();
        }
    }

    @Override
    public boolean markSucceeded(Database database, UUID id, UUID claimToken) throws SQLException {
        Objects.requireNonNull(claimToken, "A claim token is required");
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + TABLE_NAME
                    + " SET status = 'SUCCEEDED', next_attempt_at = NULL, last_modified = CURRENT_TIMESTAMP(6),"
                    + " claim_token = NULL, claim_until = NULL WHERE " + OWNED_CLAIM)) {
                preparedStatement.setString(1, id.toString());
                preparedStatement.setString(2, claimToken.toString());
                return preparedStatement.executeUpdate() == 1;
            }
        } finally {
            dataSource.close();
        }
    }

    @Override
    public boolean markFailed(Database database, UUID id, UUID claimToken, String error, Duration retryDelay,
                              int maxAttempts, boolean recoverable)
            throws SQLException {
        Objects.requireNonNull(claimToken, "A claim token is required");
        if (retryDelay.isNegative()) {
            throw new IllegalArgumentException("Retry delay must not be negative");
        }
        final long retryMicros = retryDelay.toNanos() / 1000;
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            final String statement = """
                    UPDATE tuple_replication_notification_outbox
                    SET next_attempt_at = CASE WHEN (status = 'FAILED' OR attempts >= ?) AND NOT ? THEN NULL
                            ELSE TIMESTAMPADD(MICROSECOND, ?, CURRENT_TIMESTAMP(6)) END,
                        status = CASE WHEN status = 'FAILED' OR attempts >= ? THEN 'FAILED' ELSE 'PENDING' END,
                        attempts = CASE WHEN attempts < 2147483647 THEN attempts + 1 ELSE attempts END,
                        last_error = ?, last_modified = CURRENT_TIMESTAMP(6), claim_token = NULL, claim_until = NULL
                    WHERE
                    """ + OWNED_CLAIM;
            try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
                preparedStatement.setInt(1, Math.max(1, maxAttempts) - 1);
                preparedStatement.setBoolean(2, recoverable);
                preparedStatement.setLong(3, retryMicros);
                preparedStatement.setInt(4, Math.max(1, maxAttempts) - 1);
                preparedStatement.setString(5, error);
                preparedStatement.setString(6, id.toString());
                preparedStatement.setString(7, claimToken.toString());
                return preparedStatement.executeUpdate() == 1;
            }
        } finally {
            dataSource.close();
        }
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
                WHERE status <> 'SUCCEEDED'
                ORDER BY event_sequence ASC
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

    private List<TupleReplicationOutboxEntry> findDue(Connection connection, int limit, long leaseMicros)
            throws SQLException {
        final String statement = """
                SELECT %s
                FROM tuple_replication_notification_outbox
                WHERE %s
                ORDER BY next_attempt_at ASC, event_sequence ASC
                LIMIT ?
                """.formatted(SELECT_COLUMNS, CLAIMABLE);
        final List<TupleReplicationOutboxEntry> entries = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setLong(1, -leaseMicros);
            preparedStatement.setBoolean(2, false);
            preparedStatement.setInt(3, limit);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(map(resultSet));
                }
            }
        }
        return entries;
    }

    private Optional<TupleReplicationOutboxEntry> claim(Connection connection, UUID id, long leaseMicros,
                                                       boolean manualRetry) throws SQLException {
        final String statement = """
                UPDATE tuple_replication_notification_outbox
                SET status = CASE WHEN status = 'FAILED' THEN 'FAILED' ELSE 'PROCESSING' END,
                    last_modified = CURRENT_TIMESTAMP(6), claim_token = ?,
                    claim_until = TIMESTAMPADD(MICROSECOND, ?, CURRENT_TIMESTAMP(6))
                WHERE id = ? AND
                """ + CLAIMABLE;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setString(1, UUID.randomUUID().toString());
            preparedStatement.setLong(2, leaseMicros);
            preparedStatement.setString(3, id.toString());
            preparedStatement.setLong(4, -leaseMicros);
            preparedStatement.setBoolean(5, manualRetry);
            return preparedStatement.executeUpdate() == 1 ? findById(connection, id) : Optional.empty();
        }
    }

    private long leaseMicros(Duration processingTimeout) {
        final long micros = processingTimeout.toNanos() / 1000;
        if (micros < 1) {
            throw new IllegalArgumentException("Processing timeout must be at least one microsecond");
        }
        return micros;
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
                .created(toInstant(resultSet.getBigDecimal("created")))
                .lastModified(toInstant(resultSet.getBigDecimal("last_modified")))
                .nextAttemptAt(toInstant(resultSet.getBigDecimal("next_attempt_at")))
                .claimToken(resultSet.getString("claim_token") == null ? null
                        : UUID.fromString(resultSet.getString("claim_token")))
                .claimUntil(toInstant(resultSet.getBigDecimal("claim_until")))
                .build();
    }

    @Override
    public void ensureTableExists(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Prepare source journal schema before starting a transaction");
        }
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, COUNTER_TABLE, null)) {
            if (tables.next()) {
                try (PreparedStatement ready = connection.prepareStatement("SELECT initialized FROM " + COUNTER_TABLE
                        + " WHERE id = 1"); ResultSet result = ready.executeQuery()) {
                    if (result.next() && result.getBoolean(1)) {
                        ensureClaimColumns(connection);
                        return;
                    }
                }
            }
        }
        final String statement = """
                CREATE TABLE IF NOT EXISTS tuple_replication_notification_outbox (
                    id              VARCHAR(36)  NOT NULL,
                    event_sequence  BIGINT UNIQUE,
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
                    claim_token     VARCHAR(36),
                    claim_until     TIMESTAMP(6) NULL,
                    PRIMARY KEY (id),
                    INDEX idx_tuple_replication_outbox_due (status, next_attempt_at),
                    INDEX idx_tuple_replication_outbox_table (table_id)
                ) ENGINE=InnoDB
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        }
        ensureClaimColumns(connection);
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, TABLE_NAME, "event_sequence")) {
            if (!columns.next()) {
                try (PreparedStatement upgrade = connection.prepareStatement("ALTER TABLE " + TABLE_NAME
                        + " ADD COLUMN IF NOT EXISTS event_sequence BIGINT UNIQUE")) {
                    upgrade.executeUpdate();
                }
            } else if ("YES".equals(columns.getString("IS_AUTOINCREMENT"))) {
                try (PreparedStatement upgrade = connection.prepareStatement("ALTER TABLE " + TABLE_NAME
                        + " MODIFY COLUMN event_sequence BIGINT")) {
                    upgrade.executeUpdate();
                }
            }
        }
        try (PreparedStatement counter = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + COUNTER_TABLE
                + " (id INT PRIMARY KEY, last_sequence BIGINT NOT NULL, legacy_through BIGINT NOT NULL,"
                + " initialized BOOLEAN NOT NULL) ENGINE=InnoDB")) {
            counter.executeUpdate();
        }
        if ("MariaDB".equals(connection.getMetaData().getDatabaseProductName())) {
            try (PreparedStatement engines = connection.prepareStatement("SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (?, ?)")) {
                engines.setString(1, TABLE_NAME);
                engines.setString(2, COUNTER_TABLE);
                try (ResultSet result = engines.executeQuery()) {
                    while (result.next()) {
                        if (!"InnoDB".equalsIgnoreCase(result.getString("ENGINE"))) {
                            throw new SQLException("Source journal requires InnoDB: " + result.getString("TABLE_NAME"));
                        }
                    }
                }
            }
        }
        try (PreparedStatement seed = connection.prepareStatement("INSERT INTO " + COUNTER_TABLE
                + " (id, last_sequence, legacy_through, initialized) VALUES (1, 0, 0, FALSE)"
                + " ON DUPLICATE KEY UPDATE id = id")) {
            seed.executeUpdate();
        }
        connection.setAutoCommit(false);
        try {
            migrateLegacyEntries(connection);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void ensureClaimColumns(Connection connection) throws SQLException {
        for (var column : Map.of("claim_token", "VARCHAR(36)", "claim_until", "TIMESTAMP(6) NULL").entrySet()) {
            try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null,
                    TABLE_NAME, column.getKey())) {
                if (!columns.next()) {
                    try (PreparedStatement alter = connection.prepareStatement("ALTER TABLE " + TABLE_NAME
                            + " ADD COLUMN IF NOT EXISTS " + column.getKey() + " " + column.getValue())) {
                        alter.executeUpdate();
                    }
                }
            }
        }
    }

    private void migrateLegacyEntries(Connection connection) throws SQLException {
        try (PreparedStatement lock = connection.prepareStatement("SELECT initialized FROM " + COUNTER_TABLE
                + " WHERE id = 1 FOR UPDATE"); ResultSet result = lock.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Source journal counter is missing");
            }
            if (result.getBoolean(1)) {
                return;
            }
        }
        long sequence;
        try (PreparedStatement maximum = connection.prepareStatement("SELECT COALESCE(MAX(event_sequence), 0) FROM "
                + TABLE_NAME); ResultSet result = maximum.executeQuery()) {
            result.next();
            sequence = result.getLong(1);
        }
        try (PreparedStatement select = connection.prepareStatement("SELECT id, event_sequence, payload FROM " + TABLE_NAME
                + " ORDER BY created, id FOR UPDATE"); ResultSet rows = select.executeQuery();
             PreparedStatement update = connection.prepareStatement("UPDATE " + TABLE_NAME
                     + " SET event_sequence = ?, payload = ? WHERE id = ?")) {
            while (rows.next()) {
                final String id = rows.getString("id");
                final long existing = rows.getLong("event_sequence");
                final long assigned;
                if (rows.wasNull()) {
                    if (sequence == Long.MAX_VALUE) {
                        throw new SQLException("Source journal sequence exhausted during migration");
                    }
                    assigned = ++sequence;
                } else {
                    if (existing <= 0) {
                        throw new SQLException("Invalid legacy event sequence for " + id);
                    }
                    assigned = existing;
                }
                final ObjectNode payload;
                try {
                    UUID.fromString(id);
                    final String originalPayload = rows.getString("payload");
                    final var json = objectMapper.readTree(originalPayload);
                    if (!(json instanceof ObjectNode object)) {
                        throw new SQLException("Legacy event payload must be an object: " + id);
                    }
                    payload = object;
                    if (payload.hasNonNull("eventId") && !id.equals(payload.get("eventId").asText())) {
                        throw new SQLException("Conflicting legacy event identity: " + id);
                    }
                    if (payload.hasNonNull("eventSequence") && (!payload.get("eventSequence").isIntegralNumber()
                            || !payload.get("eventSequence").canConvertToLong()
                            || payload.get("eventSequence").longValue() != assigned)) {
                        throw new SQLException("Conflicting legacy event sequence: " + id);
                    }
                    final boolean alreadyIdentified = payload.hasNonNull("eventId") && payload.hasNonNull("eventSequence");
                    payload.put("eventId", id);
                    payload.put("eventSequence", assigned);
                    update.setLong(1, assigned);
                    update.setString(2, alreadyIdentified ? originalPayload : objectMapper.writeValueAsString(payload));
                    update.setString(3, id);
                    update.executeUpdate();
                } catch (JsonProcessingException | IllegalArgumentException e) {
                    throw new SQLException("Invalid legacy source event " + id, e);
                }
            }
        }
        try (PreparedStatement finish = connection.prepareStatement("UPDATE " + COUNTER_TABLE
                + " SET last_sequence = ?, legacy_through = ?, initialized = TRUE WHERE id = 1")) {
            finish.setLong(1, sequence);
            finish.setLong(2, sequence);
            finish.executeUpdate();
        }
    }

    private Instant toInstant(BigDecimal epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds.longValue(),
                epochSeconds.remainder(BigDecimal.ONE).movePointRight(9).longValue());
    }

    private String writePayload(DataReplicationDto payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize tuple replication notification", e);
        }
    }
}
