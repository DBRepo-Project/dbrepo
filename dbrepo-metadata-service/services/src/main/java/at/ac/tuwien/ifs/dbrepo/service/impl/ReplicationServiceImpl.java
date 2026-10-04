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
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationCreation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationNotificationDispatcher;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationNotificationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final MetadataMapper metadataMapper;
    private final ReplicationNotificationOutboxService outboxService;
    private final ReplicationNotificationDispatcher dispatcher;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

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
    public UUID reserveCreation(String kind, UUID parentId, String origin, UUID creationId,
                                String physicalName, Object payload) {
        final UUID legacyId = ReplicationCreation.localId(kind, parentId, origin, creationId);
        final UUID targetId = ReplicationCreation.localId(kind, parentId, origin, creationId, baseUrl);
        final String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(payload)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot fingerprint replication creation", e);
        }
        final TransactionTemplate intentTransaction = new TransactionTemplate(transactionManager);
        intentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        UUID id;
        try {
            id = intentTransaction.execute(status -> {
                final UUID selected = entityManager.find(ReplicationCreation.class, legacyId) == null
                        ? targetId : legacyId;
                if (entityManager.find(ReplicationCreation.class, selected) == null) {
                    final String name = physicalName == null ? ReplicationCreation.databaseName(selected) : physicalName;
                    entityManager.persist(ReplicationCreation.builder().id(selected).kind(kind).parentId(parentId)
                            .physicalName(name).payloadHash(hash).build());
                    entityManager.flush();
                }
                return selected;
            });
        } catch (RuntimeException failure) {
            // A competing identical request may have committed the reservation first.
            final UUID existing = intentTransaction.execute(status -> {
                if (entityManager.find(ReplicationCreation.class, legacyId) != null) return legacyId;
                return entityManager.find(ReplicationCreation.class, targetId) == null ? null : targetId;
            });
            if (existing == null) throw failure;
            id = existing;
        }
        // Held through the caller's metadata transaction, including endpoint work after create returns.
        final ReplicationCreation intent = entityManager.find(ReplicationCreation.class, id, LockModeType.PESSIMISTIC_WRITE);
        final String name = physicalName == null ? ReplicationCreation.databaseName(id) : physicalName;
        if (!hash.equals(intent.getPayloadHash()) || !name.equals(intent.getPhysicalName())) {
            throw new IllegalArgumentException("Replication creation id was reused with a different payload");
        }
        return id;
    }

    @Override
    public <T> T findCreated(Class<T> type, UUID id) {
        return entityManager.find(type, id, LockModeType.PESSIMISTIC_READ);
    }

    @Override
    public void replicateDatabase(CreateDatabaseDto createDatabaseDto, UUID creationId, UserDto owner) {
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
        dispatchAfterCommit(entry.getId());
    }

    @Override
    public void replicateTable(CreateTableDto createTableDto, UUID databaseId, List<ReplicaLocation> replicas,
                               UUID creationId) {
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
        dispatchAfterCommit(entry.getId());
    }

    @Override
    public void replicateTableDelete(Database database, Table table) {
        final Map<String, UUID> databases = new LinkedHashMap<>();
        final Map<String, UUID> tables = new LinkedHashMap<>();
        database.getReplicaUrls().forEach(replica -> {
            if (replica.getUrl() != null) {
                databases.put(replica.getUrl(), replica.getReplicaDatabaseId());
                tables.put(replica.getUrl(), null);
            }
        });
        if (table.getReplicaUrls() != null) {
            table.getReplicaUrls().forEach(replica -> {
                if (replica.getUrl() != null) {
                    tables.put(replica.getUrl(), replica.getReplicaTableId());
                }
            });
        }
        final TableDeleteNotificationDto notification = TableDeleteNotificationDto.builder()
                .databaseId(database.getId())
                .tableId(table.getId())
                .archivedAt(table.getArchivedAt())
                .databaseReplicaIds(databases)
                .tableReplicaIds(tables)
                .build();
        final ReplicationNotificationOutbox entry = outboxService.enqueue(
                ReplicationNotificationType.TABLE_DELETE, HttpMethod.DELETE, "/api/replication/table",
                notification, table.getId());
        dispatchAfterCommit(entry.getId());
    }

    @Override
    public void replicateView(View view) {
        final ViewNotificationDto notification = ViewNotificationDto.builder()
                .databaseId(view.getDatabase().getId())
                .creationId(view.getId())
                .viewDto(metadataMapper.viewToViewDto(view))
                .replicas(view.getDatabase().getReplicaUrls())
                .build();
        final ReplicationNotificationOutbox entry = outboxService.enqueue(
                ReplicationNotificationType.VIEW_CREATE, HttpMethod.POST, "/api/replication/view",
                notification, view.getId());
        dispatchAfterCommit(entry.getId());
    }

    private void dispatchAfterCommit(UUID id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.dispatchAsync(id);
                }
            });
        } else {
            dispatcher.dispatchAsync(id);
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
