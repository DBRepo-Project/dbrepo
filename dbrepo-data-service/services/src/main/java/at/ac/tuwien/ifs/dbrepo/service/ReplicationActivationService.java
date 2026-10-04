package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.AddReplicaDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReplicationActivationService extends DataConnector {
    private final MetadataService metadata;
    private final RestTemplate metadataRest;
    private final TupleReplicationOutboxService journal;
    private final ReplicationPeers peers;
    private final String localSite;
    private final Set<UUID> initialized = ConcurrentHashMap.newKeySet();

    public ReplicationActivationService(MetadataService metadata,
            @Qualifier("metadataServiceRestTemplate") RestTemplate metadataRest,
            TupleReplicationOutboxService journal,
            @Value("${dbrepo.replication.allowedSites:}") String allowedSites,
            @Value("${dbrepo.baseUrl}") String localSite) {
        this.metadata = metadata;
        this.metadataRest = metadataRest;
        this.journal = journal;
        this.peers = new ReplicationPeers(allowedSites);
        this.localSite = localSite;
    }

    // A shared row lock spans the entire mutation, including CSV parsing and its eventual commit.
    // The exclusive activation lock uses a separate connection so ALTER TABLE cannot release it.
    public Mutation beginMutation(Database database, Table table) throws SQLException {
        if (ReplicationSites.isReplica(database.getCreationLocation(), localSite)) {
            return new Mutation(database, table, null);
        }
        final Mutation mutation = lock(database, table, false);
        try {
            if (mutation.enabled && (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()
                    || table.getColumns().stream().noneMatch(c -> "replication_key".equals(c.getInternalName())))) {
                mutation.database = metadata.refreshDatabase(database.getId());
                mutation.table = metadata.refreshTable(database.getId(), table.getId());
                if (mutation.database.getReplicaUrls() == null || mutation.database.getReplicaUrls().isEmpty()) {
                    throw new SQLException("Replication activation is awaiting metadata registration; retry the mutation");
                }
            }
            return mutation;
        } catch (Exception e) {
            mutation.close();
            throw new SQLException("Cannot reload activated replication metadata", e);
        }
    }

    public void activate(UUID databaseId, String targetSite) throws Exception {
        final String target = peers.requireAllowedSite(targetSite);
        if (!ReplicationSites.isReplica(target, localSite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot replicate to the source site");
        }
        Database database = metadata.refreshDatabase(databaseId);
        if (ReplicationSites.isReplica(database.getCreationLocation(), localSite)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Activate replication on the primary database");
        }
        try (Mutation lock = lock(database, null, true)) {
            for (int attempt = 0; attempt < 3; attempt++) {
                database = metadata.refreshDatabase(databaseId);
                try (var pool = getDataSource(database); Connection connection = pool.getConnection()) {
                    for (Table table : database.getTables()) {
                        prepareTable(connection, table.getInternalName());
                    }
                    journal.ensureTableExists(connection);
                }
                final String path = UriComponentsBuilder.fromPath("/api/v1/database/" + databaseId + "/replicas/register")
                        .queryParam("preparedTableId", database.getTables().stream().map(Table::getId).toArray())
                        .build().toUriString();
                try {
                    metadataRest.postForEntity(path, new AddReplicaDto(target), Void.class);
                } catch (HttpClientErrorException.Conflict e) {
                    // A table created during preparation needs its key before registration can commit.
                    continue;
                } catch (HttpClientErrorException e) {
                    throw e;
                } catch (RuntimeException e) {
                    // The response may be lost after registration committed. Stale requests must never resume raw writes.
                    markEnabled(lock);
                    throw e;
                }
                markEnabled(lock);
                return;
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Table inventory changed during replication activation; retry");
        }
    }

    private void markEnabled(Mutation lock) throws SQLException {
        try (Statement statement = lock.connection.createStatement()) {
            statement.executeUpdate("UPDATE replication_activation SET enabled = TRUE WHERE id = 1");
        }
        lock.connection.commit();
    }

    public void prepareTable(Connection connection, String name) throws SQLException {
        requireInnoDb(connection, name);
        final String table = "`" + name.replace("`", "``") + "`";
        try (Statement statement = connection.createStatement(); ResultSet period = statement.executeQuery(
                "SELECT ROW_START, ROW_END FROM " + table + " FOR SYSTEM_TIME ALL LIMIT 0")) {
            if (period.getMetaData().getColumnType(1) != Types.TIMESTAMP
                    || period.getMetaData().getColumnType(2) != Types.TIMESTAMP) {
                throw new SQLException("Replication activation requires native timestamp-versioned history: " + name);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'replication_key'")) {
            statement.setString(1, name);
            try (ResultSet row = statement.executeQuery(); Statement ddl = connection.createStatement()) {
                ddl.execute("SET SESSION system_versioning_alter_history = KEEP");
                if (!row.next()) {
                    // Backfill legacy versions without UPDATE, preserving ROW_START and ROW_END exactly.
                    ddl.execute("ALTER TABLE " + table + " ADD COLUMN replication_key VARCHAR(36) NOT NULL DEFAULT(UUID()), "
                            + "ADD UNIQUE KEY (replication_key)");
                } else if (!"varchar".equalsIgnoreCase(row.getString(1)) || row.getLong(2) != 36
                        || !"NO".equals(row.getString(3))) {
                    throw new SQLException("Existing replication_key must be a non-null VARCHAR(36): " + name);
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = ? AND NON_UNIQUE = 0 GROUP BY INDEX_NAME "
                        + "HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME) = 'replication_key'")) {
            statement.setString(1, name);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("Existing replication_key requires its own unique constraint: " + name);
            }
        }
    }

    private Mutation lock(Database database, Table table, boolean exclusive) throws SQLException {
        Connection connection = null;
        try {
            final var container = database.getContainer();
            connection = DriverManager.getConnection(getJdbcUrl(container, database.getInternalName()),
                    container.getUsername(), container.getPassword());
            if (!initialized.contains(database.getId())) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS replication_activation "
                            + "(id TINYINT PRIMARY KEY, enabled BOOLEAN NOT NULL DEFAULT FALSE) ENGINE=InnoDB");
                    statement.executeUpdate("INSERT IGNORE INTO replication_activation (id) VALUES (1)");
                }
                initialized.add(database.getId());
            }
            connection.setAutoCommit(false);
            final Mutation mutation = new Mutation(database, table, connection);
            try (Statement statement = connection.createStatement(); ResultSet row = statement.executeQuery(
                    "SELECT enabled FROM replication_activation WHERE id = 1 "
                            + (exclusive ? "FOR UPDATE" : "LOCK IN SHARE MODE"))) {
                if (!row.next()) throw new SQLException("Missing replication activation lock");
                mutation.enabled = row.getBoolean(1);
            }
            return mutation;
        } catch (SQLException | RuntimeException e) {
            if (connection != null) connection.close();
            throw e;
        }
    }

    public static final class Mutation implements AutoCloseable {
        private Database database;
        private Table table;
        private final Connection connection;
        private boolean enabled;

        private Mutation(Database database, Table table, Connection connection) {
            this.database = database;
            this.table = table;
            this.connection = connection;
        }

        public Database database() { return database; }
        public Table table() { return table; }

        @Override
        public void close() throws SQLException {
            if (connection != null) {
                try { connection.rollback(); } finally { connection.close(); }
            }
        }
    }
}
