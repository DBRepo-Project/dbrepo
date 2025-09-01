package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationTimestampService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    private final ReplicationService replicationService;
    private final RestTemplate externalReplicationRestTemplate;
    private final MetadataServiceGateway metadataServiceGateway;
    private final ReplicationTimestampService replicationTimestampService;

    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public TableServiceImpl(ReplicationService replicationService, MetadataServiceGateway metadataServiceGateway,
                            @Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate,
                            ReplicationTimestampService replicationTimestampService) {
        this.replicationService = replicationService;
        this.metadataServiceGateway = metadataServiceGateway;
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
        this.replicationTimestampService = replicationTimestampService;
    }

    @Override
    public String handleTableReplication(TableNotificationDto tableNotificationDto) {

        if (tableNotificationDto.getReplicas() != null && !tableNotificationDto.getReplicas().isEmpty()) {
            log.info("Sending table replication to {} instances", tableNotificationDto.getReplicas().size());

            // Send replication to other instances
            replicationService.sendTableReplicationToInstances(tableNotificationDto);
        } else {
            log.info("No replica URLs provided, skipping table replication to other instances");
            System.out.println("No replica URLs to contact");
        }


        return tableNotificationDto.getCreationId().toString();
    }

    @Override
    public Map<String, Object> insertReplicatedTable(UUID databaseId, TableNotificationDto tableNotificationDto) {
        log.info("Creating table locally from replication notification");

        try {
            // Call the metadata service to create the table
            final String path = "/api/v1/database/" + databaseId + "/table/replicate";
            final Map<String, Object> response = metadataServiceGateway.createReplicatedTable(path, databaseId, tableNotificationDto);

            log.info("Table created successfully with ID: {}", response.get("tableId"));
            return response;

        } catch (Exception e) {
            log.error("Failed to create table from replication: {}", e.getMessage());
            return Map.of(
                    "status", "error",
                    "message", "Table creation failed: " + e.getMessage()
            );
        }
    }

    @Override
    public void handleDataReplication(DataReplicationDto dataReplicationDto) {
        final DatabaseDto database = dataReplicationDto.getDatabase();
        final TableDto table = dataReplicationDto.getTable();
        if (database == null || table == null || database.getReplicaUrls() == null || table.getReplicaUrls() == null) {
            log.info("No replica URLs provided for data replication; skipping fan-out");
            return;
        }

        // List to collect timestamps for each replica
        List<TupleReplicationTimestamp> timestampsToSave = new ArrayList<>();
        final Instant replicationStartTime = Instant.now();

        // For each replica URL in the database, send the tuple to its replication endpoint with the proper remote table id
        for (var entry : database.getReplicaUrls().entrySet()) {
            final String replicaUrl = entry.getKey();
            final java.util.UUID remoteDatabaseId = entry.getValue();
            final java.util.UUID remoteTableId = table.getReplicaUrls().get(replicaUrl);
            if (remoteTableId == null) {
                log.warn("Missing remote table id for replicaUrl={} in table replica map; skipping", replicaUrl);
                continue;
            }

            try {
                final String path = replicaUrl + "/api/v1/database/" + remoteDatabaseId + "/table/" + remoteTableId + "/data/replicate";
                log.info("Fan-out data replication to {}", path);
                final HttpEntity<DataReplicationDto> request = new HttpEntity<>(dataReplicationDto);
                ResponseEntity<java.util.Map> response = externalReplicationRestTemplate.exchange(path, HttpMethod.POST, request, java.util.Map.class);
                log.info("Fan-out replication response: {}", response.getStatusCode());

                final Object tsInserted = response.getBody().get("inserted_at");
                final Object tsDeleted = response.getBody().get("deleted_at");
                log.info("Remote timestamps from {}: inserted_at={}, deleted_at={}", replicaUrl, tsInserted, tsDeleted);
                log.debug("Full remote response body: {}", response.getBody());

                log.info("Replication ID: " + (String) dataReplicationDto.getTuple().get("replication_key"));


                // Create timestamp record for successful replication
                TupleReplicationTimestamp timestamp = TupleReplicationTimestamp.builder()
                        .siteUrl(replicaUrl)
                        .replicationId((String) dataReplicationDto.getTuple().get("replication_key"))
                        .databaseId(remoteDatabaseId)
                        .tableId(remoteTableId)
                        .rowStart(parseTimestamp((String) tsInserted))
                        .rowEnd(parseTimestamp((String) tsDeleted)) // Replication completed
                        .build();

                timestampsToSave.add(timestamp);
                log.debug("Added timestamp for successful replication to {}: {}", replicaUrl, timestamp.getReplicationId());

            } catch (Exception e) {
                log.error("Failed to replicate data to {}: {}", replicaUrl, e.getMessage());

            }
        }
        log.info("Start replicating timestamps");
        // After the loop, save all timestamps to the database
        if (!timestampsToSave.isEmpty()) {
            try {
                // Ensure the table exists before saving
                replicationTimestampService.ensureTableExists(database);
                replicationTimestampService.saveReplicationTimestamps(database, timestampsToSave);
                log.info("Saved {} replication timestamps to database", timestampsToSave.size());

                // add local timestamp to be included in full replication
                timestampsToSave.add(
                        TupleReplicationTimestamp.builder()
                                .siteUrl(baseUrl)
                                .replicationId((String) dataReplicationDto.getTuple().get("replication_key"))
                                .databaseId(database.getId())
                                .tableId(table.getId())
                                .rowStart(parseTimestamp((String) dataReplicationDto.getTuple().get("inserted_at")))
                                .rowEnd(parseTimestamp((String) dataReplicationDto.getTuple().get("deleted_at")))
                                .build());

                // Now replicate these timestamps to all other replica sites
                replicateTimestampsToAllSites(database, table, timestampsToSave);

            } catch (Exception e) {
                log.error("Failed to save replication timestamps: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Replicate timestamps to all other replica sites
     */
    private void replicateTimestampsToAllSites(DatabaseDto database, TableDto table, List<TupleReplicationTimestamp> timestamps) {
        if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
            log.info("No replica URLs available for timestamp replication");
            return;
        }

        // Get current site URL from BASE_URL environment variable
        String currentSiteUrl = this.baseUrl;

        // Iterate over each replica URL
        for (String replicaUrl : database.getReplicaUrls().keySet()) {
            // Skip current site
            if (replicaUrl.equals(currentSiteUrl)) {
                continue;
            }

            // Find the timestamp for this replica to get its database/table IDs
            TupleReplicationTimestamp replicaTimestamp = null;
            for (TupleReplicationTimestamp ts : timestamps) {
                if (ts.getSiteUrl().equals(replicaUrl)) {
                    replicaTimestamp = ts;
                    break;
                }
            }

            if (replicaTimestamp == null) {
                log.warn("No timestamp found for replica URL: {}", replicaUrl);
                continue;
            }

            // Get database and table IDs from this replica's timestamp
            UUID targetDatabaseId = replicaTimestamp.getDatabaseId();
            UUID targetTableId = replicaTimestamp.getTableId();

            // Prepare timestamps to send (all timestamps EXCEPT the one for this replica)
            List<Map<String, Object>> timestampsForReplication = new ArrayList<>();
            for (TupleReplicationTimestamp ts : timestamps) {
                if (!ts.getSiteUrl().equals(replicaUrl)) {
                    Map<String, Object> tsMap = Map.of(
                            "siteUrl", ts.getSiteUrl(),
                            "replicationId", ts.getReplicationId(),
                            "databaseId", ts.getDatabaseId().toString(),
                            "tableId", ts.getTableId().toString(),
                            "rowStart", ts.getRowStart() != null ? ts.getRowStart().toString() : null,
                            "rowEnd", ts.getRowEnd() != null ? ts.getRowEnd().toString() : null
                    );
                    timestampsForReplication.add(tsMap);
                }
            }

            log.info("Sending {} timestamps to replica {} using databaseId={}, tableId={}",
                    timestampsForReplication.size(), replicaUrl, targetDatabaseId, targetTableId);

            try {
                String path = replicaUrl + "/api/v1/database/" + targetDatabaseId + "/table/" + targetTableId + "/timestamps";
                Map<String, Object> request = Map.of(
                        "timestamps", timestampsForReplication
                );

                HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request);
                ResponseEntity<Map> response = externalReplicationRestTemplate.exchange(
                        path, HttpMethod.POST, httpRequest, Map.class);

                log.info("Timestamp replication to {}: {}", replicaUrl, response.getStatusCode());

            } catch (Exception e) {
                log.error("Failed to replicate timestamps to {}: {}", replicaUrl, e.getMessage());
            }
        }
    }

    /**
     * Parse microsecond timestamp string to SQL Timestamp
     */
    private Timestamp parseTimestamp(String timestampStr) {
        if (timestampStr == null) return null;

        log.info(timestampStr);

        String withoutTz = timestampStr.substring(0, timestampStr.length() - 6);
        LocalDateTime ldt = LocalDateTime.parse(withoutTz,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        log.info(String.valueOf(Timestamp.valueOf(ldt)));
        return Timestamp.valueOf(ldt);
    }
}
