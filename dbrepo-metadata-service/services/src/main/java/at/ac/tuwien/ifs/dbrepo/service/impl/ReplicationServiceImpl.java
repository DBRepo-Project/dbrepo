package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableDeleteNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationNotificationDispatcher;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationNotificationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final MetadataMapper metadataMapper;
    private final ReplicationNotificationOutboxService outboxService;
    private final ReplicationNotificationDispatcher dispatcher;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    public ReplicationServiceImpl(MetadataMapper metadataMapper, ReplicationNotificationOutboxService outboxService,
                                  ReplicationNotificationDispatcher dispatcher) {
        this.metadataMapper = metadataMapper;
        this.outboxService = outboxService;
        this.dispatcher = dispatcher;
    }

    @Override
    public void replicateDatabase(CreateDatabaseDto createDatabaseDto, UUID creationId, UserDto owner) {
        try {
            createDatabaseDto.setCreationLocation(baseUrl);
            final DatabaseNotificationDto notification = DatabaseNotificationDto.builder()
                    .createDatabaseDto(createDatabaseDto)
                    .creationId(creationId)
                    .owner(ReplicationOwnerDto.builder()
                            .siteUrl(baseUrl)
                            .issuer(issuer)
                            .subject(owner.getId().toString())
                            .username(owner.getUsername())
                            .build())
                    .build();
            final ReplicationNotificationOutbox entry = outboxService.enqueue(
                    ReplicationNotificationType.DATABASE_CREATE, HttpMethod.POST, "/api/replication/database",
                    notification, creationId);
            dispatcher.dispatchAsync(entry.getId());
        } catch (Exception e) {
            log.error("Failed to enqueue database replication notification for database {}: {}", creationId,
                    e.getMessage(), e);
        }
    }

    @Override
    public void replicateTable(CreateTableDto createTableDto, UUID databaseId, List<ReplicaLocation> replicas,
                               UUID creationId) {
        try {
            createTableDto.setCreationLocation(baseUrl);
            final TableNotificationDto notification = TableNotificationDto.builder()
                    .databaseId(databaseId)
                    .creationId(creationId)
                    .createTableDto(createTableDto)
                    .replicas(replicas)
                    .build();
            final ReplicationNotificationOutbox entry = outboxService.enqueue(
                    ReplicationNotificationType.TABLE_CREATE, HttpMethod.POST, "/api/replication/table",
                    notification, creationId);
            dispatcher.dispatchAsync(entry.getId());
        } catch (Exception e) {
            log.error("Failed to enqueue table replication notification for table {} in database {}: {}", creationId,
                    databaseId, e.getMessage(), e);
        }
    }

    @Override
    public void replicateTableDelete(Database database, Table table) {
        final TableDeleteNotificationDto notification = TableDeleteNotificationDto.builder()
                .databaseId(database.getId())
                .tableId(table.getId())
                .databaseReplicaIds(database.getReplicaUrls()
                        .stream()
                        .filter(replica -> replica.getUrl() != null && replica.getReplicaDatabaseId() != null)
                        .collect(Collectors.toMap(ReplicaLocation::getUrl, ReplicaLocation::getReplicaDatabaseId,
                                (first, ignored) -> first)))
                .tableReplicaIds(table.getReplicaUrls()
                        .stream()
                        .filter(replica -> replica.getUrl() != null && replica.getReplicaTableId() != null)
                        .collect(Collectors.toMap(replica -> replica.getUrl(), replica -> replica.getReplicaTableId(),
                                (first, ignored) -> first)))
                .build();
        final ReplicationNotificationOutbox entry = outboxService.enqueue(
                ReplicationNotificationType.TABLE_DELETE, HttpMethod.DELETE, "/api/replication/table",
                notification, table.getId());
        dispatcher.dispatchAsync(entry.getId());
    }

    @Override
    public void replicateView(View view) {
        try {
            final ViewNotificationDto notification = ViewNotificationDto.builder()
                    .databaseId(view.getDatabase().getId())
                    .creationId(view.getId())
                    .viewDto(metadataMapper.viewToViewDto(view))
                    .replicas(view.getDatabase().getReplicaUrls())
                    .build();
            final ReplicationNotificationOutbox entry = outboxService.enqueue(
                    ReplicationNotificationType.VIEW_CREATE, HttpMethod.POST, "/api/replication/view",
                    notification, view.getId());
            dispatcher.dispatchAsync(entry.getId());
        } catch (Exception e) {
            log.error("Failed to enqueue view replication notification for view {}: {}", view.getId(), e.getMessage(),
                    e);
        }
    }

    @Override
    public List<ReplicationNotificationOutbox> findOutboxEntries() {
        return outboxService.findAll();
    }

    @Override
    public int retryDueOutboxEntries() {
        return dispatcher.dispatchDue();
    }

    @Override
    public boolean retryOutboxEntry(UUID id) {
        return dispatcher.dispatch(id);
    }

}
