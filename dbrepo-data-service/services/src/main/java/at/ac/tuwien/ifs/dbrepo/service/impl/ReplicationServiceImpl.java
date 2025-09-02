package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate replicationRestTemplate;
    private final RestTemplate externalReplicationRestTemplate;
    
    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public ReplicationServiceImpl(RestTemplate replicationRestTemplate,
                                @Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate) {
        this.replicationRestTemplate = replicationRestTemplate;
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
    }

    @Override
    public void replicateDatabase(CreateDatabaseDto createDatabaseDto) {
        try {
            // Use the BASE_URL environment variable for the current instance URL
            String currentInstanceUrl = baseUrl;
            
            // Create the notification DTO
            DatabaseNotificationDto notificationDto = DatabaseNotificationDto.builder()
                    .createDatabaseDto(createDatabaseDto)
                    .build();
            
            log.info("Sending database replication notification to replication service: {}", notificationDto);
            
            // Send POST request to replication service
            ResponseEntity<Void> response = replicationRestTemplate.exchange(
                    "/api/replication/notify",
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
    public void replicateTuple(TupleWithTimestampsDto tupleWithTimestamps, DatabaseDto database, TableDto table) {
        try {
            log.info("Sending tuple replication to replication service for {}.{}, replicationKey={}", 
                    database.getInternalName(), table.getInternalName(), 
                    tupleWithTimestamps != null ? tupleWithTimestamps.getReplicationKey() : null);

            final var request = DataReplicationDto.builder()
                    .tuple(tupleWithTimestamps)
                    .database(database)
                    .table(table)
                    .build();

            ResponseEntity<Void> response = replicationRestTemplate.exchange(
                    "/api/replication/data",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    Void.class
            );
            log.info("Tuple replication sent. Status: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send tuple replication: {}", e.getMessage(), e);
        }
    }

    @Override
    public void replicateQuery(DatabaseDto database, QueryDto query) {
        try {
            log.info("Sending query replication to replication service for database: {}, query: {}", 
                    database.getInternalName(), query.getId());

            // Check if database has replica URLs configured
            if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
                log.debug("No replica URLs configured for database: {}, skipping replication", database.getInternalName());
                return;
            }

            // Iterate over each replica URL and call the subset endpoint
            for (Map.Entry<String, UUID> replicaEntry : database.getReplicaUrls().entrySet()) {
                String replicaUrl = replicaEntry.getKey();
                UUID remoteDatabaseId = replicaEntry.getValue();
                
                try {
                    // Construct the full URL for the subset endpoint
                    String subsetEndpointUrl = replicaUrl + "/api/replication/replicate/subset?databaseId=" + remoteDatabaseId;
                    
                    log.debug("Replicating subset to replica: {} at URL: {} with remote database ID: {}", 
                            replicaUrl, subsetEndpointUrl, remoteDatabaseId);
                    
                    // Send POST request to the replica's subset endpoint
                    ResponseEntity<Map> response = externalReplicationRestTemplate.exchange(
                            subsetEndpointUrl,
                            HttpMethod.POST,
                            new HttpEntity<>(query),
                            Map.class
                    );
                    
                    log.info("Successfully replicated subset to replica: {}. Response status: {}", 
                            replicaUrl, response.getStatusCode());
                    
                } catch (Exception e) {
                    log.warn("Failed to replicate subset to replica: {}. Error: {}", replicaUrl, e.getMessage(), e);
                    // Continue with other replicas even if one fails
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to replicate query: {}", e.getMessage(), e);
        }
    }

}
