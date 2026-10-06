package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetHistory;
import at.ac.tuwien.ifs.dbrepo.service.impl.TupleVersionHistory;
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
                sendEvidence(connection, database, payload, entry.site(), targetId);
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

    private void sendEvidence(Connection c, Database database, SubsetReplicationDto query, String site, UUID targetId)
            throws SQLException {
        if (query.executionContext() == null) return;
        final var execution = SubsetHistory.decode(query.executionContext());
        for (var binding : execution.tables().values()) {
            if (binding.nativeOnly()) continue;
            final var table = SubsetHistory.table(database, query.originSite(), execution, binding);
            final UUID targetTable = table.getReplicaUrls() == null ? null : table.getReplicaUrls().get(site);
            if (targetTable == null) throw new IllegalStateException("Waiting for subset history table mapping");
            final String url = site + "/api/v1/database/" + targetId + "/table/" + targetTable + "/timestamps";
            // ponytail: resend relevant interval history; add per-peer checkpoints if this scan dominates delivery.
            try (var s = c.prepareStatement("SELECT * FROM tuple_replication_timestamps WHERE site_url=?"
                    + " AND database_id=? AND table_id=? AND row_start<=? ORDER BY replication_id,row_start")) {
                s.setString(1, query.originSite()); s.setString(2, execution.databaseId().toString());
                s.setString(3, binding.tableId().toString());
                s.setTimestamp(4, Timestamp.from(query.selectedAt()), TupleVersionHistory.utc());
                s.setFetchSize(256);
                final var batch = new ArrayList<TupleReplicationTimestampDto>(256);
                try (var rows = s.executeQuery()) {
                    while (rows.next()) {
                        final Timestamp end = rows.getTimestamp("row_end", TupleVersionHistory.utc());
                        final Timestamp version = rows.getTimestamp("master_site_ts", TupleVersionHistory.utc());
                        batch.add(TupleReplicationTimestampDto.builder().siteUrl(query.originSite())
                                .databaseId(execution.databaseId()).tableId(binding.tableId())
                                .replicationId(rows.getString("replication_id"))
                                .masterSiteTs(version == null ? null : version.toInstant())
                                .rowStart(rows.getTimestamp("row_start", TupleVersionHistory.utc()).toInstant())
                                .rowEnd(end == null ? null : end.toInstant())
                                .visibilityStart((Long) rows.getObject("visibility_start"))
                                .visibilityEnd((Long) rows.getObject("visibility_end")).build());
                        if (batch.size() == 256) { sendEvidenceBatch(url, batch); batch.clear(); }
                    }
                    if (!batch.isEmpty()) sendEvidenceBatch(url, batch);
                }
            }
        }
    }

    private void sendEvidenceBatch(String url, java.util.List<TupleReplicationTimestampDto> batch) {
        final var response = client.exchange(url, HttpMethod.POST, new HttpEntity<>(java.util.List.copyOf(batch)), Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) throw new IllegalStateException("Subset timestamp evidence delivery failed");
    }

    private record Pending(UUID queryId, String site, long revision) { }
}
