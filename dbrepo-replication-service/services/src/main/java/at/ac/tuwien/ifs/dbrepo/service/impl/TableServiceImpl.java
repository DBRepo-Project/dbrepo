package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    private final ReplicationService replicationService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public TableServiceImpl(ReplicationService replicationService, MetadataServiceGateway metadataServiceGateway) {
        this.replicationService = replicationService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @Override
    public void handleTableReplication(List<ReplicaLocation> replicas, CreateTableDto createTableDto) {
        log.info("=== TABLE REPLICATION HANDLER ===");
        
        if (replicas != null && !replicas.isEmpty()) {
            log.info("Sending table replication to {} instances", replicas.size());
            
            // Send replication to each replica individually
            for (ReplicaLocation replica : replicas) {
                try {
                    String replicaUrl = replica.getUrl();
                    UUID databaseId = replica.getReplicaDatabaseId();
                    
                    log.info("Sending table replication to replica: {} with database ID: {}", replicaUrl, databaseId);
                    
                    // Send replication to this specific instance
                    replicationService.sendTableReplicationToInstance(databaseId, createTableDto, replicaUrl);
                    
                } catch (Exception e) {
                    log.error("Failed to send table replication to replica {}: {}", replica.getUrl(), e.getMessage());
                }
            }
        } else {
            log.info("No replica URLs provided, skipping table replication to other instances");
        }
        
        System.out.println("========================");
    }

    @Override
    public Map<String, Object> insertReplicatedTable(UUID databaseId, CreateTableDto createTableDto) {
        log.info("Creating table locally from replication notification");
        
        try {
            // Call the metadata service to create the table
            final String path = "/api/v1/database/" + databaseId + "/table";
            final Map<String, Object> response = metadataServiceGateway.createReplicatedTable(path, databaseId, createTableDto);
            
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
}
