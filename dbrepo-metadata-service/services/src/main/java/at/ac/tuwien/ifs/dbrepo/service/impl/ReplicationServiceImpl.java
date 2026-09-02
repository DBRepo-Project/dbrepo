package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    private final MetadataMapper metadataMapper;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    public ReplicationServiceImpl(@Qualifier("replicationRestTemplate") RestTemplate replicationRestTemplate,
                                  MetadataMapper metadataMapper) {
        this.replicationRestTemplate = replicationRestTemplate;
        this.metadataMapper = metadataMapper;
    }

    @Override
    @Async
    public void replicateDatabase(CreateDatabaseDto createDatabaseDto, UUID creationId) {
        try {
            waitForCreateTransaction();
            createDatabaseDto.setCreationLocation(baseUrl);
            final DatabaseNotificationDto notification = DatabaseNotificationDto.builder()
                    .createDatabaseDto(createDatabaseDto)
                    .creationId(creationId)
                    .build();
            final ResponseEntity<Void> response = replicationRestTemplate.exchange("/api/replication/database",
                    HttpMethod.POST, new HttpEntity<>(notification), Void.class);
            log.info("Sent database replication notification for database {}: {}", creationId,
                    response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send database replication notification for database {}: {}", creationId,
                    e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void replicateTable(CreateTableDto createTableDto, UUID databaseId, List<ReplicaLocation> replicas,
                               UUID creationId) {
        try {
            waitForCreateTransaction();
            createTableDto.setCreationLocation(baseUrl);
            final TableNotificationDto notification = TableNotificationDto.builder()
                    .databaseId(databaseId)
                    .creationId(creationId)
                    .createTableDto(createTableDto)
                    .replicas(replicas)
                    .build();
            final ResponseEntity<Void> response = replicationRestTemplate.exchange("/api/replication/table",
                    HttpMethod.POST, new HttpEntity<>(notification), Void.class);
            log.info("Sent table replication notification for table {} in database {}: {}", creationId, databaseId,
                    response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send table replication notification for table {} in database {}: {}", creationId,
                    databaseId, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void replicateView(View view) {
        try {
            waitForCreateTransaction();
            final ViewNotificationDto notification = ViewNotificationDto.builder()
                    .databaseId(view.getDatabase().getId())
                    .creationId(view.getId())
                    .viewDto(metadataMapper.viewToViewDto(view))
                    .replicas(view.getDatabase().getReplicaUrls())
                    .build();
            final ResponseEntity<Void> response = replicationRestTemplate.exchange("/api/replication/view",
                    HttpMethod.POST, new HttpEntity<>(notification), Void.class);
            log.info("Sent view replication notification for view {} in database {}: {}", view.getId(),
                    view.getDatabase().getId(), response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send view replication notification for view {}: {}", view.getId(), e.getMessage(), e);
        }
    }

    private void waitForCreateTransaction() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Replication notification interrupted", e);
        }
    }

}
