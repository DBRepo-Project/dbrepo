package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
public class SubsetReplicationDispatcher extends DataConnector {
    private final MetadataService metadata;
    private final SubsetReplicationService replication;
    private final RestTemplate client;
    private final MariaDbMapper mapper;

    @Value("${dbrepo.replication.subset.enabled:${SUBSET_REPLICATION_ENABLED:true}}")
    private boolean enabled = true;

    public SubsetReplicationDispatcher(MetadataService metadata, SubsetReplicationService replication,
                                        @Qualifier("subsetReplicationRestTemplate") RestTemplate client,
                                        MariaDbMapper mapper) {
        this.metadata = metadata;
        this.replication = replication;
        this.client = client;
        this.mapper = mapper;
    }

    @Scheduled(fixedDelayString = "${dbrepo.replication.subset.retryDelayMs:30000}")
    public void dispatchDue() {
        if (!enabled) return;
        try {
            for (Database database : metadata.getDatabases()) {
                try {
                    dispatch(database);
                } catch (Exception e) {
                    log.error("Subset replication unavailable for database {}: {}", database.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Cannot list databases for subset replication: {}", e.getMessage());
        }
    }

    public int dispatch(Database database) throws SQLException {
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(mapper.queryStoreCreateSubsetOutboxRawQuery());
            // One sender per database; use per-target claims if serial delivery limits throughput.
            try (ResultSet lock = statement.executeQuery(
                    "SELECT GET_LOCK(SHA2(CONCAT(DATABASE(), ':subset-delivery'),256),0)")) {
                if (!lock.next() || lock.getInt(1) != 1) return 0;
            }
            try {
                return sendDue(connection, database);
            } finally {
                statement.execute("DO RELEASE_LOCK(SHA2(CONCAT(DATABASE(), ':subset-delivery'),256))");
            }
        } finally {
            pool.close();
        }
    }

    private int sendDue(Connection connection, Database database) throws SQLException {
        final var pending = new ArrayList<Pending>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("""
                SELECT query_id, target_site, revision FROM qs_subset_outbox
                WHERE next_attempt <= UTC_TIMESTAMP(6) ORDER BY next_attempt, query_id, target_site LIMIT 25
                """)) {
            while (rows.next()) pending.add(new Pending(UUID.fromString(rows.getString(1)), rows.getString(2), rows.getLong(3)));
        }
        int sent = 0;
        for (Pending entry : pending) {
            long deliveredRevision = entry.revision();
            try {
                // Resolve current IDs and revalidate the allowlist on every attempt; never trust payload URLs.
                final UUID targetId = replication.routes(database).get(entry.site());
                if (targetId == null) throw new IllegalStateException("Waiting for an allowed target database mapping");
                final SubsetReplicationDto payload = replication.read(connection, database, entry.queryId());
                deliveredRevision = payload.revision();
                final var response = client.exchange(entry.site() + "/api/v1/database/" + targetId + "/subset/replicate",
                        HttpMethod.PUT, new HttpEntity<>(payload), Void.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new IllegalStateException("Subset replication returned " + response.getStatusCode());
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        DELETE FROM qs_subset_outbox WHERE query_id = ? AND target_site = ? AND revision <= ?
                        """)) {
                    bind(statement, entry, deliveredRevision);
                    statement.executeUpdate();
                }
                sent++;
            } catch (Exception e) {
                log.warn("Subset {} delivery to {} failed: {}", entry.queryId(), entry.site(), e.getMessage());
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE qs_subset_outbox SET last_error = ?,
                            next_attempt = TIMESTAMPADD(SECOND, LEAST(900, 30 * POW(2, LEAST(attempts, 5))), UTC_TIMESTAMP(6)),
                            attempts = attempts + 1
                        WHERE query_id = ? AND target_site = ? AND revision <= ?
                        """)) {
                    statement.setString(1, e.getClass().getSimpleName());
                    statement.setString(2, entry.queryId().toString());
                    statement.setString(3, entry.site());
                    statement.setLong(4, deliveredRevision);
                    statement.executeUpdate();
                }
            }
        }
        return sent;
    }

    private void bind(PreparedStatement statement, Pending entry, long revision) throws SQLException {
        statement.setString(1, entry.queryId().toString());
        statement.setString(2, entry.site());
        statement.setLong(3, revision);
    }

    private record Pending(UUID queryId, String site, long revision) { }
}
