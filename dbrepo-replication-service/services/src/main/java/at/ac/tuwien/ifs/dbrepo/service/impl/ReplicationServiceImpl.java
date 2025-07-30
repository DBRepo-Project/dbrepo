package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
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

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate externalReplicationRestTemplate;

    @Autowired
    public ReplicationServiceImpl(@Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate) {
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
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
            String replicationUrl = replicaUrl + "/api/replication/database";
            
            log.info("Sending POST request to: {}", replicationUrl);
            
            // Send the request
            ResponseEntity<String> response = externalReplicationRestTemplate.postForEntity(replicationUrl, requestEntity, String.class);
            
            log.info("Replication sent successfully to {} with status: {}", replicaUrl, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Failed to send replication to {}: {}", replicaUrl, e.getMessage());
            throw new RuntimeException("Failed to send replication to " + replicaUrl, e);
        }
    }
} 