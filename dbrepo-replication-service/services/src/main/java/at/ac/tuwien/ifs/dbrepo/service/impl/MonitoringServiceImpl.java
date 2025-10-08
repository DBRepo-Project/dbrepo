package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringReplicaDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringTableDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.MonitoringService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl extends DataConnector implements MonitoringService {

    private final MetadataServiceGateway metadataServiceGateway;

    @Override
    @Observed(name = "replication_monitoring_status")
    public ReplicationMonitoringDatabaseDto status(UUID databaseId) throws RemoteUnavailableException, MetadataServiceException, at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException {
        log.info("Starting monitoring status check for database {}", databaseId);

        final DatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final List<TableBriefDto> tables = metadataServiceGateway.getTablesByDatabaseId(databaseId);

        final ReplicationMonitoringDatabaseDto.ReplicationMonitoringDatabaseDtoBuilder dbBuilder = ReplicationMonitoringDatabaseDto.builder()
                .id(database.getId())
                .name(database.getName())
                .internalName(database.getInternalName())
                .tableCount(tables.size());

        // Per-table tuple counts: direct COUNT(*) via JDBC; for versioned tables include history using FOR SYSTEM_TIME ALL
        final java.util.List<ReplicationMonitoringTableDto> tableSummaries = new java.util.ArrayList<>();
        long totalLifetimeTuples = 0L;
        final com.mchange.v2.c3p0.ComboPooledDataSource dataSource = getDataSource(database);
        try (java.sql.Connection connection = dataSource.getConnection()) {
            for (TableBriefDto table : tables) {
                final long count = countTuples(connection, database.getInternalName(), table.getInternalName(), Boolean.TRUE.equals(table.getIsVersioned()));
                totalLifetimeTuples += count;
                final ReplicationMonitoringTableDto.ReplicationMonitoringTableDtoBuilder tableBuilder = ReplicationMonitoringTableDto.builder()
                        .id(table.getId())
                        .name(table.getName())
                        .internalName(table.getInternalName())
                        .tupleCount(count);
                // Per-replica replication/missing tuples breakdown
                if (database.getReplicaUrls() != null && !database.getReplicaUrls().isEmpty()) {
                    final java.util.List<ReplicationMonitoringReplicaDto> replicaSummaries = new java.util.ArrayList<>();
                    for (java.util.Map.Entry<String, java.util.UUID> replica : database.getReplicaUrls().entrySet()) {
                        final String siteUrl = replica.getKey();
                        final long replicatedCount = countRemoteTuples(connection, database.getInternalName(), table.getInternalName(), siteUrl, java.time.Instant.now());
                        final long missingCount = Math.max(0L, count - replicatedCount);
                        replicaSummaries.add(ReplicationMonitoringReplicaDto.builder()
                                .siteUrl(siteUrl)
                                .remoteDatabaseId(replica.getValue())
                                .replicatedCount(replicatedCount)
                                .missingCount(missingCount)
                                .build());
                    }
                    tableBuilder.replicas(replicaSummaries);
                }
                tableSummaries.add(tableBuilder.build());
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to query database counts: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }

        return dbBuilder
                .lifetimeTupleCount(totalLifetimeTuples)
                .tables(tableSummaries)
                .build();
    }

    private long countTuples(java.sql.Connection connection, String databaseName, String tableInternalName, boolean isVersioned) {
        final StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableInternalName)
                .append("`");
        if (isVersioned) {
            sql.append(" FOR SYSTEM_TIME ALL");
        }
        sql.append(";");
        try (java.sql.Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql.toString())) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to count tuples for table " + databaseName + "." + tableInternalName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Counts tuples for a specific remote site by joining tuple_replication_timestamps on the table's replication_key.
     * Uses point-in-time semantics similar to modifyQueryWithReplicationTimestamps by applying row_start/row_end bounds.
     */
    private long countRemoteTuples(java.sql.Connection connection, String databaseName, String tableInternalName, String siteUrl,
                                  java.time.Instant asOf) {
        if (asOf == null) {
            asOf = java.time.Instant.now();
        }
        final StringBuilder sql = new StringBuilder()
                .append("SELECT COUNT(1) FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableInternalName)
                .append("` t ")
                .append("JOIN tuple_replication_timestamps trt ON trt.replication_id = t.`replication_key` AND trt.site_url = ? ")
                .append("WHERE trt.row_start <= ? AND (trt.row_end IS NULL OR trt.row_end > ?);");
        try (java.sql.PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setString(1, siteUrl);
            final java.sql.Timestamp ts = java.sql.Timestamp.from(asOf);
            ps.setTimestamp(2, ts);
            ps.setTimestamp(3, ts);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to count remote tuples for table " + databaseName + "." + tableInternalName + ": " + e.getMessage(), e);
        }
    }
}


