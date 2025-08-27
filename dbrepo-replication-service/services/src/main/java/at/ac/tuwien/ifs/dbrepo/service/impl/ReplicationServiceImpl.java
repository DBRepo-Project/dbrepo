package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaTableLocation;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationTimestampService;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;

import java.util.List;
import java.util.UUID;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate externalReplicationRestTemplate;
    private final GatewayConfig gatewayConfig;
    private final ReplicationTimestampService replicationTimestampService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public ReplicationServiceImpl(@Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate,
                                GatewayConfig gatewayConfig,
                                ReplicationTimestampService replicationTimestampService,
                                MetadataServiceGateway metadataServiceGateway) {
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
        this.gatewayConfig = gatewayConfig;
        this.replicationTimestampService = replicationTimestampService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @Override
    public void sendDatabaseReplicationToInstances(DatabaseNotificationDto databaseNotificationDto, List<String> replicaUrls) {
        log.info("Sending database replication to {} instances", replicaUrls.size());
        
        Map<String, String> replicaUrlToDatabaseIdMap = new HashMap<>();
        
        for (String replicaUrl : replicaUrls) {
            try {
                String databaseId = sendDatabaseReplicationToInstance(databaseNotificationDto, replicaUrl);
                if (databaseId != null) {
                    replicaUrlToDatabaseIdMap.put(replicaUrl, databaseId);
                }
            } catch (Exception e) {
                log.error("Failed to send replication to instance {}: {}", replicaUrl, e.getMessage());
            }
        }
        
        // Now you have the map with replica URLs and their corresponding database IDs
        // You can work with this map here or pass it to another method
        log.info("Collected {} successful database replications", replicaUrlToDatabaseIdMap.size());
        
        // Now call the external endpoint for each replica to update their replication database IDs
        for (String replicaUrl : replicaUrls) {
            try {
                // Send the complete map to each replica
                // Each replica will receive information about all other replicas
                callExternalUpdateReplicationDatabaseIds(replicaUrl, replicaUrlToDatabaseIdMap);
            } catch (Exception e) {
                log.error("Failed to call external update replication database IDs for {}: {}", replicaUrl, e.getMessage());
            }
        }
        
        // Example: You could call another method to process the results
        // processReplicationResults(replicaUrlToDatabaseIdMap);
    }

    @Override
    public String sendDatabaseReplicationToInstance(DatabaseNotificationDto databaseNotificationDto, String replicaUrl) {
        log.info("Sending database replication to instance: {}", replicaUrl);
        
        try {
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<DatabaseNotificationDto> requestEntity = new HttpEntity<>(databaseNotificationDto, headers);
            
            // Build the full URL for the replication endpoint
            String replicationUrl = replicaUrl + "/api/replication/replicate/database";
            
            log.info("Sending POST request to: {}", replicationUrl);
            
            // Send the request
            ResponseEntity<String> response = externalReplicationRestTemplate.postForEntity(replicationUrl, requestEntity, String.class);
            
            log.info("Replication sent successfully to {} with status: {}", replicaUrl, response.getStatusCode());
            log.info("Response details - Status code: {}, Headers: {}, Body: {}", 
                response.getStatusCode(),
                response.getHeaders(),
                response.getBody());
            
            // Parse the response to extract the remote database ID
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    
                    if (responseJson.has("databaseId")) {
                        String remoteDatabaseId = responseJson.get("databaseId").asText();
                        log.info("Extracted remote database ID: {} from response", remoteDatabaseId);
                        
                        // Call the new endpoint to update the replication URL with the remote database ID
                        updateReplicationUrlWithRemoteId(databaseNotificationDto.getCreationId(), 
                                                       replicaUrl, UUID.fromString(remoteDatabaseId));
                        
                        return remoteDatabaseId;
                    } else {
                        log.warn("Response does not contain databaseId field: {}", response.getBody());
                        return null;
                    }
                } catch (Exception e) {
                    log.error("Failed to parse replication response: {}", e.getMessage());
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to send replication to {}: {}", replicaUrl, e.getMessage());
            throw new RuntimeException("Failed to send replication to " + replicaUrl, e);
        }
    }

    @Override
    public void sendTableReplicationToInstances(TableNotificationDto tableNotificationDto) {
        log.info("Sending table replication to {} instances", tableNotificationDto.getReplicas().size());
        
        Map<String, String> replicaUrlToTableIdMap = new HashMap<>();
        
        for (ReplicaLocation replica : tableNotificationDto.getReplicas()) {
            try {
                String tableId = sendTableReplicationToInstance(replica.getReplicaDatabaseId(), tableNotificationDto, replica.getUrl());
                if (tableId != null) {
                    replicaUrlToTableIdMap.put(replica.getUrl(), tableId);
                }
            } catch (Exception e) {
                log.error("Failed to send table replication to instance {}: {}", replica.getUrl(), e.getMessage());
            }
        }
        
        // Now you have the map with replica URLs and their corresponding table IDs
        // You can work with this map here or pass it to another method
        log.info("Collected {} successful table replications", replicaUrlToTableIdMap.size());
        
        // Now call the external endpoint for each replica to update their replication table IDs
        for (ReplicaLocation replica : tableNotificationDto.getReplicas()) {
            try {
                // Send the complete map to each replica
                // Each replica will receive information about all other replicas
                callExternalUpdateReplicationTableIds(replica.getUrl(), replicaUrlToTableIdMap, 
                    replica.getReplicaDatabaseId());
            } catch (Exception e) {
                log.error("Failed to call external update replication table IDs for {}: {}", replica.getUrl(), e.getMessage());
            }
        }
        

    }

    @Override
    public String sendTableReplicationToInstance(UUID remoteDatabaseId, TableNotificationDto tableNotificationDto, String replicaUrl) {
        log.info("Sending table replication to instance: {} with creationId: {}", replicaUrl, tableNotificationDto.getCreationId());
        
        try {
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<TableNotificationDto> requestEntity = new HttpEntity<>(tableNotificationDto, headers);
            
            // Build the full URL for the table replication endpoint
            //TODO: maybe call other replication service first
            String replicationUrl = replicaUrl + "/api/v1/database/" + remoteDatabaseId + "/table/replicate";
            
            log.info("Sending POST request to: {}", replicationUrl);
            
            // Send the request
            ResponseEntity<String> response = externalReplicationRestTemplate.postForEntity(replicationUrl, requestEntity, String.class);
            
            log.info("Table replication sent successfully to {} with status: {}", replicaUrl, response.getStatusCode());
            log.info("Response details - Status code: {}, Headers: {}, Body: {}", 
                response.getStatusCode(),
                response.getHeaders(),
                response.getBody());
            
            // Parse the response to extract the remote table ID if available
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    
                    if (responseJson.has("id")) {
                        String remoteTableId = responseJson.get("id").asText();
                        log.info("Extracted remote table ID: {} from response", remoteTableId);

                        // Call the new endpoint to update the replication URL with the remote table ID
                        // Use creationId as the local table ID and remoteTableId as the remote table ID
                        updateTableReplicationUrlWithRemoteId(tableNotificationDto.getDatabaseId(), tableNotificationDto.getCreationId(),
                                replicaUrl, UUID.fromString(remoteTableId));
                        
                        return remoteTableId;
                    } else {
                        log.warn("Response does not contain id field: {}", response.getBody());
                        return null;
                    }
                } catch (Exception e) {
                    log.error("Failed to parse table replication response: {}", e.getMessage());
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to send table replication to {}: {}", replicaUrl, e.getMessage());
            throw new RuntimeException("Failed to send table replication to " + replicaUrl, e);
        }
    }
    
    @Override
    public void updateReplicationUrlWithRemoteId(UUID databaseId, String replicaUrl, UUID remoteDatabaseId) {
        try {
            log.info("Updating replication URL {} with remote database ID {} for database {}", 
                    replicaUrl, remoteDatabaseId, databaseId);
            
            // Create the DTO for updating replication URL
            DatabaseUpdateReplicationUrlDto updateDto = DatabaseUpdateReplicationUrlDto.builder()
                    .replicaUrl(replicaUrl)
                    .replicaDatabaseId(remoteDatabaseId)
                    .build();
            
            // Create headers for the update request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<DatabaseUpdateReplicationUrlDto> requestEntity = new HttpEntity<>(updateDto, headers);
            
            // Build the URL for the replication URL update endpoint
            String updateUrl = gatewayConfig.getMetadataEndpoint() + "/api/v1/database/" + databaseId + "/replication-url";
            
            log.info("Sending PUT request to update replication URL: {}", updateUrl);
            
            // Send the request
            ResponseEntity<String> response = externalReplicationRestTemplate.exchange(
                updateUrl, 
                org.springframework.http.HttpMethod.PUT, 
                requestEntity, 
                String.class
            );
            
            log.info("Replication URL updated successfully with status: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to update replication URL for database {}: {}", databaseId, e.getMessage());
        }
    }

    @Override
    public void updateTableReplicationUrlWithRemoteId(UUID databaseId, UUID localTableId, String replicaUrl, UUID remoteTableId) {
        try {
            log.info("Updating table replication URL {} with remote table ID {} for database {} and local table ID {}", 
                    replicaUrl, remoteTableId, databaseId, localTableId);
            
            // Create the DTO for updating replication URL
            TableUpdateReplicationUrlDto updateDto = TableUpdateReplicationUrlDto.builder()
                    .replicaUrl(replicaUrl)
                    .replicaTableId(remoteTableId)
                    .build(); 
            
            // Create headers for the update request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<TableUpdateReplicationUrlDto> requestEntity = new HttpEntity<>(updateDto, headers);
            
            // Build the URL for the replication URL update endpoint
            String updateUrl = gatewayConfig.getMetadataEndpoint() + "/api/v1/database/" + databaseId + "/table/" + localTableId + "/replication-url";
            
            log.info("Sending PUT request to update table replication URL: {}", updateUrl);
            
            // Send the request
            ResponseEntity<String> response = externalReplicationRestTemplate.exchange(
                updateUrl, 
                org.springframework.http.HttpMethod.PUT, 
                requestEntity, 
                String.class
            );
            
            log.info("Table replication URL updated successfully with status: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to update table replication URL for database {} and table {}: {}", databaseId, localTableId, e.getMessage());
        }
    }
    
    private void callExternalUpdateReplicationDatabaseIds(String replicaUrl, Map<String, String> replicaUrlToDatabaseIdMap) {
        try {
            log.info("Calling external endpoint to update replication database IDs for replica: {}", replicaUrl);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(replicaUrlToDatabaseIdMap, headers);
            
            // Build the full URL for the update replication database IDs endpoint
            String updateUrl = replicaUrl + "/api/replication/replicate/update-replication-database-ids";
            
            log.info("Sending POST request to: {}", updateUrl);
            
            // Send the request
            ResponseEntity<String> response = externalReplicationRestTemplate.postForEntity(updateUrl, requestEntity, String.class);
            
            log.info("External update replication database IDs call successful to {} with status: {}", replicaUrl, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Failed to call external update replication database IDs for {}: {}", replicaUrl, e.getMessage());
            throw new RuntimeException("Failed to call external update replication database IDs for " + replicaUrl, e);
        }
    }

    private void callExternalUpdateReplicationTableIds(String replicaUrl, Map<String, String> replicaUrlToTableIdMap, UUID databaseId) {
        try {
            log.info("Calling external endpoint to update replication table IDs for replica: {} with database {} ", replicaUrl, databaseId);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(replicaUrlToTableIdMap, headers);
            
            // Build the full URL for the update replication table IDs endpoint with query parameters
            String updateUrl = replicaUrl + "/api/replication/replicate/update-replication-table-ids?databaseId=" + databaseId;
            
            log.info("Sending POST request to: {}", updateUrl);
            
            // Send the request
            ResponseEntity<String> response = externalReplicationRestTemplate.postForEntity(updateUrl, requestEntity, String.class);
            
            log.info("External update replication table IDs call successful to {} with status: {}", replicaUrl, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Failed to call external update replication table IDs for {}: {}", replicaUrl, e.getMessage());
            throw new RuntimeException("Failed to call external update replication table IDs for " + replicaUrl, e);
        }
    }

    /**
     * Method that gets called every time the replication service is started.
     * This method is automatically invoked by Spring Boot when the application context is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Override
    public void onApplicationStartup() {
        log.info("Replication service is starting up - performing startup tasks...");
        
        try {
            // Add your startup logic here
            performStartupTasks();
            
            log.info("Replication service startup tasks completed successfully");
        } catch (Exception e) {
            log.error("Error during replication service startup: {}", e.getMessage(), e);
            // You can choose to throw the exception to prevent the application from starting
            // or handle it gracefully depending on your requirements
        }
    }

    /**
     * Performs the actual startup tasks for the replication service.
     * Override this method or add your specific startup logic here.
     */
    private void performStartupTasks() {
        log.info("Performing replication service startup tasks...");
        
        // 1. Get the latest replication timestamp to determine when the service received updates last
        getLatestReplicationTimestamp();
        
        // 2. Initialize replication state
        log.info("Initializing replication state...");
        
        // 3. Check connectivity to replica instances
        log.info("Checking replica instance connectivity...");
        
        // 4. Validate configuration
        log.info("Validating replication configuration...");
        
        // 5. Initialize any required resources
        log.info("Initializing replication resources...");
        
        // 6. Start any background processes if needed
        log.info("Starting background replication processes...");
        
        // Add your specific startup logic here
        // For example:
        // - Initialize connection pools
        // - Load configuration from external sources
        // - Establish connections to replica databases
        // - Start monitoring services
        // - Initialize caches
        // - etc.
    }

    /**
     * Gets the latest replication timestamp from all databases by querying the metadata service.
     * This helps determine when the service received updates for the last time.
     */
    private void getLatestReplicationTimestamp() {
        try {
            log.info("Retrieving all databases from metadata service to check replication timestamps...");
            
            // Get all databases from the metadata service (brief information)
            List<DatabaseBriefDto> allDatabaseBriefs = metadataServiceGateway.getAllDatabases();
            log.info("Found {} databases in metadata service", allDatabaseBriefs.size());
            
            java.time.Instant overallLatestTimestamp = null;
            String databaseNameWithLatestTimestamp = null;
            
            // Check each database for replication timestamps
            for (DatabaseBriefDto databaseBrief : allDatabaseBriefs) {
                try {
                    log.debug("Checking replication timestamps for database: {} ({})", 
                            databaseBrief.getInternalName(), databaseBrief.getId());
                    
                    // Get the full database details to access container information
                    DatabaseDto fullDatabase = metadataServiceGateway.getDatabaseById(databaseBrief.getId());
                    
                    java.time.Instant latestTimestamp = replicationTimestampService.getLatestReplicationTimestamp(fullDatabase);
                    
                    if (latestTimestamp != null) {
                        log.debug("Database {} has latest replication timestamp: {}", 
                                databaseBrief.getInternalName(), latestTimestamp);
                        
                        // Track the overall latest timestamp across all databases
                        if (overallLatestTimestamp == null || latestTimestamp.isAfter(overallLatestTimestamp)) {
                            overallLatestTimestamp = latestTimestamp;
                            databaseNameWithLatestTimestamp = databaseBrief.getInternalName();
                        }
                    } else {
                        log.debug("Database {} has no replication timestamps", databaseBrief.getInternalName());
                    }
                    
                } catch (Exception e) {
                    log.warn("Could not check replication timestamps for database {}: {}", 
                            databaseBrief.getInternalName(), e.getMessage());
                    log.debug("Database timestamp check failed for {}", databaseBrief.getInternalName(), e);
                }
            }
            
            // Log the overall result
            if (overallLatestTimestamp != null) {
                log.info("Overall latest replication timestamp: {} (from database: {})", 
                        overallLatestTimestamp, databaseNameWithLatestTimestamp);
                log.info("Service last received updates at: {}", overallLatestTimestamp);
            } else {
                log.info("No replication timestamps found in any database - this appears to be the first startup");
            }
            
        } catch (RemoteUnavailableException e) {
            log.error("Metadata service is not available: {}", e.getMessage());
        } catch (MetadataServiceException e) {
            log.error("Error retrieving databases from metadata service: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Could not retrieve latest replication timestamp: {}", e.getMessage());
            log.debug("Timestamp retrieval failed", e);
        }
    }
} 