package at.ac.tuwien.ifs.dbrepo.service.impl;


import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate replicationRestTemplate;
    private final DatabaseService databaseService;

    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public ReplicationServiceImpl(@Qualifier("replicationRestTemplate") RestTemplate replicationRestTemplate,
                                @Lazy DatabaseService databaseService) {
        this.replicationRestTemplate = replicationRestTemplate;
        this.databaseService = databaseService;
    }

    @Override
    @Async
    public void replicateDatabase(CreateDatabaseDto createDatabaseDto, UUID creationId) {
        try {
            // Add a small delay to ensure the transaction is fully committed
            Thread.sleep(1000);
            
            // Use the BASE_URL environment variable for the current instance URL
            String currentInstanceUrl = baseUrl;

            createDatabaseDto.setCreationLocation(currentInstanceUrl);

            // Create the notification DTO
            DatabaseNotificationDto notificationDto = DatabaseNotificationDto.builder()
                    .createDatabaseDto(createDatabaseDto)
                    .creationId(creationId)
                    .build();

            log.info("Sending database replication notification to replication service: {} for database: {}", notificationDto, creationId);

            // Send POST request to replication service
            ResponseEntity<Void> response = replicationRestTemplate.exchange(
                    "api/replication/database",
                    HttpMethod.POST,
                    new HttpEntity<>(notificationDto),
                    Void.class
            );

            log.info("Database replication notification sent successfully. Response status: {}", response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to send database replication notification: {}", e.getMessage(), e);
            // You might want to throw a custom exception here depending on your error handling strategy
        }
    }

    @Override
    @Async
    public void replicateTable(CreateTableDto createTableDto, UUID databaseId) {
        try {
            // Add a small delay to ensure the transaction is fully committed
            Thread.sleep(1000);
            
            log.info("Sending table replication notification to replication service for database: {} and table: {}", databaseId, createTableDto.getName());

            // Get the database to access replicas
            List<ReplicaLocation> replicas = List.of();
            try {
                var database = databaseService.findById(databaseId);
                replicas = database.getReplicaUrls();
                log.debug("Found {} replicas for database {}", replicas.size(), databaseId);
            } catch (Exception e) {
                log.warn("Failed to get replicas for database {}: {}", databaseId, e.getMessage());
            }

            // Create the notification DTO
            TableNotificationDto notificationDto = TableNotificationDto.builder()
                    .databaseId(databaseId)
                    .createTableDto(createTableDto)
                    .replicas(replicas)
                    .build();

            // Send POST request to replication service
            ResponseEntity<Void> response = replicationRestTemplate.exchange(
                    "api/replication/table",
                    HttpMethod.POST,
                    new HttpEntity<>(notificationDto),
                    Void.class
            );

            log.info("Table replication notification sent successfully. Response status: {}", response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to send table replication notification: {}", e.getMessage(), e);
            // You might want to throw a custom exception here depending on your error handling strategy
        }
    }
    

}
