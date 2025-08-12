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

    @Autowired
    public ReplicationServiceImpl(@Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate,
                                GatewayConfig gatewayConfig) {
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
        this.gatewayConfig = gatewayConfig;
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
} 