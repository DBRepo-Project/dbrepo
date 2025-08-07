package at.ac.tuwien.ifs.dbrepo.service.impl;


import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
public class DatabaseServiceMariaDbImpl extends DataConnector implements DatabaseService {


    private final ReplicationService replicationService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public DatabaseServiceMariaDbImpl(DataMapper dataMapper, MariaDbMapper mariaDbMapper, 
                                     ReplicationService replicationService, MetadataServiceGateway metadataServiceGateway) {

        this.replicationService = replicationService;
        this.metadataServiceGateway = metadataServiceGateway;
    }



    @Override
    public void handleDatabaseReplication(DatabaseNotificationDto databaseNotificationDto) {
        log.info("=== DATABASE REPLICATION HANDLER ===");
        
        // Print replica URLs
        System.out.println("Replica URLs:");
        System.out.println("Database Name: " + databaseNotificationDto.getCreateDatabaseDto().getName());
        System.out.println("Creation Location: " + databaseNotificationDto.getCreateDatabaseDto().getCreationLocation());
        System.out.println("Database ID: " + databaseNotificationDto.getCreationId());
        
        // Get replica URLs from the database notification
        var replicaUrls = databaseNotificationDto.getCreateDatabaseDto().getReplicaUrls();
        
        if (replicaUrls != null && !replicaUrls.isEmpty()) {
            log.info("Sending replication to {} instances", replicaUrls.size());
            System.out.println("Replica URLs to contact: " + replicaUrls);
            
            // Send replication to other instances
            replicationService.sendDatabaseReplicationToInstances(databaseNotificationDto, replicaUrls);
        } else {
            log.info("No replica URLs provided, skipping replication to other instances");
            System.out.println("No replica URLs to contact");
        }
        
        System.out.println("========================");
    }

    @Override
    public Map<String, Object> insertReplicatedDatabase(DatabaseNotificationDto databaseNotificationDto) {
        log.info("Creating database locally from replication notification");
        
        try {
            // Call the metadata service to create the database
            final String path = "/api/v1/database/replicate";
            final Map<String, Object> response = metadataServiceGateway.createReplicatedDatabase(path, databaseNotificationDto);
            
            log.info("Database created successfully with ID: {}", response.get("databaseId"));
            return response;
            
        } catch (Exception e) {
            log.error("Failed to create database from replication: {}", e.getMessage());
            return Map.of(
                "status", "error",
                "message", "Database creation failed: " + e.getMessage()
            );
        }
    }
}
