package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryExecutionException;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SubsetReplicationService extends DataConnector {
    private final MariaDbMapper mapper;
    private final ObjectMapper json;
    private final ReplicationPeers peers;
    private final Validator validator;
    private final String localSite;
    private final SubsetResultService results;

    public SubsetReplicationService(MariaDbMapper mapper, ObjectMapper json,
                                    @Qualifier("subsetReplicationPeers") ReplicationPeers peers, Validator validator,
                                    @Value("${dbrepo.baseUrl}") String localSite, SubsetResultService results) {
        this.mapper = mapper;
        this.json = json;
        this.peers = peers;
        this.validator = validator;
        this.localSite = new ReplicationPeers(localSite).requireAllowedSite(localSite);
        this.results = Objects.requireNonNull(results, "Subset results are required");
    }

    /** DDL must finish before any query-store mutation, because MariaDB DDL commits. */
    public void prepare(Connection connection, Database database, String sender) throws SQLException {
        results.initialize(connection);
        final Set<String> targets = new TreeSet<>(routes(database).keySet());
        if (sender != null) targets.remove(sender);
        final String encoded;
        try {
            encoded = json.writeValueAsString(targets);
        } catch (JsonProcessingException e) {
            throw new SQLException("Cannot encode subset replication targets", e);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(mapper.queryStoreCreateSubsetOutboxRawQuery());
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SET @dbrepo_subset_origin = ?, @dbrepo_subset_targets = ?,"
                        + " @dbrepo_subset_context=NULL,@dbrepo_subset_execution_sql=NULL,time_zone = '+00:00'")) {
            statement.setString(1, localSite);
            statement.setString(2, encoded);
            statement.execute();
        }
    }

    public Map<String, UUID> routes(Database database) {
        final Map<String, UUID> routes = new TreeMap<>();
        if (database.getReplicaUrls() != null) {
            database.getReplicaUrls().forEach((site, id) -> {
                final String normalized = new ReplicationPeers(site).requireAllowedSite(site);
                if (!normalized.equals(localSite)) routes.put(peers.requireAllowedSite(site), id);
            });
        }
        // A secondary must retain a pending route to its primary even before its ID mapping arrives.
        if (database.getCreationLocation() != null && !database.getCreationLocation().isBlank()) {
            final String origin = new ReplicationPeers(database.getCreationLocation())
                    .requireAllowedSite(database.getCreationLocation());
            if (!origin.equals(localSite) && !routes.containsKey(origin)) {
                routes.put(peers.requireAllowedSite(origin), null);
            }
        }
        return routes;
    }

    /** Queue current canonical state only; never recreate results or touch versioned query rows. */
    public void backfill(Database database, String targetSite) throws SQLException {
        final String target = requireBackfillTarget(database, targetSite);
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection(); Statement ddl = connection.createStatement()) {
            ddl.execute(mapper.queryStoreCreateSubsetOutboxRawQuery());
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO qs_subset_outbox (query_id, target_site, revision)
                    SELECT id, ?, replication_revision FROM qs_queries
                    WHERE creation_location IS NOT NULL AND replication_revision > 0
                    ON DUPLICATE KEY UPDATE
                        next_attempt = IF(VALUES(revision) > revision, UTC_TIMESTAMP(6), next_attempt),
                        revision = GREATEST(revision, VALUES(revision))
                    """)) {
                statement.setString(1, target);
                statement.executeUpdate();
            }
        } finally {
            pool.close();
        }
    }

    public String requireBackfillTarget(Database database, String targetSite) {
        final String target;
        final Map<String, UUID> configured;
        try {
            target = peers.requireAllowedSite(targetSite);
            configured = routes(database);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Untrusted subset replication site");
        }
        if (target.equals(localSite) || configured.get(target) == null) {
            throw conflict("Subset backfill requires a configured non-local target database mapping");
        }
        return target;
    }

    public void enqueue(Connection connection, UUID queryId) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Subset outbox requires the query-store transaction");
        try (PreparedStatement statement = connection.prepareStatement(mapper.queryStoreEnqueueSubsetRawQuery())) {
            statement.setString(1, queryId.toString());
            statement.executeUpdate();
        }
    }

    public SubsetReplicationDto read(Connection connection, Database database, UUID queryId) throws SQLException {
        final SubsetReplicationDto value = find(connection, database, queryId, false);
        if (value == null) throw new SQLException("Subset outbox references a missing query");
        return value;
    }

    private SubsetReplicationDto find(Connection connection, Database database, UUID queryId, boolean lock) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM qs_queries WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            statement.setString(1, queryId.toString());
            try (ResultSet row = statement.executeQuery()) {
                return row.next() ? read(row, database) : null;
            }
        }
    }

    private SubsetReplicationDto read(ResultSet row, Database database) throws SQLException {
        return new SubsetReplicationDto(UUID.fromString(row.getString("id")), row.getString("creation_location"),
                localSite, database.getId(), row.getString("query"), row.getString("query_normalized"),
                row.getTimestamp("executed", utc()).toInstant(), row.getBoolean("is_persisted"),
                row.getString("result_hash"), row.getLong("result_number"), row.getLong("replication_revision"),
                row.getString("snapshot_hash"), row.getString("execution_context"));
    }

    public void receive(Database database, SubsetReplicationDto incoming) throws SQLException {
        validate(database, incoming);
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection()) {
            prepare(connection, database, incoming.senderSite());
            connection.setAutoCommit(false);
            try {
                // Even a no-op UPDATE creates MariaDB system history; duplicates must be read-only.
                final SubsetReplicationDto stored = find(connection, database, incoming.queryId(), true);
                if (stored != null && (!sameIdentity(stored, incoming)
                        || (stored.revision() == incoming.revision() && (!stored.persisted().equals(incoming.persisted())
                            || !Objects.equals(stored.snapshotHash(), incoming.snapshotHash())))
                        || (incoming.revision() > stored.revision() && stored.snapshotHash() != null
                            && !stored.snapshotHash().equals(incoming.snapshotHash())))) {
                    throw conflict("Canonical subset identity or revision conflicts with the stored query");
                }
                final boolean changed = stored == null || incoming.revision() > stored.revision();
                if (changed && incoming.originSite().equals(localSite)) {
                    throw conflict("A peer cannot advance a locally originated subset");
                }
                if (stored == null) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO qs_queries (id, created_by, query, query_normalized, executed,
                            is_persisted, query_hash, result_hash, result_number, creation_location, replication_revision, snapshot_hash, execution_context)
                        VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                        statement.setString(1, incoming.queryId().toString());
                        statement.setString(2, incoming.query());
                        statement.setString(3, incoming.queryNormalized());
                        statement.setTimestamp(4, Timestamp.from(incoming.selectedAt()), utc());
                        statement.setBoolean(5, incoming.persisted());
                        statement.setString(6, digest(incoming.query()));
                        statement.setString(7, incoming.resultHash());
                        statement.setLong(8, incoming.resultCount());
                        statement.setString(9, incoming.originSite());
                        statement.setLong(10, incoming.revision());
                        statement.setString(11, incoming.snapshotHash());
                        statement.setString(12, incoming.executionContext());
                        statement.executeUpdate();
                    }
                } else if (changed) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            UPDATE qs_queries SET is_persisted = ?, replication_revision = ?, snapshot_hash = ? WHERE id = ?
                            """)) {
                        statement.setBoolean(1, incoming.persisted());
                        statement.setLong(2, incoming.revision());
                        statement.setString(3, incoming.snapshotHash());
                        statement.setString(4, incoming.queryId().toString());
                        statement.executeUpdate();
                    }
                }
                if (changed) enqueue(connection, incoming.queryId());
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } finally {
            pool.close();
        }
    }

    private void validate(Database database, SubsetReplicationDto value) {
        if (value == null || !validator.validate(value).isEmpty()
                || !value.selectedAt().equals(value.selectedAt().truncatedTo(ChronoUnit.MICROS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid canonical subset state");
        }
        final String sender;
        final String origin;
        try {
            sender = peers.requireAllowedSite(value.senderSite());
            origin = value.originSite().equals(localSite) ? localSite : peers.requireAllowedSite(value.originSite());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Untrusted subset replication site");
        }
        if (!sender.equals(value.senderSite()) || !origin.equals(value.originSite())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subset sites must use canonical origins");
        }
        if (!value.senderDatabaseId().equals(routes(database).get(sender))) {
            throw conflict("Subset sender database mapping is missing or does not match");
        }
    }

    private boolean sameIdentity(SubsetReplicationDto a, SubsetReplicationDto b) {
        return Objects.equals(a.originSite(), b.originSite()) && a.query().equals(b.query())
                && a.queryNormalized().equals(b.queryNormalized()) && a.selectedAt().equals(b.selectedAt())
                && Objects.equals(a.executionContext(), b.executionContext())
                && (a.snapshotHash() == null || b.snapshotHash() == null || a.snapshotHash().equals(b.snapshotHash()))
                && Objects.equals(a.resultHash(), b.resultHash()) && Objects.equals(a.resultCount(), b.resultCount());
    }

    public void requireSender(Database database, String site, UUID sourceId) {
        final String allowed;
        try { allowed = peers.requireAllowedSite(site); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Untrusted subset sender"); }
        if (!allowed.equals(site) || sourceId == null || !sourceId.equals(routes(database).get(allowed))) {
            throw conflict("Subset sender database mapping is missing or does not match");
        }
    }

    public SubsetResultReader openResult(Database database, Subset subset) throws SQLException, QueryExecutionException {
        return results.open(database, subset);
    }

    /** Upgrade a known local v2 query only when its materialized observation still matches its original reference. */
    public void captureForPersistence(Connection connection, Database database, UUID id) throws SQLException {
        final SubsetReplicationDto before = read(connection, database, id);
        requireLocalOrigin(before.originSite());
        if (before.snapshotHash() != null || before.originSite() == null || before.resultHash() == null
                || !before.resultHash().startsWith("v2:")) return;
        final String work = "_dbrepo_query_" + UUID.randomUUID().toString().replace("-", "");
        try (Statement statement = connection.createStatement()) {
            // CTAS commits in MariaDB; finish it before locking or changing canonical metadata.
            statement.execute("CREATE TABLE `" + work + "` AS " + before.queryNormalized());
            connection.setAutoCommit(false);
            try {
                final SubsetReplicationDto current = find(connection, database, id, true);
                if (current == null || !sameIdentity(before, current)) throw conflict("Subset changed during capture");
                if (current.snapshotHash() == null) {
                    try (CallableStatement capture = connection.prepareCall("{CALL _capture_subset_result(?,?,?,?)}")) {
                        capture.setString(1, work);
                        capture.setString(2, id.toString());
                        capture.setString(3, current.resultHash());
                        capture.setLong(4, current.resultCount());
                        capture.execute();
                    }
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE qs_queries q JOIN qs_subset_results r ON r.query_id=q.id
                            SET q.snapshot_hash=r.snapshot_hash,q.replication_revision=q.replication_revision+1 WHERE q.id=?
                            """)) {
                        update.setString(1, id.toString());
                        update.executeUpdate();
                    }
                    enqueue(connection, id);
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } finally {
            try (Statement statement = connection.createStatement()) { statement.execute("DROP TABLE IF EXISTS `" + work + "`"); }
        }
    }

    public void requireLocalOrigin(String origin) {
        if (origin != null && !origin.equals(localSite)) {
            throw conflict("Change persistence at the subset origin: " + origin);
        }
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    public String localSite() { return localSite; }

    private static Calendar utc() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    private static String digest(String query) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(query.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
