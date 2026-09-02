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
import java.util.List;

@Slf4j
@Service
public class ReplicationTimestampServiceMariaDbImpl extends DataConnector implements ReplicationTimestampService {

    @Override
    public void saveTimestamps(Database database, List<TupleReplicationTimestampDto> timestamps) throws SQLException {
        if (timestamps == null || timestamps.isEmpty()) {
            return;
        }
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureTableExists(connection);
            for (TupleReplicationTimestampDto timestamp : timestamps) {
                upsertTimestamp(connection, timestamp);
            }
            connection.commit();
        } catch (SQLException e) {
            log.error("Failed to save replication timestamps in database {}: {}", database.getInternalName(),
                    e.getMessage());
            throw e;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public void closeAndSaveTimestamps(Database database, List<TupleReplicationTimestampDto> timestamps)
            throws SQLException {
        if (timestamps == null || timestamps.isEmpty()) {
            return;
        }
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureTableExists(connection);
            for (TupleReplicationTimestampDto timestamp : timestamps) {
                closeActiveTimestamp(connection, timestamp);
                upsertTimestamp(connection, timestamp);
            }
            connection.commit();
        } catch (SQLException e) {
            log.error("Failed to update replication timestamps in database {}: {}", database.getInternalName(),
                    e.getMessage());
            throw e;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public void updateTimestampRowEnds(Database database, List<TupleReplicationTimestampDto> timestamps)
            throws SQLException {
        if (timestamps == null || timestamps.isEmpty()) {
            return;
        }
        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureTableExists(connection);
            for (TupleReplicationTimestampDto timestamp : timestamps) {
                updateTimestampRowEnd(connection, timestamp);
            }
            connection.commit();
        } catch (SQLException e) {
            log.error("Failed to close replication timestamps in database {}: {}", database.getInternalName(),
                    e.getMessage());
            throw e;
        } finally {
            dataSource.close();
        }
    }

    private void ensureTableExists(Connection connection) throws SQLException {
        final String statement = """
                CREATE TABLE IF NOT EXISTS tuple_replication_timestamps (
                    site_url       TEXT         NOT NULL,
                    replication_id VARCHAR(255) NOT NULL,
                    database_id    VARCHAR(36)  NOT NULL,
                    table_id       VARCHAR(36)  NOT NULL,
                    row_start      TIMESTAMP(6) NOT NULL,
                    row_end        TIMESTAMP(6),
                    PRIMARY KEY (`site_url`(255), `replication_id`, `row_start`)
                )
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        }
    }

    private void upsertTimestamp(Connection connection, TupleReplicationTimestampDto timestamp) throws SQLException {
        if (timestamp.getRowStart() == null) {
            log.warn("Skip replication timestamp without rowStart: {}", timestamp);
            return;
        }
        final String statement = """
                INSERT INTO tuple_replication_timestamps
                    (site_url, replication_id, database_id, table_id, row_start, row_end)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    database_id = VALUES(database_id),
                    table_id = VALUES(table_id),
                    row_end = VALUES(row_end)
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            bindTimestamp(preparedStatement, timestamp);
            preparedStatement.executeUpdate();
        }
    }

    private void closeActiveTimestamp(Connection connection, TupleReplicationTimestampDto timestamp)
            throws SQLException {
        if (timestamp.getRowStart() == null) {
            log.warn("Skip active timestamp close without rowStart: {}", timestamp);
            return;
        }
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

    private void updateTimestampRowEnd(Connection connection, TupleReplicationTimestampDto timestamp)
            throws SQLException {
        if (timestamp.getRowEnd() == null) {
            log.warn("Skip replication timestamp rowEnd update without rowEnd: {}", timestamp);
            return;
        }
        final String statement = """
                UPDATE tuple_replication_timestamps
                SET row_end = ?
                WHERE site_url = ?
                  AND replication_id = ?
                  AND database_id = ?
                  AND table_id = ?
                  AND row_start = ?
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setTimestamp(1, toTimestamp(timestamp.getRowEnd()));
            preparedStatement.setString(2, timestamp.getSiteUrl());
            preparedStatement.setString(3, timestamp.getReplicationId());
            preparedStatement.setString(4, String.valueOf(timestamp.getDatabaseId()));
            preparedStatement.setString(5, String.valueOf(timestamp.getTableId()));
            preparedStatement.setTimestamp(6, toTimestamp(timestamp));
            final int updated = preparedStatement.executeUpdate();
            if (updated == 0 && timestamp.getRowStart() != null) {
                upsertTimestamp(connection, timestamp);
            }
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
