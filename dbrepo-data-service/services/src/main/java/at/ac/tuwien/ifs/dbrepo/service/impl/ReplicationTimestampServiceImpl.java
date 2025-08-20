package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationTimestampService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationTimestampServiceImpl extends DataConnector implements ReplicationTimestampService {

    @Override
    public void saveReplicationTimestamp(DatabaseDto database, TupleReplicationTimestamp timestamp) {
        final String sql = """
            INSERT INTO tuple_replication_timestamps
            (site_url, replication_id, database_id, table_id, row_start, row_end)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            database_id = VALUES(database_id),
            table_id = VALUES(table_id),
            row_start = VALUES(row_start),
            row_end = VALUES(row_end)
            """;

        final ComboPooledDataSource dataSource = getDataSource(database);
        System.out.println(dataSource.toString());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, timestamp.getSiteUrl());
            statement.setString(2, timestamp.getReplicationId());
            statement.setString(3, timestamp.getDatabaseId().toString());
            statement.setString(4, timestamp.getTableId().toString());
            statement.setTimestamp(5, timestamp.getRowStart());
            statement.setTimestamp(6, timestamp.getRowEnd());

            statement.executeUpdate();
            log.debug("Saved replication timestamp: {}", timestamp);

        } catch (SQLException e) {
            log.error("Failed to save replication timestamp: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save replication timestamp", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public void saveReplicationTimestamps(DatabaseDto database, List<TupleReplicationTimestamp> timestamps) {
        if (timestamps == null || timestamps.isEmpty()) {
            return;
        }

        final String sql = """
            INSERT INTO tuple_replication_timestamps
            (site_url, replication_id, database_id, table_id, row_start, row_end)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            database_id = VALUES(database_id),
            table_id = VALUES(table_id),
            row_start = VALUES(row_start),
            row_end = VALUES(row_end)
            """;
        System.out.println(sql);
        final ComboPooledDataSource dataSource = getDataSource(database);
        System.out.println(dataSource.toString());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            for (TupleReplicationTimestamp timestamp : timestamps) {
                statement.setString(1, timestamp.getSiteUrl());
                statement.setString(2, timestamp.getReplicationId());
                statement.setString(3, timestamp.getDatabaseId().toString());
                statement.setString(4, timestamp.getTableId().toString());
                statement.setTimestamp(5, timestamp.getRowStart());
                statement.setTimestamp(6, timestamp.getRowEnd());
                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();
            log.debug("Saved {} replication timestamps", timestamps.size());

        } catch (SQLException e) {
            log.error("Failed to save replication timestamps: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save replication timestamps", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationTimestamp> findByDatabaseIdAndTableId(DatabaseDto database, UUID databaseId, UUID tableId) {
        final String sql = "SELECT * FROM tuple_replication_timestamps WHERE database_id = ? AND table_id = ?";

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, databaseId.toString());
            statement.setString(2, tableId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResultSetToList(resultSet);
            }

        } catch (SQLException e) {
            log.error("Failed to find timestamps by database and table: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find timestamps by database and table", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationTimestamp> findBySiteUrl(DatabaseDto database, String siteUrl) {
        final String sql = "SELECT * FROM tuple_replication_timestamps WHERE site_url = ?";

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, siteUrl);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResultSetToList(resultSet);
            }

        } catch (SQLException e) {
            log.error("Failed to find timestamps by site URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find timestamps by site URL", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationTimestamp> findByReplicationId(DatabaseDto database, String replicationId) {
        final String sql = "SELECT * FROM tuple_replication_timestamps WHERE replication_id = ?";

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, replicationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResultSetToList(resultSet);
            }

        } catch (SQLException e) {
            log.error("Failed to find timestamps by replication ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find timestamps by replication ID", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationTimestamp> findByTimeRange(DatabaseDto database, Instant startTime, Instant endTime) {
        final String sql = """
            SELECT * FROM tuple_replication_timestamps
            WHERE row_start >= ? AND (row_end IS NULL OR row_end <= ?)
            """;

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.from(startTime));
            statement.setTimestamp(2, Timestamp.from(endTime));

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResultSetToList(resultSet);
            }

        } catch (SQLException e) {
            log.error("Failed to find timestamps by time range: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find timestamps by time range", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationTimestamp> findActiveTimestamps(DatabaseDto database, UUID databaseId, UUID tableId) {
        final String sql = """
            SELECT * FROM tuple_replication_timestamps
            WHERE database_id = ? AND table_id = ? AND row_end IS NULL
            """;

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, databaseId.toString());
            statement.setString(2, tableId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResultSetToList(resultSet);
            }

        } catch (SQLException e) {
            log.error("Failed to find active timestamps: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find active timestamps", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TupleReplicationTimestamp> findBySiteUrlAndDatabaseIdAndTableId(DatabaseDto database, String siteUrl, UUID databaseId, UUID tableId) {
        final String sql = """
            SELECT * FROM tuple_replication_timestamps
            WHERE site_url = ? AND database_id = ? AND table_id = ?
            """;

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, siteUrl);
            statement.setString(2, databaseId.toString());
            statement.setString(3, tableId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapResultSetToList(resultSet);
            }

        } catch (SQLException e) {
            log.error("Failed to find timestamps by site, database and table: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find timestamps by site, database and table", e);
        } finally {
            dataSource.close();
        }
    }

    private List<TupleReplicationTimestamp> mapResultSetToList(ResultSet resultSet) throws SQLException {
        List<TupleReplicationTimestamp> timestamps = new ArrayList<>();

        while (resultSet.next()) {
            TupleReplicationTimestamp timestamp = TupleReplicationTimestamp.builder()
                    .siteUrl(resultSet.getString("site_url"))
                    .replicationId(resultSet.getString("replication_id"))
                    .databaseId(UUID.fromString(resultSet.getString("database_id")))
                    .tableId(UUID.fromString(resultSet.getString("table_id")))
                    .rowStart(resultSet.getTimestamp("row_start"))
                    .rowEnd(resultSet.getTimestamp("row_end"))
                    .build();

            timestamps.add(timestamp);
        }

        return timestamps;
    }

    @Override
    public void ensureTableExists(DatabaseDto database) {
        final String createTableSql = """
            CREATE TABLE IF NOT EXISTS tuple_replication_timestamps (
                site_url        TEXT         NOT NULL,
                replication_id  VARCHAR(255) NOT NULL,
                database_id     VARCHAR(36)  NOT NULL,
                table_id        VARCHAR(36)  NOT NULL,
                row_start       TIMESTAMP(6) NOT NULL,
                row_end         TIMESTAMP(6),
                PRIMARY KEY (`site_url`(255), `replication_id`)
            )
            """;

        System.out.println("JDBC URL:");
        System.out.println(getJdbcUrl(database.getContainer(), database.getInternalName()));
        System.out.println("Container details: ");
        System.out.println(database.getContainer().getName());
        System.out.println(database.getContainer().getUsername());
        System.out.println(database.getContainer().getPassword());
        final ComboPooledDataSource dataSource = getDataSource(database);
        System.out.println(dataSource.toString());
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(createTableSql)) {

            statement.executeUpdate();
            log.debug("Ensured tuple_replication_timestamps table exists in database: {}", database.getInternalName());

        } catch (SQLException e) {
            log.error("Failed to create tuple_replication_timestamps table in database {}: {}",
                    database.getInternalName(), e.getMessage(), e);
            throw new RuntimeException("Failed to create tuple_replication_timestamps table", e);
        } finally {
            dataSource.close();
        }
    }
}
