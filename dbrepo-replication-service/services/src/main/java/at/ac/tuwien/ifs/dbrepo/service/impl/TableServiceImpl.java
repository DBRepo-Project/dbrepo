package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
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
}
