package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationHealthDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringReplicaDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringSiteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationServiceHealthDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.LocalTableIdDto;
import at.ac.tuwien.ifs.dbrepo.service.MonitoringService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl extends DataConnector implements MonitoringService {

    private final MetadataServiceGateway metadataServiceGateway;
    private final @Qualifier("healthCheckRestTemplate") RestTemplate healthCheckRestTemplate;

    @Value("${BASE_URL:http://localhost:8080}")
    private String localBaseUrl;

    @Value("${monitoring.lag.threshold.seconds:300}")
    private long lagThresholdSeconds;

    @Value("${monitoring.missing.threshold.fraction:0.05}")
    private double missingFractionThreshold;

    @Override
    @Observed(name = "replication_monitoring_status")
    public ReplicationMonitoringDatabaseDto status(UUID databaseId)
            throws RemoteUnavailableException, MetadataServiceException,
            at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException {
        log.info("Starting monitoring status check for database {}", databaseId);

        final DatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final List<TableBriefDto> tables = metadataServiceGateway.getTablesByDatabaseId(databaseId);
        final Map<String, UUID> replicaUrls =
                database.getReplicaUrls() != null ? database.getReplicaUrls() : Map.of();

        final Map<String, SiteAccumulator> siteAccumulators = initialiseSiteAccumulators(database);

        // Map table IDs to their internal names for later joins when computing latency
        final Map<UUID, String> tableInternalNames = new HashMap<>();
        for (TableBriefDto table : tables) {
            tableInternalNames.put(table.getId(), table.getInternalName());
        }

        final String primarySiteUrl = database.getCreationLocation() != null
                ? database.getCreationLocation()
                : localBaseUrl;
        final String primarySiteKey = normalizeUrl(primarySiteUrl);

        final List<ReplicationMonitoringTableDto> tableSummaries = new ArrayList<>();
        long totalLifetimeTuples = 0L;

        log.info("Monitoring {} tables and {} replica sites for database {}", tables.size(),
                replicaUrls.size(), databaseId);

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            log.info("Opened JDBC connection for monitoring database {}", database.getInternalName());
            for (TableBriefDto table : tables) {
                log.info("Monitoring table {} ({})", table.getName(), table.getInternalName());
                final TableMonitoringResult tableResult = monitorTable(
                        connection,
                        database,
                        table,
                        replicaUrls,
                        siteAccumulators,
                        primarySiteKey
                );
                tableSummaries.add(tableResult.tableDto());
                totalLifetimeTuples += tableResult.tupleCount();
            }

            // Compute per-site latency (average over the last 10 tuples across all tables)
            // while we still have a connection
            log.info("Computing per-site latency for {} sites (primarySiteUrl={})",
                    siteAccumulators.size(), primarySiteUrl);
            for (SiteAccumulator accumulator : siteAccumulators.values()) {
                try {
                    accumulator.latencyMs = computeAverageLatencyMillis(
                            connection,
                            database,
                            tableInternalNames,
                            accumulator.siteUrl,
                            primarySiteUrl
                    );
                    log.info("Computed latency for site {} in database {}: {} ms (null means no samples)",
                            accumulator.siteUrl, database.getId(), accumulator.latencyMs);
                } catch (SQLException e) {
                    log.warn("Failed to compute latency for site {} in database {}: {}",
                            accumulator.siteUrl, database.getId(), e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query monitoring data for database {}: {}", databaseId, e.getMessage(), e);
            throw new RuntimeException("Failed to query database counts: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }

        final List<ReplicationMonitoringSiteDto> siteSummaries = buildSiteSummaries(siteAccumulators);

        return ReplicationMonitoringDatabaseDto.builder()
                .id(database.getId())
                .name(database.getName())
                .internalName(database.getInternalName())
                .tableCount(tables.size())
                .lifetimeTupleCount(totalLifetimeTuples)
                .tables(tableSummaries)
                .sites(siteSummaries)
                .build();
    }

    private Map<String, SiteAccumulator> initialiseSiteAccumulators(DatabaseDto database) {
        final Map<String, SiteAccumulator> siteAccumulators = new LinkedHashMap<>();
        addSiteAccumulator(siteAccumulators, database.getCreationLocation());
        addSiteAccumulator(siteAccumulators, localBaseUrl);
        if (database.getReplicaUrls() != null) {
            for (String siteUrl : database.getReplicaUrls().keySet()) {
                addSiteAccumulator(siteAccumulators, siteUrl);
            }
        }
        return siteAccumulators;
    }

    private void addSiteAccumulator(Map<String, SiteAccumulator> map, String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return;
        }
        final String normalized = normalizeUrl(siteUrl);
        map.computeIfAbsent(normalized, key -> new SiteAccumulator(siteUrl));
    }

    private TableMonitoringResult monitorTable(Connection connection,
                                               DatabaseDto database,
                                               TableBriefDto table,
                                               Map<String, UUID> replicaUrls,
                                               Map<String, SiteAccumulator> siteAccumulators,
                                               String primarySiteKey) throws SQLException {
        final String databaseName = database.getInternalName();
        final String tableInternalName = table.getInternalName();
        final boolean versioned = Boolean.TRUE.equals(table.getIsVersioned());

        final long tupleCount = countTuples(connection, databaseName, tableInternalName, versioned);
        final Instant primaryLatestTimestamp = resolvePrimaryReferenceTimestamp(connection, database, tableInternalName);

        final List<ReplicationMonitoringReplicaDto> replicaSummaries = new ArrayList<>();
        final List<String> tableAnomalies = new ArrayList<>();
        final Instant now = Instant.now();

        for (Map.Entry<String, UUID> replica : replicaUrls.entrySet()) {
            final String siteUrl = replica.getKey();
            final String siteKey = normalizeUrl(siteUrl);
            final SiteAccumulator accumulator = siteAccumulators.computeIfAbsent(siteKey, key -> new SiteAccumulator(siteUrl));

            final long replicatedCount = countRemoteTuples(connection, databaseName, tableInternalName, siteUrl, now);
            final long missingCount = Math.max(0L, tupleCount - replicatedCount);
            final Instant replicaLatestTimestamp = fetchMaxRowStartForSite(connection, databaseName, tableInternalName, siteUrl);

            final Long lagSeconds = computeLagSeconds(primaryLatestTimestamp, replicaLatestTimestamp);
            final double missingFraction = tupleCount == 0L ? 0D : (double) missingCount / (double) tupleCount;

            final List<String> replicaAnomalies = new ArrayList<>(findTemporalInconsistencies(connection, databaseName, tableInternalName, siteUrl));
            if (missingCount > 0L) {
                replicaAnomalies.add("missing " + missingCount + " tuples");
            }
            if (lagSeconds != null && lagSeconds > lagThresholdSeconds) {
                replicaAnomalies.add("lag " + lagSeconds + "s exceeds threshold");
            }
            if (!replicaAnomalies.isEmpty()) {
                for (String anomaly : replicaAnomalies) {
                    tableAnomalies.add(siteUrl + ": " + anomaly);
                }
            }

            updateSiteAccumulator(accumulator, tupleCount, replicatedCount, missingCount, lagSeconds,
                    primaryLatestTimestamp, replicaLatestTimestamp, replicaAnomalies);

            final String replicaStatus = resolveReplicaStatus(missingFraction, lagSeconds, replicaAnomalies);

            replicaSummaries.add(ReplicationMonitoringReplicaDto.builder()
                    .siteUrl(siteUrl)
                    .remoteDatabaseId(replica.getValue())
                    .replicatedCount(replicatedCount)
                    .missingCount(missingCount)
                    .missingFraction(roundFraction(missingFraction))
                    .status(replicaStatus)
                    .lagSeconds(lagSeconds)
                    .latestPrimaryTimestamp(primaryLatestTimestamp)
                    .latestReplicaTimestamp(replicaLatestTimestamp)
                    .anomalies(replicaAnomalies.isEmpty() ? Collections.emptyList() : replicaAnomalies)
                    .build());
        }

        // Primary or local site accumulator should reflect the source tuple count.
        if (primarySiteKey != null) {
            final SiteAccumulator primaryAccumulator = siteAccumulators.get(primarySiteKey);
            if (primaryAccumulator != null) {
                updateSiteAccumulator(primaryAccumulator, tupleCount, tupleCount, 0L, 0L,
                        primaryLatestTimestamp, primaryLatestTimestamp, Collections.emptyList());
            }
        }

        final ReplicationMonitoringTableDto tableDto = ReplicationMonitoringTableDto.builder()
                .id(table.getId())
                .name(table.getName())
                .internalName(tableInternalName)
                .tupleCount(tupleCount)
                .replicas(replicaSummaries)
                .anomalies(tableAnomalies.isEmpty() ? Collections.emptyList() : tableAnomalies)
                .build();

        return new TableMonitoringResult(tupleCount, tableDto);
    }

    private void updateSiteAccumulator(SiteAccumulator accumulator,
                                       long expectedCount,
                                       long replicatedCount,
                                       long missingCount,
                                       Long lagSeconds,
                                       Instant primaryLatest,
                                       Instant replicaLatest,
                                       List<String> replicaAnomalies) {
        accumulator.totalExpectedTuples += expectedCount;
        accumulator.totalReplicatedTuples += replicatedCount;
        accumulator.totalMissingTuples += missingCount;
        if (lagSeconds != null) {
            accumulator.maxObservedLagSeconds = Math.max(accumulator.maxObservedLagSeconds, lagSeconds);
        }
        if (primaryLatest != null) {
            accumulator.latestPrimaryTimestamp = moreRecent(accumulator.latestPrimaryTimestamp, primaryLatest);
        }
        if (replicaLatest != null) {
            accumulator.latestReplicaTimestamp = moreRecent(accumulator.latestReplicaTimestamp, replicaLatest);
        }
        if (replicaAnomalies != null && !replicaAnomalies.isEmpty()) {
            accumulator.anomalies.addAll(replicaAnomalies);
        }
    }

    private List<ReplicationMonitoringSiteDto> buildSiteSummaries(Map<String, SiteAccumulator> siteAccumulators) {
        final List<ReplicationMonitoringSiteDto> summaries = new ArrayList<>();
        final Instant snapshot = Instant.now();

        for (SiteAccumulator accumulator : siteAccumulators.values()) {
            final double missingFraction = accumulator.totalExpectedTuples == 0L
                    ? 0D
                    : (double) accumulator.totalMissingTuples / (double) accumulator.totalExpectedTuples;
            final boolean metricsDegraded = missingFraction > missingFractionThreshold
                    || accumulator.maxObservedLagSeconds > lagThresholdSeconds
                    || !accumulator.anomalies.isEmpty();

            // Integrate remote /api/replication/health from each site, if reachable
            ReplicationHealthDto remoteHealth = null;
            boolean siteUnreachable = false;
            try {
                remoteHealth = fetchRemoteHealth(accumulator.siteUrl);
                if (remoteHealth == null) {
                    // fetchRemoteHealth returned null (e.g., empty response or non-2xx status)
                    siteUnreachable = true;
                }
            } catch (Exception e) {
                log.warn("Remote health check failed for site {}: {}", accumulator.siteUrl, e.getMessage());
                siteUnreachable = true;
            }

            final ReplicationServiceHealthDto metadataHealth =
                    remoteHealth != null ? remoteHealth.getMetadataService() : null;
            final ReplicationServiceHealthDto dataHealth =
                    remoteHealth != null ? remoteHealth.getDataService() : null;
            final ReplicationServiceHealthDto replicationHealth =
                    remoteHealth != null ? remoteHealth.getReplicationService() : null;
            final ReplicationServiceHealthDto brokerHealth =
                    remoteHealth != null ? remoteHealth.getBroker() : null;

            final boolean replicationUp = replicationHealth != null
                    && "UP".equalsIgnoreCase(String.valueOf(replicationHealth.getStatus()));
            final boolean metadataUp = metadataHealth != null
                    && "UP".equalsIgnoreCase(String.valueOf(metadataHealth.getStatus()));
            final boolean dataUp = dataHealth != null
                    && "UP".equalsIgnoreCase(String.valueOf(dataHealth.getStatus()));
            final boolean brokerUp = brokerHealth != null
                    && "UP".equalsIgnoreCase(String.valueOf(brokerHealth.getStatus()));

            final boolean serviceDegraded = remoteHealth != null
                    && !"UP".equalsIgnoreCase(String.valueOf(remoteHealth.getStatus()));

            // Determine final status: unreachable > degraded > healthy
            final String status;
            if (siteUnreachable) {
                status = "unreachable";
            } else if (metricsDegraded || serviceDegraded) {
                status = "degraded";
            } else {
                status = "healthy";
            }
            final String message = buildSiteMessage(accumulator, missingFraction);

            summaries.add(ReplicationMonitoringSiteDto.builder()
                    .siteUrl(accumulator.siteUrl)
                    .status(status)
                    .replicationServiceReachable(replicationUp)
                    .metadataServiceReachable(metadataUp)
                    .dataServiceReachable(dataUp)
                    .brokerReachable(brokerUp)
                    .latencyMs(accumulator.latencyMs)
                    .lastChecked(snapshot)
                    .message(message)
                    .build());
        }

        return summaries;
    }

    private String buildSiteMessage(SiteAccumulator accumulator, double missingFraction) {
        final StringBuilder message = new StringBuilder();
        message.append("replicated=")
                .append(accumulator.totalReplicatedTuples)
                .append("/")
                .append(accumulator.totalExpectedTuples);
        if (accumulator.totalExpectedTuples > 0) {
            message.append(" (missing=")
                    .append(accumulator.totalMissingTuples)
                    .append(", fraction=")
                    .append(roundFraction(missingFraction))
                    .append(")");
        }
        if (accumulator.maxObservedLagSeconds > 0L) {
            message.append("; maxLag=")
                    .append(accumulator.maxObservedLagSeconds)
                    .append("s");
        }
        if (!accumulator.anomalies.isEmpty()) {
            message.append("; anomalies=")
                    .append(String.join(", ", accumulator.anomalies));
        }
        return message.toString();
    }

    private long countTuples(Connection connection, String databaseName, String tableInternalName, boolean isVersioned)
            throws SQLException {
        final StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableInternalName)
                .append("`");
        if (isVersioned) {
            sql.append(" FOR SYSTEM_TIME ALL");
        }
        sql.append(";");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        }
    }

    private long countRemoteTuples(Connection connection,
                                   String databaseName,
                                   String tableInternalName,
                                   String siteUrl,
                                   Instant asOf) throws SQLException {
        final StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(1) FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableInternalName)
                .append("` t ")
                .append("JOIN tuple_replication_timestamps trt ")
                .append("ON trt.replication_id = t.`replication_key` AND trt.site_url = ? ")
                .append("WHERE trt.row_start <= ? AND (trt.row_end IS NULL OR trt.row_end > ?);");
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setString(1, siteUrl);
            final Timestamp ts = Timestamp.from(asOf);
            ps.setTimestamp(2, ts);
            ps.setTimestamp(3, ts);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        }
    }

    /**
     * Computes the average replication latency in milliseconds for a site based on the last 10 tuples
     * across all tables for that siteUrl. Latency is defined as (remote replication timestamp - local tuple timestamp).
     * <p>
     * NOTE: We deliberately do NOT filter by database_id because the remote site may use a different
     * databaseId for the same logical database. Instead, we later filter by known table_ids.
     */
    private Long computeAverageLatencyMillis(Connection connection,
                                             DatabaseDto database,
                                             Map<UUID, String> tableInternalNames,
                                             String siteUrl,
                                             String primarySiteUrl) throws SQLException {
        if (siteUrl == null || primarySiteUrl == null) {
            return null;
        }
        final String normalizedSite = normalizeUrl(siteUrl);
        final String normalizedPrimary = normalizeUrl(primarySiteUrl);

        // For the primary site, latency is effectively 0
        if (Objects.equals(normalizedSite, normalizedPrimary)) {
            log.debug("computeAverageLatencyMillis: site {} is primary ({}), returning 0 ms",
                    siteUrl, primarySiteUrl);
            return 0L;
        }

        // Last 10 replication events for this site over all tables
        final String latestEventsSql = """
                SELECT table_id, replication_id, row_start AS remote_start
                FROM tuple_replication_timestamps
                WHERE site_url = ?
                ORDER BY row_start DESC
                LIMIT 10
                """;

        long sumMillis = 0L;
        int count = 0;

        try (PreparedStatement ps = connection.prepareStatement(latestEventsSql)) {
            ps.setString(1, siteUrl);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final UUID remoteTableId = UUID.fromString(rs.getString("table_id"));
                    final String replicationId = rs.getString("replication_id");
                    final Timestamp remoteStartTs = rs.getTimestamp("remote_start");
                    if (replicationId == null || remoteStartTs == null) {
                        continue;
                    }

                    // Resolve remote tableId to local tableId via metadata service (cached there)
                    final UUID localTableId = resolveLocalTableId(database.getId(), remoteTableId);
                    if (localTableId == null) {
                        log.info("Latency: could not resolve local tableId for remoteTableId {} at site {}, skipping",
                                remoteTableId, siteUrl);
                        continue;
                    }

                    final String tableInternalName = tableInternalNames.get(localTableId);
                    if (tableInternalName == null) {
                        log.info("Latency: no internal name for local tableId {} (remoteTableId {}) at site {}, skipping",
                                localTableId, remoteTableId, siteUrl);
                        continue;
                    }

                    final Timestamp localStartTs = lookupLocalTupleTimestamp(
                            connection,
                            database.getInternalName(),
                            tableInternalName,
                            replicationId
                    );
                    if (localStartTs == null) {
                        log.info("Latency: no local tuple timestamp for table {} / replicationId {} at site {}",
                                tableInternalName, replicationId, siteUrl);
                        continue;
                    }

                    final long diff = remoteStartTs.getTime() - localStartTs.getTime();
                    if (diff >= 0L) {
                        sumMillis += diff;
                        count++;
                        log.info("Latency sample for site {}: table={}, replicationId={}, remoteStart={}, localStart={}, diffMs={}",
                                siteUrl, tableInternalName, replicationId, remoteStartTs, localStartTs, diff);
                    }
                }
            }
        }

        if (count == 0) {
            log.info("computeAverageLatencyMillis: no latency samples for site {} in database {}", siteUrl, database.getId());
            return null;
        }
        final long avg = sumMillis / count;
        log.info("computeAverageLatencyMillis: site {} in database {} has {} samples, avgLatencyMs={}",
                siteUrl, database.getId(), count, avg);
        return avg;
    }

    /**
     * Looks up the local tuple timestamp (row_start) in the base table for a given replication_id.
     */
    private Timestamp lookupLocalTupleTimestamp(Connection connection,
                                                String databaseName,
                                                String tableInternalName,
                                                String replicationId) throws SQLException {
        final StringBuilder sql = new StringBuilder()
                .append("SELECT MAX(t.row_start) AS local_start FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableInternalName)
                .append("` t WHERE t.`replication_key` = ?;");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setString(1, replicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("local_start");
                }
            }
        }
        return null;
    }

    /**
     * Calls the remote replication-service health endpoint on a given site and returns the parsed DTO.
     * Uses a short-timeout RestTemplate to fail fast if the site is unreachable.
     */
    private ReplicationHealthDto fetchRemoteHealth(String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return null;
        }
        final String base = normalizeUrl(siteUrl);
        final String url = base + "/api/replication/health";
        try {
            final ResponseEntity<ReplicationHealthDto> response =
                    healthCheckRestTemplate.getForEntity(url, ReplicationHealthDto.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Remote health check for {} returned non-2xx status {}", siteUrl, response.getStatusCode());
                return null;
            }
            return response.getBody();
        } catch (RestClientException e) {
            log.warn("Failed to call remote health endpoint {}: {}", url, e.getMessage());
            throw e;  // Re-throw so caller knows the site is unreachable
        }
    }

    /**
     * Resolve a remote (replica) tableId to the local tableId using the metadata service gateway.
     * The gateway implementation already caches results, so we simply delegate.
     */
    private UUID resolveLocalTableId(UUID databaseId, UUID remoteTableId) {
        try {
            final LocalTableIdDto dto = metadataServiceGateway.getLocalTableIdByReplicaTableId(databaseId, remoteTableId);
            if (dto != null && dto.getLocalTableId() != null) {
                return dto.getLocalTableId();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve local tableId for remoteTableId {} in database {}: {}",
                    remoteTableId, databaseId, e.getMessage());
        }
        return null;
    }

    private Instant fetchMaxRowStartForSite(Connection connection,
                                            String databaseName,
                                            String tableInternalName,
                                            String siteUrl) throws SQLException {
        if (siteUrl == null || siteUrl.isBlank()) {
            return null;
        }
        final StringBuilder sql = new StringBuilder()
                .append("SELECT MAX(trt.row_start) AS max_row_start FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableInternalName)
                .append("` t ")
                .append("JOIN tuple_replication_timestamps trt ")
                .append("ON trt.replication_id = t.`replication_key` ")
                .append("WHERE trt.site_url = ?;");
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setString(1, siteUrl);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final Timestamp ts = rs.getTimestamp("max_row_start");
                    return ts != null ? ts.toInstant() : null;
                }
            }
        }
        return null;
    }

    private Instant resolvePrimaryReferenceTimestamp(Connection connection,
                                                     DatabaseDto database,
                                                     String tableInternalName) throws SQLException {
        final String primarySite = database.getCreationLocation() != null
                ? database.getCreationLocation()
                : localBaseUrl;
        return fetchMaxRowStartForSite(connection, database.getInternalName(), tableInternalName, primarySite);
    }

    private List<String> findTemporalInconsistencies(Connection connection,
                                                     String databaseName,
                                                     String tableInternalName,
                                                     String siteUrl) {
        if (siteUrl == null || siteUrl.isBlank()) {
            return Collections.emptyList();
        }

        final StringBuilder sql = new StringBuilder()
                .append("SELECT trt.replication_id, trt.row_start, trt.row_end ")
                .append("FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableInternalName)
                .append("` t ")
                .append("JOIN tuple_replication_timestamps trt ")
                .append("ON trt.replication_id = t.`replication_key` ")
                .append("WHERE trt.site_url = ? ")
                .append("ORDER BY trt.replication_id, trt.row_start;");

        final List<String> anomalies = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setString(1, siteUrl);
            try (ResultSet rs = ps.executeQuery()) {
                String currentReplicationId = null;
                Timestamp previousRowEnd = null;
                boolean previousWasOpen = false;

                while (rs.next()) {
                    final String replicationId = rs.getString("replication_id");
                    final Timestamp rowStart = rs.getTimestamp("row_start");
                    final Timestamp rowEnd = rs.getTimestamp("row_end");

                    if (!Objects.equals(currentReplicationId, replicationId)) {
                        currentReplicationId = replicationId;
                        previousRowEnd = null;
                        previousWasOpen = false;
                    }

                    if (previousRowEnd != null && rowStart != null && previousRowEnd.after(rowStart)) {
                        anomalies.add("replication " + replicationId + " has overlapping intervals");
                    }
                    if (previousWasOpen && rowEnd == null) {
                        anomalies.add("replication " + replicationId + " has multiple open intervals");
                    }

                    previousRowEnd = rowEnd;
                    previousWasOpen = rowEnd == null;
                }
            }
        } catch (SQLException e) {
            log.warn("Temporal consistency check failed for {}.{} at {}: {}", databaseName, tableInternalName, siteUrl, e.getMessage());
            anomalies.add("temporal consistency check failed: " + e.getMessage());
        }

        return anomalies;
    }

    private Long computeLagSeconds(Instant primary, Instant replica) {
        if (primary == null || replica == null) {
            return null;
        }
        final long seconds = Duration.between(replica, primary).getSeconds();
        return Math.max(seconds, 0L);
    }

    private String resolveReplicaStatus(double missingFraction,
                                        Long lagSeconds,
                                        List<String> replicaAnomalies) {
        boolean degraded = missingFraction > missingFractionThreshold;
        if (!degraded && lagSeconds != null) {
            degraded = lagSeconds > lagThresholdSeconds;
        }
        if (!degraded && replicaAnomalies != null && !replicaAnomalies.isEmpty()) {
            degraded = true;
        }
        return degraded ? "degraded" : "healthy";
    }

    private Double roundFraction(double fraction) {
        if (Double.isNaN(fraction) || Double.isInfinite(fraction)) {
            return null;
        }
        return Math.round(fraction * 10_000D) / 10_000D;
    }

    private Instant moreRecent(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return right.isAfter(left) ? right : left;
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public String deriveGlobalStatus(ReplicationMonitoringDatabaseDto dto) {
        if (dto == null || dto.getSites() == null || dto.getSites().isEmpty()) {
            return "healthy";
        }
        // Check for unreachable first (worst status)
        final boolean hasUnreachable = dto.getSites().stream()
                .anyMatch(site -> site.getStatus() != null && site.getStatus().equalsIgnoreCase("unreachable"));
        if (hasUnreachable) {
            return "unreachable";
        }
        final boolean hasDegraded = dto.getSites().stream()
                .anyMatch(site -> site.getStatus() != null && site.getStatus().equalsIgnoreCase("degraded"));
        return hasDegraded ? "degraded" : "healthy";
    }

    @Override
    public HttpStatus mapStatusToHttp(String status) {
        if (status == null) {
            return HttpStatus.OK;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "degraded" -> HttpStatus.MULTI_STATUS;
            case "unreachable" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.OK;
        };
    }

    private record TableMonitoringResult(long tupleCount, ReplicationMonitoringTableDto tableDto) {
    }

    private static final class SiteAccumulator {
        private final String siteUrl;
        private long totalExpectedTuples;
        private long totalReplicatedTuples;
        private long totalMissingTuples;
        private long maxObservedLagSeconds;
        private Instant latestPrimaryTimestamp;
        private Instant latestReplicaTimestamp;
        private final List<String> anomalies = new ArrayList<>();

        // Average latency in ms over the last 10 tuples (computed in status)
        private Long latencyMs;

        private SiteAccumulator(String siteUrl) {
            this.siteUrl = siteUrl;
        }
    }
}

