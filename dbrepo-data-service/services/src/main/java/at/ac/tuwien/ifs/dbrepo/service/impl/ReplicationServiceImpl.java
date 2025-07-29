package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate replicationRestTemplate;
    
    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public ReplicationServiceImpl(RestTemplate replicationRestTemplate) {
        this.replicationRestTemplate = replicationRestTemplate;
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
}
