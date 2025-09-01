package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.LocalTableIdDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.ReplicationSynchronisationDataDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
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
import at.ac.tuwien.ifs.dbrepo.auth.InternalRequestInterceptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;


@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate externalReplicationRestTemplate;
    private final RestTemplate localDataServiceRestTemplate;
    private final GatewayConfig gatewayConfig;
    private final ReplicationTimestampService replicationTimestampService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public ReplicationServiceImpl(@Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate,
                                @Qualifier("dataRestTemplate") RestTemplate localDataServiceRestTemplate,
                                GatewayConfig gatewayConfig,
                                ReplicationTimestampService replicationTimestampService,
                                MetadataServiceGateway metadataServiceGateway) {
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
        this.localDataServiceRestTemplate = localDataServiceRestTemplate;
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
        log.info("🚀 Replication service is starting up - performing startup tasks...");
        
        try {
            // Perform startup tasks and get replication data if available
            ReplicationSynchronisationDataDto replicationData = performStartupTasks();
            
            if (replicationData != null) {
                log.info("🎯 Replication service found new data during startup!");
                log.info("📊 Summary: {} tuples, {} replication timestamps", 
                        replicationData.getTuples() != null ? replicationData.getTuples().size() : 0,
                        replicationData.getReplicationTimestamps() != null ? replicationData.getReplicationTimestamps().size() : 0);
            } else {
                log.info("ℹ️ No new replication data found during startup");
            }
            
            log.info("✅ Replication service startup tasks completed successfully");
        } catch (Exception e) {
            log.error("❌ Error during replication service startup: {}", e.getMessage(), e);
            // You can choose to throw the exception to prevent the application from starting
            // or handle it gracefully depending on your requirements
        }
    }

    /**
     * Performs the actual startup tasks for the replication service.
     * This is the central orchestrator with all logic in one place - no nested method calls!
     * Returns ReplicationSynchronisationDataDto if new data is found, null otherwise.
     */
    private ReplicationSynchronisationDataDto performStartupTasks() {
        log.info("=== STARTING REPLICATION SERVICE STARTUP TASKS ===");
        
        try {
            // 1. Get all databases from metadata service
            log.info("Step 1: Retrieving databases from metadata service...");
            List<DatabaseBriefDto> allDatabases;
            try {
                allDatabases = metadataServiceGateway.getAllDatabases();
                log.info("Found {} databases in metadata service", allDatabases.size());
            } catch (RemoteUnavailableException e) {
                log.error("Metadata service is not available: {}", e.getMessage());
                return null;
            } catch (MetadataServiceException e) {
                log.error("Error retrieving databases from metadata service: {}", e.getMessage());
                return null;
            } catch (Exception e) {
                log.warn("Could not retrieve databases from metadata service: {}", e.getMessage());
                log.debug("Database retrieval failed", e);
                return null;
            }
            
            if (allDatabases.isEmpty()) {
                log.warn("No databases found in metadata service");
                return null;
            }
            
            log.info("✅ Retrieved {} databases from metadata service", allDatabases.size());
            
            // 2. Process each database sequentially
            log.info("Step 2: Processing each database for replication timestamps...");
            for (DatabaseBriefDto databaseBrief : allDatabases) {
                try {
                    log.info("=== PROCESSING DATABASE: {} ===", databaseBrief.getInternalName());
                    
                    // Get full database details
                    DatabaseDto fullDatabase = metadataServiceGateway.getDatabaseById(databaseBrief.getId());
                    
                    // Log replica URLs if available
                    if (fullDatabase.getReplicaUrls() != null && !fullDatabase.getReplicaUrls().isEmpty()) {
                        log.info("  - Replica URLs:");
                        fullDatabase.getReplicaUrls().forEach((url, id) -> 
                            log.info("    * {} -> Database ID: {}", url, id));
                    } else {
                        log.info("  - Replica URLs: None configured");
                    }
                    
                    // Get latest replication timestamp for this database
                    Instant latestTimestamp = replicationTimestampService.getLatestReplicationTimestamp(fullDatabase);
                    
                    if (latestTimestamp != null) {
                        log.info("✅ Database {} has latest replication timestamp: {}", 
                                databaseBrief.getInternalName(), latestTimestamp);
                        
                        // Check for new tuples after this timestamp
                        ReplicationSynchronisationDataDto replicationData = checkForNewTuplesAfterTimestamp(fullDatabase, latestTimestamp, databaseBrief);
                        
                        if (replicationData != null) {
                            log.info("🔄 Found replication data for database {}: {} tuples, {} replication timestamps", 
                                    databaseBrief.getInternalName(), 
                                    replicationData.getTuples() != null ? replicationData.getTuples().size() : 0,
                                    replicationData.getReplicationTimestamps() != null ? replicationData.getReplicationTimestamps().size() : 0);
                            
                            // Add replication timestamps to local data service
                            log.info("Timestmap format returned from remote data service:");
                            log.info(String.valueOf(replicationData.getReplicationTimestamps().get(0).getRowStart()));
                            log.info("📥 Adding replication timestamps to local data service...");
                            addReplicationTimestampsToLocalDataService(replicationData, fullDatabase);
                            
                            // Return the replication data to the main method
                            return replicationData;
                        }
                    } else {
                        log.info("ℹ️ Database {} has no replication timestamps", databaseBrief.getInternalName());
                    }
                    
                    log.info("=== END PROCESSING DATABASE: {} ===\n", databaseBrief.getInternalName());
                    
                } catch (Exception e) {
                    log.error("❌ Could not process database {}: {}", 
                            databaseBrief.getInternalName(), e.getMessage());
                    log.debug("Database processing failed for {}", databaseBrief.getInternalName(), e);
                }
            }
            
            log.info("=== REPLICATION SERVICE STARTUP TASKS COMPLETED SUCCESSFULLY ===");
            log.info("ℹ️ No new replication data found in any database");
            return null;
            
        } catch (Exception e) {
            log.error("❌ Error during startup tasks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to complete startup tasks", e);
        }
    }


    /**
     * Checks for new tuples inserted after a given timestamp by calling the data service
     * at the creation location of the database.
     * Returns ReplicationSynchronisationDataDto if new data is found, null otherwise.
     */
    private ReplicationSynchronisationDataDto checkForNewTuplesAfterTimestamp(DatabaseDto database, java.time.Instant timestamp, DatabaseBriefDto databaseBrief) {
        log.info("=== CHECKING FOR NEW TUPLES AFTER TIMESTAMP ===");
        log.info("Database: {} ({})", database.getName(), database.getInternalName());
        log.info("Timestamp: {}", timestamp);
        log.info("Creation Location: {}", database.getCreationLocation());
        
        try {
            // Check if creation location is available
            if (database.getCreationLocation() == null || database.getCreationLocation().trim().isEmpty()) {
                log.warn("⚠️ No creation location available for database: {}", database.getInternalName());
                return null;
            }
            
            // Check if replica URLs are available
            if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
                log.info("ℹ️ No replica URLs configured for database: {}", database.getInternalName());
                return null;
            }
            
            log.info("📊 Found {} replica URLs to check", database.getReplicaUrls().size());
            
            // Find the replica URL that matches the creation location
            String matchingReplicaUrl = null;
            UUID matchingReplicaDatabaseId = null;
            
            for (Map.Entry<String, UUID> replicaEntry : database.getReplicaUrls().entrySet()) {
                String replicaUrl = replicaEntry.getKey();
                UUID replicaDatabaseId = replicaEntry.getValue();
                
                if (replicaUrl.equals(database.getCreationLocation())) {
                    matchingReplicaUrl = replicaUrl;
                    matchingReplicaDatabaseId = replicaDatabaseId;
                    log.info("✅ Found matching replica URL: {} -> Database ID: {}", replicaUrl, replicaDatabaseId);
                    break;
                }
            }
            
            if (matchingReplicaUrl != null) {
                log.info("🔍 Calling data service at creation location: {}", matchingReplicaUrl);
                ReplicationSynchronisationDataDto replicationData = callDataServiceForNewTuples(database, timestamp, databaseBrief, matchingReplicaUrl, matchingReplicaDatabaseId);
                
                if (replicationData != null) {
                    log.info("✅ Successfully received replication synchronisation data from remote service");
                    log.info("📊 Summary: {} tuples, {} replication timestamps", 
                        replicationData.getTuples() != null ? replicationData.getTuples().size() : 0,
                        replicationData.getReplicationTimestamps() != null ? replicationData.getReplicationTimestamps().size() : 0);
                    
                    log.info("=== END CHECKING FOR NEW TUPLES ===");
                    return replicationData;
                } else {
                    log.warn("⚠️ No replication data received from remote service");
                }
            } else {
                log.info("ℹ️ No replica URL matches creation location: {}", database.getCreationLocation());
            }
            
            log.info("=== END CHECKING FOR NEW TUPLES ===");
            return null;
            
        } catch (Exception e) {
            log.error("❌ Error in checkForNewTuplesAfterTimestamp: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Makes an external call to the data service to check for new tuples after a timestamp.
     * Returns the ReplicationSynchronisationDataDto from the remote service.
     */
    private ReplicationSynchronisationDataDto callDataServiceForNewTuples(DatabaseDto database, java.time.Instant timestamp, DatabaseBriefDto databaseBrief, String replicaUrl, UUID replicaDatabaseId) {
        log.info("=== CALLING DATA SERVICE FOR NEW TUPLES ===");
        log.info("Database: {} ({})", database.getName(), database.getInternalName());
        log.info("Database ID: {}", database.getId());
        log.info("Timestamp: {}", timestamp);
        log.info("Replica URL: {}", replicaUrl);
        log.info("Replica Database ID: {}", replicaDatabaseId);
        
        try {
            // Build the request payload
            Map<String, Object> requestPayload = Map.of(
                "timestamp", timestamp.toString(),
                "replicaDatabaseId", replicaDatabaseId.toString()
            );
            
            log.info("Request payload: {}", requestPayload);
            
            // Build the full URL using replica URL as base
            String baseUrl = replicaUrl;
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            
            // Since we're checking at database level, we need a different endpoint
            // We'll use a database-level endpoint instead of table-level
            String endpoint = String.format("/api/v1/database/%s/check-tuples-after-timestamp", replicaDatabaseId.toString());
            
            String fullUrl = baseUrl + endpoint;
            log.info("Full URL: {}", fullUrl);
            
            // Make the external HTTP call
            log.info("🌐 Making external HTTP call to data service...");
            
            // Use the existing externalReplicationRestTemplate that's already configured
            log.info("📡 Sending POST request to: {}", fullUrl);
            
            try {
                ResponseEntity<ReplicationSynchronisationDataDto> response = externalReplicationRestTemplate.postForEntity(
                    fullUrl, 
                    requestPayload, 
                    ReplicationSynchronisationDataDto.class
                );
                
                log.info("✅ HTTP call successful! Status: {}", response.getStatusCode());
                
                // Process the response
                if (response.getBody() != null) {
                    ReplicationSynchronisationDataDto replicationData = response.getBody();
                    log.info("=== REPLICATION SYNCHRONISATION DATA RECEIVED ===");
                    log.info("Tuples count: {}", replicationData.getTuples() != null ? replicationData.getTuples().size() : 0);
                    log.info("Replication timestamps count: {}", replicationData.getReplicationTimestamps() != null ? replicationData.getReplicationTimestamps().size() : 0);
                    
                    // Log detailed tuple information
                    if (replicationData.getTuples() != null && !replicationData.getTuples().isEmpty()) {
                        log.info("=== TUPLES DETAILS ===");
                        for (int i = 0; i < replicationData.getTuples().size(); i++) {
                            var tuple = replicationData.getTuples().get(i);
                            log.info("Tuple {}: replicationKey={}, insertedAt={}, deletedAt={}, dataSize={}", 
                                i + 1, 
                                tuple.getReplicationKey(),
                                tuple.getInsertedAt(),
                                tuple.getDeletedAt(),
                                tuple.getData() != null ? tuple.getData().size() : 0);
                        }
                    }
                    
                    // Log detailed replication timestamp information
                    if (replicationData.getReplicationTimestamps() != null && !replicationData.getReplicationTimestamps().isEmpty()) {
                        log.info("=== REPLICATION TIMESTAMPS DETAILS ===");
                        for (int i = 0; i < replicationData.getReplicationTimestamps().size(); i++) {
                            var tmpTimestamp = replicationData.getReplicationTimestamps().get(i);
                            log.info("Timestamp {}: siteUrl={}, replicationId={}, databaseId={}, tableId={}, rowStart={}, rowEnd={}", 
                                i + 1,
                                    tmpTimestamp.getSiteUrl(),
                                    tmpTimestamp.getReplicationId(),
                                    tmpTimestamp.getDatabaseId(),
                                    tmpTimestamp.getTableId(),
                                    tmpTimestamp.getRowStart(),
                                    tmpTimestamp.getRowEnd());
                        }
                    }
                    
                    log.info("=== END REPLICATION DATA ANALYSIS ===");
                    
                    return replicationData;
                } else {
                    log.warn("⚠️ Response body is null");
                    return null;
                }
                
            } catch (Exception httpException) {
                log.error("❌ HTTP call failed: {}", httpException.getMessage(), httpException);
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ Error calling data service: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Adds replication timestamps to the local data service.
     * This method processes the ReplicationSynchronisationDataDto and inserts
     * replication timestamps into the appropriate local database tables.
     */
    private void addReplicationTimestampsToLocalDataService(ReplicationSynchronisationDataDto replicationData, DatabaseDto database) {
        if (replicationData == null) {
            log.warn("⚠️ No replication data to process");
            return;
        }

        log.info("=== ADDING REPLICATION TIMESTAMPS TO LOCAL DATA SERVICE ===");
        log.info("Database: {} ({})", database.getName(), database.getInternalName());
        log.info("Database ID: {}", database.getId());

        try {
            // Process replication timestamps
            if (replicationData.getReplicationTimestamps() != null && !replicationData.getReplicationTimestamps().isEmpty()) {
                log.info("📥 Processing {} replication timestamps...", replicationData.getReplicationTimestamps().size());
                addReplicationTimestampsToLocalDataService(replicationData.getReplicationTimestamps(), database);
            } else {
                log.info("ℹ️ No replication timestamps to process");
            }

            log.info("✅ Successfully processed all replication timestamps for database: {}", database.getInternalName());

        } catch (Exception e) {
            log.error("❌ Error adding replication timestamps to local data service: {}", e.getMessage(), e);
        }
    }



    /**
     * Adds replication timestamps to the local data service.
     */
    private void addReplicationTimestampsToLocalDataService(List<TupleReplicationTimestampDto> timestamps, DatabaseDto database) {
        log.info("=== ADDING REPLICATION TIMESTAMPS TO LOCAL DATA SERVICE ===");
        
        // Group timestamps by table ID
        Map<UUID, List<TupleReplicationTimestampDto>> timestampsByTable = new HashMap<>();
        
        for (TupleReplicationTimestampDto timestamp : timestamps) {
            UUID tableId = timestamp.getTableId();
            timestampsByTable.computeIfAbsent(tableId, k -> new ArrayList<>()).add(timestamp);
        }

        // Process each table's timestamps
        for (Map.Entry<UUID, List<TupleReplicationTimestampDto>> entry : timestampsByTable.entrySet()) {
            UUID tableId = entry.getKey();
            List<TupleReplicationTimestampDto> tableTimestamps = entry.getValue();
            
            log.info("📋 Processing {} timestamps for table ID: {}", tableTimestamps.size(), tableId);
            
            try {
                addTableTimestampsToLocalDataService(tableTimestamps, database.getId(), tableId);
            } catch (Exception e) {
                log.error("❌ Failed to add timestamps for table ID {}: {}", tableId, e.getMessage());
            }
        }
    }

    /**
     * Adds timestamps for a specific table to the local data service.
     */
    private void addTableTimestampsToLocalDataService(List<TupleReplicationTimestampDto> timestamps, UUID databaseId, UUID tableId) {
        try {
            // Resolve local table ID in case provided ID is from a remote site
            UUID resolvedLocalTableId = tableId;
            try {
                LocalTableIdDto localIdDto = metadataServiceGateway.getLocalTableIdByReplicaTableId(databaseId, tableId);
                if (localIdDto != null && localIdDto.getLocalTableId() != null) {
                    resolvedLocalTableId = localIdDto.getLocalTableId();
                    log.info("🔁 Resolved remote tableId {} to local tableId {}", tableId, resolvedLocalTableId);
                }
            } catch (Exception resolveEx) {
                log.warn("⚠️ Could not resolve local table ID for {}: {}. Proceeding with provided ID.", tableId, resolveEx.getMessage());
            }
            // Convert TupleReplicationTimestampDto to the format expected by the endpoint
            List<Map<String, Object>> timestampsList = new ArrayList<>();
            
            for (TupleReplicationTimestampDto timestamp : timestamps) {
                Map<String, Object> timestampMap = Map.of(
                    "siteUrl", timestamp.getSiteUrl(),
                    "replicationId", timestamp.getReplicationId(),
                    "databaseId", timestamp.getDatabaseId().toString(),
                    "tableId", timestamp.getTableId().toString(),
                    "rowStart", timestamp.getRowStart() != null ? timestamp.getRowStart().toString() : null,
                    "rowEnd", timestamp.getRowEnd() != null ? timestamp.getRowEnd().toString() : null
                );
                timestampsList.add(timestampMap);
            }

            // Build the request payload
            Map<String, Object> requestPayload = Map.of("timestamps", timestampsList);

            // Build the full URL for the timestamps endpoint
            String path = String.format("/api/v1/database/%s/table/%s/timestamps", databaseId, resolvedLocalTableId);

            log.info("🌐 Adding timestamps to local data service: {}", path);
            log.info("📤 Timestamps count: {}", timestamps.size());

            // Make the HTTP call to local data service
            ResponseEntity<Map> response = localDataServiceRestTemplate.postForEntity(
                path,
                requestPayload, 
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Successfully added {} timestamps for table ID: {}", timestamps.size(), resolvedLocalTableId);
            } else {
                log.warn("⚠️ Failed to add timestamps for table ID {}: Status {}", 
                        resolvedLocalTableId, response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error adding timestamps for table ID {}: {}", tableId, e.getMessage(), e);
        }
    }


} 