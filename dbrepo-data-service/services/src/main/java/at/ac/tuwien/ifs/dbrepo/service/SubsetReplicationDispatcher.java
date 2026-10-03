package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto.Progress;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final SubsetResultService results;

    @Value("${dbrepo.replication.subset.enabled:${SUBSET_REPLICATION_ENABLED:true}}")
    private boolean enabled = true;

    public SubsetReplicationDispatcher(MetadataService metadata, SubsetReplicationService replication,
                                        @Qualifier("subsetReplicationRestTemplate") RestTemplate client,
                                        MariaDbMapper mapper, SubsetResultService results) {
        this.metadata = metadata;
        this.replication = replication;
        this.client = client;
        this.mapper = mapper;
        this.results = java.util.Objects.requireNonNull(results);
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
                if (payload.snapshotHash() != null) sendResult(connection, payload, entry.site(), targetId);
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

    private void sendResult(Connection connection, SubsetReplicationDto query, String site, UUID targetId) throws SQLException {
        final String url = site + "/api/v1/database/" + targetId + "/subset/" + query.queryId() + "/result";
        final var begin = client.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(results.manifest(connection, query)), Progress.class);
        Progress progress = begin.getBody();
        if (progress == null || progress.nextRow() < 0 || progress.nextRow() > query.resultCount()
                || progress.nextOffset() < 0 || !begin.getStatusCode().is2xxSuccessful()
                || (progress.ready() && (progress.nextRow() != query.resultCount() || progress.nextOffset() != 0))) {
            throw new IllegalStateException("Invalid snapshot staging response");
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Subset-Sender", query.senderSite());
        headers.set("X-Subset-Database", query.senderDatabaseId().toString());
        while (!progress.ready() && progress.nextRow() < query.resultCount()) {
            final long row = progress.nextRow(), offset = progress.nextOffset();
            final byte[] chunk;
            final long length;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT row_hash,byte_length,SUBSTRING(payload,?,?) FROM qs_subset_result_rows
                    WHERE query_id=? AND row_no=?
                    """)) {
                statement.setLong(1, offset + 1);
                statement.setInt(2, SubsetResultService.CHUNK_SIZE);
                statement.setString(3, query.queryId().toString());
                statement.setLong(4, row);
                try (ResultSet value = statement.executeQuery()) {
                    if (!value.next()) throw new IllegalStateException("Missing source snapshot row");
                    length = value.getLong(2);
                    chunk = value.getBytes(3);
                    headers.set("X-Row-Hash", value.getString(1));
                }
            }
            if (chunk.length == 0 || offset > length - chunk.length) throw new IllegalStateException("Invalid snapshot staging offset");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("X-Row-Length", Long.toString(length));
            headers.set("X-Chunk-Hash", SubsetResultService.sha256(chunk));
            final var response = client.exchange(url + "/rows/" + row + "/chunks/" + offset, HttpMethod.PUT,
                    new HttpEntity<>(chunk, headers), Progress.class);
            final long expectedRow = offset + chunk.length == length ? row + 1 : row;
            final long expectedOffset = offset + chunk.length == length ? 0 : offset + chunk.length;
            progress = response.getBody();
            if (progress == null || !response.getStatusCode().is2xxSuccessful() || progress.nextRow() != expectedRow
                    || progress.nextOffset() != expectedOffset) throw new IllegalStateException("Unexpected snapshot staging acknowledgement");
        }
        final var published = client.exchange(url + "/publish", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        if (!published.getStatusCode().is2xxSuccessful()) throw new IllegalStateException("Snapshot publication failed");
    }

    private record Pending(UUID queryId, String site, long revision) { }
}
