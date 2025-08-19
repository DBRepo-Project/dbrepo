package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import org.springframework.beans.factory.annotation.Qualifier;
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
import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;
import at.ac.tuwien.ifs.dbrepo.core.repository.TupleReplicationTimestampRepository;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    private final ReplicationService replicationService;
    private final RestTemplate externalReplicationRestTemplate;
    private final MetadataServiceGateway metadataServiceGateway;
    private final TupleReplicationTimestampRepository tupleReplicationTimestampRepository;

    @Autowired
    public TableServiceImpl(ReplicationService replicationService, MetadataServiceGateway metadataServiceGateway,
                            @Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate,
                            TupleReplicationTimestampRepository tupleReplicationTimestampRepository) {
        this.replicationService = replicationService;
        this.metadataServiceGateway = metadataServiceGateway;
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
        this.tupleReplicationTimestampRepository = tupleReplicationTimestampRepository;
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
                final Object replicationId = response.getBody().get("replication_id");
                log.info("Remote timestamps from {}: inserted_at={}, deleted_at={}", replicaUrl, tsInserted, tsDeleted);
                log.debug("Full remote response body: {}", response.getBody());

                
                // Create timestamp record for successful replication
                TupleReplicationTimestamp timestamp = TupleReplicationTimestamp.builder()
                    .siteUrl(replicaUrl)
                    .replicationId((String) replicationId)
                    .databaseId(remoteDatabaseId)
                    .tableId(remoteTableId)
                    .rowStart(Timestamp.valueOf((String) tsInserted))
                    .rowEnd(Timestamp.valueOf((String) tsDeleted)) // Replication completed
                    .build();
                
                timestampsToSave.add(timestamp);
                log.debug("Added timestamp for successful replication to {}: {}", replicaUrl, timestamp.getReplicationId());
                
            } catch (Exception e) {
                log.error("Failed to replicate data to {}: {}", replicaUrl, e.getMessage());

            }
        }
        
        // After the loop, save all timestamps to the database
        if (!timestampsToSave.isEmpty()) {
            try {
                tupleReplicationTimestampRepository.saveAll(timestampsToSave);
                log.info("Saved {} replication timestamps to database", timestampsToSave.size());
            } catch (Exception e) {
                log.error("Failed to save replication timestamps: {}", e.getMessage(), e);
            }
        }
    }
}
