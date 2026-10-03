package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationTimestampService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ReplicationTimestampServiceMariaDbImpl extends DataConnector implements ReplicationTimestampService {

    @Override
    public void saveTimestamps(Database database, List<TupleReplicationTimestampDto> timestamps) throws SQLException {
        writeBatch(database, timestamps, false, false);
    }

    @Override
    public void closeAndSaveTimestamps(Database database, List<TupleReplicationTimestampDto> timestamps)
            throws SQLException {
        writeBatch(database, timestamps, true, false);
    }

    @Override
    public void updateTimestampRowEnds(Database database, List<TupleReplicationTimestampDto> timestamps)
            throws SQLException {
        writeBatch(database, timestamps, false, true);
    }

    private void writeBatch(Database database, List<TupleReplicationTimestampDto> timestamps,
                            boolean closeEarlier, boolean requireRowEnd) throws SQLException {
        if (timestamps == null || timestamps.isEmpty()) {
            return;
        }
        for (TupleReplicationTimestampDto timestamp : timestamps) {
            if (timestamp == null || timestamp.getSiteUrl() == null || timestamp.getSiteUrl().isBlank()
                    || timestamp.getReplicationId() == null || timestamp.getReplicationId().isBlank()
                    || timestamp.getDatabaseId() == null || timestamp.getTableId() == null
                    || timestamp.getRowStart() == null || (requireRowEnd && timestamp.getRowEnd() == null)
                    || (timestamp.getRowEnd() != null && timestamp.getRowEnd().isBefore(timestamp.getRowStart()))) {
                throw new SQLException("Replication timestamp requires a complete source identity and valid period");
            }
        }
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            // MariaDB DDL implicitly commits: schema work must precede the batch transaction.
            ensureTableExists(connection);
            connection.setAutoCommit(false);
            try {
                for (TupleReplicationTimestampDto timestamp : timestamps) {
                    if (closeEarlier) {
                        closeActiveTimestamp(connection, timestamp);
                    }
                    upsertTimestamp(connection, timestamp);
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                throw e;
            }
        } catch (SQLException e) {
            log.error("Failed to merge replication timestamps in database {}: {}", database.getInternalName(),
                    e.getMessage());
            throw e;
        } finally {
            dataSource.close();
        }
    }

    private void ensureTableExists(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Timestamp schema initialization requires an auto-commit connection");
        }
        final String statement = """
                CREATE TABLE IF NOT EXISTS tuple_replication_timestamps (
                    site_url       TEXT         NOT NULL,
                    replication_id VARCHAR(255) NOT NULL,
                    database_id    VARCHAR(36)  NOT NULL,
                    table_id       VARCHAR(36)  NOT NULL,
                    row_start      TIMESTAMP(6) NOT NULL,
                    row_end        TIMESTAMP(6),
                    PRIMARY KEY (`site_url`(255), `replication_id`, `database_id`, `table_id`, `row_start`)
                ) ENGINE=InnoDB
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        }
        final List<String> primaryKey = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement("""
                SELECT s.COLUMN_NAME, s.SUB_PART, t.ENGINE
                FROM information_schema.STATISTICS s
                JOIN information_schema.TABLES t
                    ON t.TABLE_SCHEMA = s.TABLE_SCHEMA AND t.TABLE_NAME = s.TABLE_NAME
                WHERE s.TABLE_SCHEMA = DATABASE() AND s.TABLE_NAME = 'tuple_replication_timestamps'
                    AND s.INDEX_NAME = 'PRIMARY'
                ORDER BY s.SEQ_IN_INDEX
                """); var result = preparedStatement.executeQuery()) {
            while (result.next()) {
                if (!"InnoDB".equalsIgnoreCase(result.getString("ENGINE"))) {
                    throw new SQLException("Timestamp storage must use InnoDB; explicit migration required");
                }
                final String prefix = result.getString("SUB_PART");
                primaryKey.add(result.getString("COLUMN_NAME") + (prefix == null ? "" : ":" + prefix));
            }
        }
        if (primaryKey.equals(List.of("site_url:255", "replication_id", "database_id", "table_id", "row_start"))) {
            return;
        }
        if (!primaryKey.equals(List.of("site_url:255", "replication_id", "row_start"))) {
            throw new SQLException("Unrecognized timestamp primary key; preserve rows and migrate explicitly");
        }
        // A single ALTER preserves surviving rows. Never guess identities lost to the old key's upsert.
        try (PreparedStatement preparedStatement = connection.prepareStatement("""
                ALTER TABLE tuple_replication_timestamps
                    DROP PRIMARY KEY,
                    ADD PRIMARY KEY (`site_url`(255), `replication_id`, `database_id`, `table_id`, `row_start`)
                """)) {
            preparedStatement.executeUpdate();
        }
        log.warn("Upgraded timestamp primary key in database {} preserving surviving rows. "
                + "Prior source-identity collisions or overwritten row ends cannot be reconstructed; "
                + "reconcile historical evidence against trusted sources or backups.", connection.getCatalog());
    }

    private void upsertTimestamp(Connection connection, TupleReplicationTimestampDto timestamp) throws SQLException {
        final String statement = """
                INSERT INTO tuple_replication_timestamps
                    (site_url, replication_id, database_id, table_id, row_start, row_end)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    row_end = CASE
                        WHEN row_end IS NULL THEN VALUES(row_end)
                        WHEN VALUES(row_end) IS NULL THEN row_end
                        ELSE LEAST(row_end, VALUES(row_end))
                    END
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            bindTimestamp(preparedStatement, timestamp);
            preparedStatement.executeUpdate();
        }
    }

    private void closeActiveTimestamp(Connection connection, TupleReplicationTimestampDto timestamp)
            throws SQLException {
        final String statement = """
                UPDATE tuple_replication_timestamps
                SET row_end = ?
                WHERE site_url = ?
                  AND replication_id = ?
                  AND database_id = ?
                  AND table_id = ?
                  AND row_start < ?
                  AND (row_end IS NULL OR row_end > ?)
                """;
        final Timestamp rowStart = toTimestamp(timestamp);
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setTimestamp(1, rowStart);
            preparedStatement.setString(2, timestamp.getSiteUrl());
            preparedStatement.setString(3, timestamp.getReplicationId());
            preparedStatement.setString(4, String.valueOf(timestamp.getDatabaseId()));
            preparedStatement.setString(5, String.valueOf(timestamp.getTableId()));
            preparedStatement.setTimestamp(6, rowStart);
            preparedStatement.setTimestamp(7, rowStart);
            preparedStatement.executeUpdate();
        }
    }

    private void bindTimestamp(PreparedStatement preparedStatement, TupleReplicationTimestampDto timestamp)
            throws SQLException {
        preparedStatement.setString(1, timestamp.getSiteUrl());
        preparedStatement.setString(2, timestamp.getReplicationId());
        preparedStatement.setString(3, String.valueOf(timestamp.getDatabaseId()));
        preparedStatement.setString(4, String.valueOf(timestamp.getTableId()));
        preparedStatement.setTimestamp(5, toTimestamp(timestamp));
        preparedStatement.setTimestamp(6, toTimestamp(timestamp.getRowEnd()));
    }

    private Timestamp toTimestamp(TupleReplicationTimestampDto timestamp) {
        return toTimestamp(timestamp.getRowStart());
    }

    private Timestamp toTimestamp(java.time.Instant value) {
        if (value == null) {
            return null;
        }
        final Timestamp timestamp = Timestamp.from(value);
        timestamp.setNanos((timestamp.getNanos() / 1000) * 1000);
        return timestamp;
    }
}
