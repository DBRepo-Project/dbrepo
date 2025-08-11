package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
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
        
        for (String replicaUrl : replicaUrls) {
            try {
                sendDatabaseReplicationToInstance(databaseNotificationDto, replicaUrl);
            } catch (Exception e) {
                log.error("Failed to send replication to instance {}: {}", replicaUrl, e.getMessage());
            }
        }
    }

    @Override
    public void sendDatabaseReplicationToInstance(DatabaseNotificationDto databaseNotificationDto, String replicaUrl) {
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
                    } else {
                        log.warn("Response does not contain databaseId field: {}", response.getBody());
                    }
                } catch (Exception e) {
                    log.error("Failed to parse replication response: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send replication to {}: {}", replicaUrl, e.getMessage());
            throw new RuntimeException("Failed to send replication to " + replicaUrl, e);
        }
    }

    @Override
    public void sendTableReplicationToInstances(UUID databaseId, CreateTableDto createTableDto, List<ReplicaLocation> replicas, UUID creationId) {
        log.info("Sending table replication to {} instances", replicas.size());
        
        for (ReplicaLocation replica : replicas) {
            try {
                sendTableReplicationToInstance(replica.getReplicaDatabaseId(), createTableDto, replica.getUrl(), creationId);
            } catch (Exception e) {
                log.error("Failed to send table replication to instance {}: {}", replica.getUrl(), e.getMessage());
            }
        }
    }

    @Override
    public void sendTableReplicationToInstance(UUID databaseId, CreateTableDto createTableDto, String replicaUrl, UUID creationId) {
        log.info("Sending table replication to instance: {} with creationId: {}", replicaUrl, creationId);
        
        try {
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<CreateTableDto> requestEntity = new HttpEntity<>(createTableDto, headers);
            
            // Build the full URL for the table replication endpoint
            String replicationUrl = replicaUrl + "/api/replication/replicate/table?databaseId=" + databaseId;
            
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
                        updateTableReplicationUrlWithRemoteId(databaseId, creationId, replicaUrl, UUID.fromString(remoteTableId));
                    } else {
                        log.info("Table replication successful, no id in response: {}", response.getBody());
                    }
                } catch (Exception e) {
                    log.error("Failed to parse table replication response: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send table replication to {}: {}", replicaUrl, e.getMessage());
            throw new RuntimeException("Failed to send table replication to " + replicaUrl, e);
        }
    }
    
    private void updateReplicationUrlWithRemoteId(UUID databaseId, String replicaUrl, UUID remoteDatabaseId) {
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

    private void updateTableReplicationUrlWithRemoteId(UUID databaseId, UUID localTableId, String replicaUrl, UUID remoteTableId) {
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
} 