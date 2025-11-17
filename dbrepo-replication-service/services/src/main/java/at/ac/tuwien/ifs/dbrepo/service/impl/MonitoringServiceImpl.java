package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringReplicaDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringSiteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringTableDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.MonitoringService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

        final String primarySiteUrl = database.getCreationLocation() != null
                ? database.getCreationLocation()
                : localBaseUrl;
        final String primarySiteKey = normalizeUrl(primarySiteUrl);

        final List<ReplicationMonitoringTableDto> tableSummaries = new ArrayList<>();
        long totalLifetimeTuples = 0L;

        final ComboPooledDataSource dataSource = getDataSource(database);
        try (Connection connection = dataSource.getConnection()) {
            for (TableBriefDto table : tables) {
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

            // Compute per-site latency (average over the last 10 tuples) while we still have a connection
            for (SiteAccumulator accumulator : siteAccumulators.values()) {
                try {
                    accumulator.latencyMs = computeAverageLatencyMillis(
                            connection,
                            database.getId(),
                            accumulator.siteUrl,
                            primarySiteUrl
                    );
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
            final boolean degraded = missingFraction > missingFractionThreshold
                    || accumulator.maxObservedLagSeconds > lagThresholdSeconds
                    || !accumulator.anomalies.isEmpty();
            final String status = degraded ? "degraded" : "healthy";
            final String message = buildSiteMessage(accumulator, missingFraction);

            summaries.add(ReplicationMonitoringSiteDto.builder()
                    .siteUrl(accumulator.siteUrl)
                    .status(status)
                    .replicationServiceReachable(Boolean.TRUE)
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
     * Computes the average replication latency in milliseconds for a site based on the last 10 tuples.
     * For each replication_id, it compares the primary site's row_start with this site's row_start.
     */
    private Long computeAverageLatencyMillis(Connection connection,
                                             UUID databaseId,
                                             String siteUrl,
                                             String primarySiteUrl) throws SQLException {
        if (siteUrl == null || primarySiteUrl == null) {
            return null;
        }
        final String normalizedSite = normalizeUrl(siteUrl);
        final String normalizedPrimary = normalizeUrl(primarySiteUrl);

        // For the primary site, latency is effectively 0
        if (Objects.equals(normalizedSite, normalizedPrimary)) {
            return 0L;
        }

        final String sql = """
                SELECT p.row_start AS primary_start, r.row_start AS replica_start
                FROM tuple_replication_timestamps r
                JOIN tuple_replication_timestamps p
                  ON p.replication_id = r.replication_id
                 AND p.database_id   = r.database_id
                 AND p.table_id      = r.table_id
                WHERE r.database_id = ?
                  AND r.site_url    = ?
                  AND p.site_url    = ?
                ORDER BY r.row_start DESC
                LIMIT 10
                """;

        long sumMillis = 0L;
        int count = 0;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, databaseId.toString());
            ps.setString(2, siteUrl);
            ps.setString(3, primarySiteUrl);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final Timestamp primaryStartTs = rs.getTimestamp("primary_start");
                    final Timestamp replicaStartTs = rs.getTimestamp("replica_start");
                    if (primaryStartTs == null || replicaStartTs == null) {
                        continue;
                    }
                    final long diff = replicaStartTs.getTime() - primaryStartTs.getTime();
                    if (diff >= 0L) {
                        sumMillis += diff;
                        count++;
                    }
                }
            }
        }

        if (count == 0) {
            return null;
        }
        return sumMillis / count;
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

