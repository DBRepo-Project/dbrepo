package at.ac.tuwien.ifs.dbrepo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableDeleteNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.service.DataSynchronisationResult;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseSynchronisationResult;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxOperationType;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private static final TypeReference<List<TupleReplicationTimestampDto>> TIMESTAMP_LIST_TYPE =
            new TypeReference<>() {
            };

    private final RestTemplate metadataServiceRestTemplate;
    private final RestTemplate dataServiceRestTemplate;
    private final RestTemplate externalReplicationRestTemplate;
    private final ObjectMapper objectMapper;
    private final ReplicationOutboxService outboxService;
    private final HistorySnapshotTransfer snapshots;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    @Value("${dbrepo.replication.outbox.retryDelaySeconds:30}")
    private long retryDelaySeconds;

    @Value("${dbrepo.replication.outbox.maxRetryDelaySeconds:900}")
    private long maxRetryDelaySeconds;

    @Value("${dbrepo.replication.outbox.maxAttempts:20}")
    private int maxAttempts;

    @Value("${dbrepo.replication.outbox.batchSize:25}")
    private int batchSize;

    public ReplicationServiceImpl(@Qualifier("metadataServiceRestTemplate") RestTemplate metadataServiceRestTemplate,
                                  @Qualifier("dataServiceRestTemplate") RestTemplate dataServiceRestTemplate,
                                  @Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate,
                                  ObjectMapper objectMapper, ReplicationOutboxService outboxService) {
        this.metadataServiceRestTemplate = metadataServiceRestTemplate;
        this.dataServiceRestTemplate = dataServiceRestTemplate;
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
        this.snapshots = new HistorySnapshotTransfer(dataServiceRestTemplate, externalReplicationRestTemplate);
    }

    @Override
    public int replicateDatabase(DatabaseNotificationDto notification) {
        if (notification == null || notification.getCreateDatabaseDto() == null
                || notification.getCreateDatabaseDto().getReplicaUrls() == null
                || notification.getCreateDatabaseDto().getReplicaUrls().isEmpty()) {
            log.info("Skip database replication: missing replica URLs");
            return 0;
        }
        final Map<String, UUID> siteDatabaseIds = new HashMap<>();
        siteDatabaseIds.put(normalizedBaseUrl(), notification.getCreationId());
        for (String replicaUrl : notification.getCreateDatabaseDto().getReplicaUrls()) {
            if (isLocalSite(replicaUrl)) {
                continue;
            }
            try {
                final String path = site(replicaUrl) + "/api/v1/database/replicate";
                final ResponseEntity<DatabaseBriefDto> response = externalReplicationRestTemplate.exchange(path,
                        HttpMethod.POST, new HttpEntity<>(notification), DatabaseBriefDto.class);
                final DatabaseBriefDto body = response.getBody();
                if (!response.getStatusCode().is2xxSuccessful() || body == null || body.getId() == null) {
                    log.warn("Database replication to {} returned {}", replicaUrl, response.getStatusCode());
                    enqueueFailure(ReplicationOutboxOperationType.DATABASE_CREATE, replicaUrl, HttpMethod.POST,
                            notification, notification.getCreationId(), null, null, null,
                            "Database replication returned " + response.getStatusCode());
                    continue;
                }
                siteDatabaseIds.put(site(replicaUrl), body.getId());
            } catch (Exception e) {
                log.error("Failed to replicate database {} to {}: {}", notification.getCreationId(), replicaUrl,
                        e.getMessage(), e);
                enqueueFailure(ReplicationOutboxOperationType.DATABASE_CREATE, replicaUrl, HttpMethod.POST,
                        notification, notification.getCreationId(), null, null, null, e.getMessage());
            }
        }
        synchronizeDatabaseReplicaIds(siteDatabaseIds);
        return siteDatabaseIds.size() - 1;
    }

    @Override
    public int replicateTable(TableNotificationDto notification) {
        if (notification == null || notification.getCreateTableDto() == null
                || notification.getReplicas() == null || notification.getReplicas().isEmpty()) {
            log.info("Skip table replication: missing replica locations");
            return 0;
        }
        final Map<String, UUID> siteDatabaseIds = new HashMap<>();
        final Map<String, UUID> siteTableIds = new HashMap<>();
        siteDatabaseIds.put(normalizedBaseUrl(), notification.getDatabaseId());
        siteTableIds.put(normalizedBaseUrl(), notification.getCreationId());
        for (ReplicaLocation replica : notification.getReplicas()) {
            if (replica == null || replica.getUrl() == null
                    || isLocalSite(replica.getUrl())) {
                continue;
            }
            final String siteUrl = site(replica.getUrl());
            siteDatabaseIds.put(siteUrl, replica.getReplicaDatabaseId());
            if (replica.getReplicaDatabaseId() == null) {
                enqueueFailure(ReplicationOutboxOperationType.TABLE_CREATE, siteUrl, HttpMethod.POST, notification,
                        notification.getDatabaseId(), notification.getCreationId(), null, null,
                        "Waiting for replica database mapping");
                continue;
            }
            try {
                final String path = siteUrl + "/api/v1/database/" + replica.getReplicaDatabaseId()
                        + "/table/replicate";
                final ResponseEntity<TableBriefDto> response = externalReplicationRestTemplate.exchange(path,
                        HttpMethod.POST, new HttpEntity<>(notification), TableBriefDto.class);
                final TableBriefDto body = response.getBody();
                if (!response.getStatusCode().is2xxSuccessful() || body == null || body.getId() == null) {
                    log.warn("Table replication to {} returned {}", replica.getUrl(), response.getStatusCode());
                    enqueueFailure(ReplicationOutboxOperationType.TABLE_CREATE, siteUrl, HttpMethod.POST, notification,
                            notification.getDatabaseId(), notification.getCreationId(), replica.getReplicaDatabaseId(),
                            null, "Table replication returned " + response.getStatusCode());
                    continue;
                }
                siteTableIds.put(siteUrl, body.getId());
            } catch (Exception e) {
                log.error("Failed to replicate table {} to {}: {}", notification.getCreationId(), replica.getUrl(),
                        e.getMessage(), e);
                enqueueFailure(ReplicationOutboxOperationType.TABLE_CREATE, siteUrl, HttpMethod.POST, notification,
                        notification.getDatabaseId(), notification.getCreationId(), replica.getReplicaDatabaseId(),
                        null, e.getMessage());
            }
        }
        synchronizeTableReplicaIds(siteDatabaseIds, siteTableIds);
        return siteTableIds.size() - 1;
    }

    @Override
    public int replicateTableDelete(TableDeleteNotificationDto notification) {
        if (notification == null || notification.getDatabaseReplicaIds() == null
                || notification.getTableReplicaIds() == null) {
            log.info("Skip table deletion replication: missing replica mappings");
            return 0;
        }
        int successful = 0;
        for (Map.Entry<String, UUID> tableReplica : notification.getTableReplicaIds().entrySet()) {
            final String targetSiteUrl = site(tableReplica.getKey());
            final UUID remoteTableId = tableReplica.getValue();
            final UUID remoteDatabaseId = replicaId(notification.getDatabaseReplicaIds(), targetSiteUrl);
            if (isLocalSite(targetSiteUrl)) {
                continue;
            }
            if (remoteDatabaseId == null || remoteTableId == null) {
                enqueueFailure(ReplicationOutboxOperationType.TABLE_DELETE, targetSiteUrl, HttpMethod.DELETE,
                        notification, notification.getDatabaseId(), notification.getTableId(), remoteDatabaseId,
                        remoteTableId, "Waiting for replica table mapping");
                continue;
            }
            try {
                deleteRemoteTable(targetSiteUrl, remoteDatabaseId, remoteTableId, notification.getArchivedAt());
                successful++;
            } catch (Exception e) {
                log.error("Failed to replicate table deletion {} to {}: {}", notification.getTableId(),
                        targetSiteUrl, e.getMessage(), e);
                enqueueFailure(ReplicationOutboxOperationType.TABLE_DELETE, targetSiteUrl, HttpMethod.DELETE,
                        notification, notification.getDatabaseId(), notification.getTableId(), remoteDatabaseId,
                        remoteTableId, e.getMessage());
            }
        }
        return successful;
    }

    @Override
    public int replicateView(ViewNotificationDto notification) {
        if (notification == null || notification.getViewDto() == null
                || notification.getReplicas() == null || notification.getReplicas().isEmpty()) {
            log.info("Skip view replication: missing replica locations");
            return 0;
        }
        int successful = 0;
        for (ReplicaLocation replica : notification.getReplicas()) {
            if (replica == null || replica.getUrl() == null
                    || isLocalSite(replica.getUrl())) {
                continue;
            }
            if (replica.getReplicaDatabaseId() == null) {
                enqueueFailure(ReplicationOutboxOperationType.VIEW_CREATE, replica.getUrl(), HttpMethod.POST,
                        notification, notification.getDatabaseId(), null, null, null,
                        "Waiting for replica database mapping");
                continue;
            }
            try {
                final String path = site(replica.getUrl()) + "/api/v1/database/" + replica.getReplicaDatabaseId()
                        + "/view/replicate";
                final ResponseEntity<ViewBriefDto> response = sendView(path, notification);
                if (response.getStatusCode().is2xxSuccessful()) {
                    successful++;
                } else {
                    log.warn("View replication to {} returned {}", replica.getUrl(), response.getStatusCode());
                    enqueueFailure(ReplicationOutboxOperationType.VIEW_CREATE, replica.getUrl(), HttpMethod.POST,
                            notification, notification.getDatabaseId(), null, replica.getReplicaDatabaseId(), null,
                            "View replication returned " + response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("Failed to replicate view {} to {}: {}", notification.getCreationId(), replica.getUrl(),
                        e.getMessage(), e);
                enqueueFailure(ReplicationOutboxOperationType.VIEW_CREATE, replica.getUrl(), HttpMethod.POST,
                        notification, notification.getDatabaseId(), null, replica.getReplicaDatabaseId(), null,
                        e.getMessage());
            }
        }
        return successful;
    }

    @Override
    public int replicateData(DataReplicationDto request, HttpMethod method) {
        if (request == null || request.getDatabase() == null || request.getTable() == null
                || request.getTuple() == null || request.getTuple().getReplicationKey() == null) {
            throw new IllegalArgumentException("Tuple replication requires database, table and tuple identity");
        }
        if (request.getDatabase().getCreationLocation() == null) {
            request.getDatabase().setCreationLocation(normalizedBaseUrl());
        }
        final List<TupleReplicationTimestampDto> timestamps = new ArrayList<>();
        int successful = 0;
        for (String replicaUrl : targetSites(request)) {
            final UUID remoteDatabaseId = replicaId(request.getDatabase().getReplicaUrls(), site(replicaUrl));
            final UUID remoteTableId = replicaId(request.getTable().getReplicaUrls(), site(replicaUrl));
            if (isLocalSite(replicaUrl)) {
                continue;
            }
            if (remoteDatabaseId == null || remoteTableId == null) {
                enqueueFailure(dataOperationType(method), replicaUrl, method, request, request.getDatabase().getId(),
                        request.getTable().getId(), remoteDatabaseId, remoteTableId,
                        "Waiting for replica database or table mapping");
                continue;
            }
            try {
                final String path = site(replicaUrl) + "/api/v1/database/" + remoteDatabaseId + "/table/"
                        + remoteTableId + "/data/replicate";
                final ResponseEntity<TupleWithTimestampsDto> response = externalReplicationRestTemplate.exchange(path,
                        method, new HttpEntity<>(request), TupleWithTimestampsDto.class);
                final TupleWithTimestampsDto tuple = response.getBody();
                if (!response.getStatusCode().is2xxSuccessful() || tuple == null) {
                    log.warn("{} tuple replication to {} returned {}", method, replicaUrl, response.getStatusCode());
                    enqueueFailure(dataOperationType(method), replicaUrl, method, request, request.getDatabase().getId(),
                            request.getTable().getId(), remoteDatabaseId, remoteTableId,
                            "Tuple replication returned " + response.getStatusCode());
                    continue;
                }
                if (!Boolean.FALSE.equals(tuple.getApplied())) {
                    timestamps.add(timestamp(replicaUrl, tuple.getReplicationKey(), remoteDatabaseId, remoteTableId,
                            tuple.getInsertedAt(), tuple.getDeletedAt()));
                }
                successful++;
            } catch (Exception e) {
                log.error("Failed to replicate {} tuple {} to {}: {}", method, request.getTuple().getReplicationKey(),
                        replicaUrl, e.getMessage(), e);
                enqueueFailure(dataOperationType(method), replicaUrl, method, request, request.getDatabase().getId(),
                        request.getTable().getId(), remoteDatabaseId, remoteTableId, e.getMessage());
            }
        }
        timestamps.add(timestamp(normalizedBaseUrl(), request.getTuple().getReplicationKey(), request.getDatabase().getId(),
                request.getTable().getId(), request.getTuple().getInsertedAt(), request.getTuple().getDeletedAt()));
        synchronizeTimestamps(request, method, timestamps);
        return successful;
    }

    @Override
    public DatabaseSynchronisationResult synchroniseDatabase(UUID databaseId, int pageSize) {
        requirePositivePageSize(pageSize);
        final DatabaseDto database = fetchDatabase(databaseId);
        requirePrimaryDatabase(database);
        if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
            log.info("Skip database data synchronization: missing replica URLs for database {}", databaseId);
            return new DatabaseSynchronisationResult(0, List.of());
        }
        final List<TableDto> tables = database.getTables() == null ? List.of() : database.getTables();
        int tableCount = 0;
        final List<UUID> jobs = new ArrayList<>();
        for (TableDto table : tables) {
            if (table == null || table.getId() == null || table.getReplicaUrls() == null
                    || table.getReplicaUrls().isEmpty()) {
                continue;
            }
            tableCount++;
            final DataSynchronisationResult tableResult = synchroniseData(database, table, pageSize);
            jobs.addAll(tableResult.jobs());
        }
        log.info("Queued {} history synchronization jobs for {} tables of database {}", jobs.size(), tableCount, databaseId);
        return new DatabaseSynchronisationResult(tableCount, jobs);
    }

    @Override
    public DataSynchronisationResult synchroniseData(UUID databaseId, UUID tableId, int pageSize) {
        requirePositivePageSize(pageSize);
        final DatabaseDto database = fetchDatabase(databaseId);
        requirePrimaryDatabase(database);
        final TableDto table = fetchTable(databaseId, tableId);
        return synchroniseData(database, table, pageSize);
    }

    private DataSynchronisationResult synchroniseData(DatabaseDto database, TableDto table, int pageSize) {
        final List<UUID> jobs = new ArrayList<>();
        for (String target : targetSites(DataReplicationDto.builder().database(database).table(table).build())) {
            if (!isLocalSite(target)) {
                final ReplicationOutboxEntry job = outboxService.enqueue(ReplicationOutboxOperationType.HISTORY_SYNC,
                        target, HttpMethod.POST, new HistorySyncRequest(UUID.randomUUID(), pageSize), database.getId(),
                        table.getId(), replicaId(database.getReplicaUrls(), target), replicaId(table.getReplicaUrls(), target), null);
                jobs.add(job.getId());
            }
        }
        return new DataSynchronisationResult(jobs);
    }

    public record HistorySyncRequest(UUID snapshotId, int pageSize, boolean checkpointCaptured,
                                     at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.Checkpoint checkpoint) {
        public HistorySyncRequest(UUID snapshotId, int pageSize) { this(snapshotId, pageSize, false, null); }
    }

    private void requirePositivePageSize(int pageSize) {
        if (pageSize <= 0 || pageSize > 1000) {
            throw new IllegalArgumentException("Page size must be between 1 and 1000");
        }
    }

    private void requirePrimaryDatabase(DatabaseDto database) {
        if (!site(database.getCreationLocation()).isEmpty() && !isLocalSite(database.getCreationLocation())) {
            throw new IllegalArgumentException("Synchronisation can only be started on the primary site");
        }
    }

    @Override
    public List<ReplicationOutboxEntry> findOutboxEntries() {
        return outboxService.findAll();
    }

    @Override
    public int retryDueOutboxEntries() {
        int retried = 0;
        for (ReplicationOutboxEntry entry : outboxService.findDue(Instant.now(), batchSize)) {
            if (retryOutboxEntry(entry.getId())) {
                retried++;
            }
        }
        return retried;
    }

    @Override
    public boolean retryOutboxEntry(UUID id) {
        final ReplicationOutboxEntry entry = outboxService.findById(id).orElse(null);
        if (entry == null || entry.getStatus() == ReplicationOutboxStatus.CANCELLED) {
            return false;
        }
        if (ReplicationOutboxStatus.SUCCEEDED.equals(entry.getStatus())) {
            return true;
        }
        try {
            retry(entry);
            outboxService.markSucceeded(entry.getId());
            return true;
        } catch (ReplicaDependencyPendingException e) {
            outboxService.defer(entry.getId(), e.getMessage(), retryDelayFor(entry.getAttempts()), Math.max(1, maxAttempts));
            return false;
        } catch (Exception e) {
            log.error("Failed to retry replication outbox entry {}: {}", id, e.getMessage(), e);
            outboxService.markFailed(entry.getId(), e.getMessage(), retryDelayFor(entry.getAttempts()),
                    Math.max(1, maxAttempts), isRecoverable(e));
            return false;
        }
    }

    private void retry(ReplicationOutboxEntry entry) throws JsonProcessingException {
        switch (entry.getOperationType()) {
            case DATABASE_CREATE -> retryDatabaseCreate(entry);
            case DATABASE_REPLICA_SYNC -> retryDatabaseReplicaSync(entry);
            case TABLE_CREATE -> retryTableCreate(entry);
            case TABLE_DELETE -> retryTableDelete(entry);
            case TABLE_REPLICA_SYNC -> retryTableReplicaSync(entry);
            case VIEW_CREATE -> retryViewCreate(entry);
            case DATA_CREATE, DATA_UPDATE, DATA_DELETE -> retryData(entry);
            case TIMESTAMP_SYNC -> retryTimestampSync(entry);
            case HISTORY_SYNC -> retryHistorySnapshot(entry);
            default -> throw new IllegalArgumentException("Unsupported outbox operation " + entry.getOperationType());
        }
    }

    private void retryHistorySnapshot(ReplicationOutboxEntry entry) throws JsonProcessingException {
        HistorySyncRequest request = readPayload(entry, HistorySyncRequest.class);
        requirePositivePageSize(request.pageSize());
        final DatabaseDto database = fetchDatabase(entry.getLocalDatabaseId());
        requirePrimaryDatabase(database);
        final TableDto table = fetchTable(entry.getLocalDatabaseId(), entry.getLocalTableId());
        final UUID remoteDatabaseId = resolveDatabaseId(entry);
        final UUID remoteTableId = resolveTableId(entry);
        if (!request.checkpointCaptured()) {
            final HistorySyncRequest captured = new HistorySyncRequest(request.snapshotId(), request.pageSize(), true,
                    snapshots.checkpoint(site(entry.getTargetSiteUrl()), remoteDatabaseId, remoteTableId));
            outboxService.bindSnapshotCheckpoint(entry.getId(), entry.getPayloadJson(), captured);
            request = readPayload(outboxService.findById(entry.getId()).orElseThrow(), HistorySyncRequest.class);
            if (!request.checkpointCaptured()) {
                throw new ReplicaDependencyPendingException("Waiting for durable snapshot checkpoint capture");
            }
        }
        snapshots.transfer(request.snapshotId(), database.getId(), table.getId(), site(entry.getTargetSiteUrl()),
                remoteDatabaseId, remoteTableId, request.pageSize(), request.checkpoint(), () -> {
                    if (outboxService.findById(entry.getId()).orElseThrow().getStatus() == ReplicationOutboxStatus.CANCELLED) {
                        throw new IllegalStateException("History synchronization was cancelled");
                    }
                }, event -> {
                    final DataReplicationDto payload = event.payload();
                    payload.setDatabase(database);
                    payload.setTable(table);
                    final HttpMethod method = HttpMethod.valueOf(event.method());
                    if (!HttpMethod.POST.equals(method) && !HttpMethod.PUT.equals(method) && !HttpMethod.DELETE.equals(method)) {
                        throw new IllegalArgumentException("Unsupported source journal operation");
                    }
                    final TupleWithTimestampsDto applied = replicateRemoteData(entry.getTargetSiteUrl(), remoteDatabaseId,
                            remoteTableId, payload, method);
                    final List<TupleReplicationTimestampDto> timestamps = new ArrayList<>();
                    if (!Boolean.FALSE.equals(applied.getApplied())) {
                        timestamps.add(timestamp(entry.getTargetSiteUrl(), applied.getReplicationKey(), remoteDatabaseId,
                                remoteTableId, applied.getInsertedAt(), applied.getDeletedAt()));
                    }
                    timestamps.add(timestamp(normalizedBaseUrl(), payload.getTuple().getReplicationKey(), database.getId(),
                            table.getId(), payload.getTuple().getInsertedAt(), payload.getTuple().getDeletedAt()));
                    synchronizeTimestamps(payload, method, timestamps);
                });
    }

    private void retryDatabaseCreate(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final DatabaseNotificationDto notification = readPayload(entry, DatabaseNotificationDto.class);
        final String path = site(entry.getTargetSiteUrl()) + "/api/v1/database/replicate";
        final ResponseEntity<DatabaseBriefDto> response = externalReplicationRestTemplate.exchange(path,
                HttpMethod.POST, new HttpEntity<>(notification), DatabaseBriefDto.class);
        final DatabaseBriefDto body = requireBody(response, "database replication retry");
        if (body.getId() == null) {
            throw new IllegalStateException("database replication retry returned no database id");
        }
        final Map<String, UUID> siteDatabaseIds = databaseReplicaIds(notification.getCreationId());
        siteDatabaseIds.put(site(entry.getTargetSiteUrl()), body.getId());
        synchronizeDatabaseReplicaIds(siteDatabaseIds);
    }

    private void retryDatabaseReplicaSync(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final DatabaseUpdateReplicationUrlDto payload = readPayload(entry, DatabaseUpdateReplicationUrlDto.class);
        updateDatabaseReplica(entry.getTargetSiteUrl(), entry.getLocalDatabaseId(), payload.getReplicaUrl(),
                payload.getReplicaDatabaseId());
    }

    private void retryTableCreate(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final TableNotificationDto notification = readPayload(entry, TableNotificationDto.class);
        final UUID remoteDatabaseId = resolveDatabaseId(entry);
        final String path = site(entry.getTargetSiteUrl()) + "/api/v1/database/" + remoteDatabaseId
                + "/table/replicate";
        final ResponseEntity<TableBriefDto> response = externalReplicationRestTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(notification), TableBriefDto.class);
        final TableBriefDto body = requireBody(response, "table replication retry");
        if (body.getId() == null) {
            throw new IllegalStateException("table replication retry returned no table id");
        }
        final Map<String, UUID> siteDatabaseIds = databaseReplicaIds(notification.getDatabaseId());
        final Map<String, UUID> siteTableIds = tableReplicaIds(notification.getDatabaseId(),
                notification.getCreationId());
        siteDatabaseIds.put(site(entry.getTargetSiteUrl()), remoteDatabaseId);
        siteTableIds.put(site(entry.getTargetSiteUrl()), body.getId());
        synchronizeTableReplicaIds(siteDatabaseIds, siteTableIds);
        final TableDto current = fetchTable(notification.getDatabaseId(), notification.getCreationId());
        if (current.getArchivedAt() != null) {
            deleteRemoteTable(entry.getTargetSiteUrl(), remoteDatabaseId, body.getId(), current.getArchivedAt());
        }
    }

    private void retryTableReplicaSync(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final TableUpdateReplicationUrlDto payload = readPayload(entry, TableUpdateReplicationUrlDto.class);
        updateTableReplica(entry.getTargetSiteUrl(), entry.getLocalDatabaseId(), entry.getLocalTableId(),
                payload.getReplicaUrl(), payload.getReplicaTableId());
    }

    private void retryTableDelete(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final TableDeleteNotificationDto notification = readPayload(entry, TableDeleteNotificationDto.class);
        deleteRemoteTable(entry.getTargetSiteUrl(), resolveDatabaseId(entry), resolveTableId(entry),
                notification.getArchivedAt());
    }

    private void retryViewCreate(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final ViewNotificationDto notification = readPayload(entry, ViewNotificationDto.class);
        final String path = site(entry.getTargetSiteUrl()) + "/api/v1/database/" + resolveDatabaseId(entry)
                + "/view/replicate";
        final ResponseEntity<ViewBriefDto> response = sendView(path, notification);
        requireBody(response, "view replication retry");
    }

    private ResponseEntity<ViewBriefDto> sendView(String path, ViewNotificationDto notification) {
        final DatabaseDto source = fetchDatabase(notification.getDatabaseId());
        // Resolve source-qualified names through the receiver connection's local database.
        // Copy the payload: retained retry events must keep their original SQL and identity.
        final var view = notification.getViewDto().toBuilder()
                .query(ViewSqlSchema.localize(notification.getViewDto().getQuery(), source.getInternalName()))
                .build();
        final ViewNotificationDto localized = ViewNotificationDto.builder()
                .databaseId(notification.getDatabaseId())
                .creationId(notification.getCreationId())
                .viewDto(view)
                .replicas(notification.getReplicas())
                .build();
        return externalReplicationRestTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(localized), ViewBriefDto.class);
    }

    private void retryData(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final DataReplicationDto request = readPayload(entry, DataReplicationDto.class);
        final HttpMethod method = HttpMethod.valueOf(entry.getHttpMethod());
        final UUID remoteDatabaseId = resolveDatabaseId(entry);
        final UUID remoteTableId = resolveTableId(entry);
        request.getDatabase().setReplicaUrls(new HashMap<>(databaseReplicaIds(entry.getLocalDatabaseId())));
        request.getTable().setReplicaUrls(new HashMap<>(tableReplicaIds(entry.getLocalDatabaseId(), entry.getLocalTableId())));
        request.getDatabase().getReplicaUrls().put(site(entry.getTargetSiteUrl()), remoteDatabaseId);
        request.getTable().getReplicaUrls().put(site(entry.getTargetSiteUrl()), remoteTableId);
        final TupleWithTimestampsDto tuple = replicateRemoteData(entry.getTargetSiteUrl(), remoteDatabaseId,
                remoteTableId, request, method);
        final List<TupleReplicationTimestampDto> timestamps = new ArrayList<>();
        if (!Boolean.FALSE.equals(tuple.getApplied())) {
            timestamps.add(timestamp(entry.getTargetSiteUrl(), tuple.getReplicationKey(), remoteDatabaseId,
                    remoteTableId, tuple.getInsertedAt(), tuple.getDeletedAt()));
        }
        timestamps.add(timestamp(normalizedBaseUrl(), request.getTuple().getReplicationKey(), request.getDatabase().getId(),
                request.getTable().getId(), request.getTuple().getInsertedAt(), request.getTuple().getDeletedAt()));
        synchronizeTimestamps(request, method, timestamps);
    }

    private void retryTimestampSync(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final List<TupleReplicationTimestampDto> timestamps = readPayload(entry, TIMESTAMP_LIST_TYPE);
        sendTimestamps(entry.getTargetSiteUrl(), resolveDatabaseId(entry), resolveTableId(entry),
                HttpMethod.valueOf(entry.getHttpMethod()), timestamps);
    }

    private <T> T requireBody(ResponseEntity<T> response, String operation) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientResponseException(operation + " returned " + response.getStatusCode(),
                    response.getStatusCode().value(), response.getStatusCode().toString(), response.getHeaders(), null, null);
        }
        if (response.getBody() == null) {
            throw new IllegalStateException(operation + " returned " + response.getStatusCode());
        }
        return response.getBody();
    }

    private DatabaseDto fetchDatabase(UUID databaseId) {
        final ResponseEntity<DatabaseDto> response = metadataServiceRestTemplate.exchange("/api/v1/database/"
                + databaseId, HttpMethod.GET, HttpEntity.EMPTY, DatabaseDto.class);
        return requireBody(response, "database lookup");
    }

    private TableDto fetchTable(UUID databaseId, UUID tableId) {
        final ResponseEntity<TableDto> response = metadataServiceRestTemplate.exchange("/api/v1/database/"
                + databaseId + "/table/" + tableId, HttpMethod.GET, HttpEntity.EMPTY, TableDto.class);
        return requireBody(response, "table lookup");
    }

    private TupleWithTimestampsDto replicateRemoteData(String replicaUrl, UUID remoteDatabaseId, UUID remoteTableId,
                                                       DataReplicationDto request, HttpMethod method) {
        if (request.getDatabase().getCreationLocation() == null) {
            request.getDatabase().setCreationLocation(normalizedBaseUrl());
        }
        final String path = site(replicaUrl) + "/api/v1/database/" + remoteDatabaseId + "/table/"
                + remoteTableId + "/data/replicate";
        final ResponseEntity<TupleWithTimestampsDto> response = externalReplicationRestTemplate.exchange(path, method,
                new HttpEntity<>(request), TupleWithTimestampsDto.class);
        return requireBody(response, "tuple replication retry");
    }

    private void deleteRemoteTable(String targetSiteUrl, UUID remoteDatabaseId, UUID remoteTableId, Instant archivedAt) {
        final String path = site(targetSiteUrl) + "/api/v1/database/" + remoteDatabaseId + "/table/"
                + remoteTableId + "/replicate" + (archivedAt == null ? "" : "?archivedAt=" + archivedAt);
        try {
            externalReplicationRestTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Replicated table {} is already absent on {}", remoteTableId, targetSiteUrl);
        }
    }

    private UUID replicaId(Map<String, UUID> replicaIds, String siteUrl) {
        if (replicaIds != null) {
            for (Map.Entry<String, UUID> entry : replicaIds.entrySet()) {
                if (site(entry.getKey()).equals(siteUrl)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Set<String> targetSites(DataReplicationDto request) {
        final Set<String> targets = new LinkedHashSet<>();
        if (request.getDatabase().getReplicaUrls() != null) {
            request.getDatabase().getReplicaUrls().keySet().forEach(url -> targets.add(site(url)));
        }
        if (request.getTable().getReplicaUrls() != null) {
            request.getTable().getReplicaUrls().keySet().forEach(url -> targets.add(site(url)));
        }
        return targets;
    }

    private UUID resolveDatabaseId(ReplicationOutboxEntry entry) {
        final UUID id = entry.getRemoteDatabaseId() != null ? entry.getRemoteDatabaseId()
                : replicaId(fetchDatabase(entry.getLocalDatabaseId()).getReplicaUrls(), site(entry.getTargetSiteUrl()));
        if (id == null) {
            throw new ReplicaDependencyPendingException("Waiting for replica database mapping");
        }
        return id;
    }

    private UUID resolveTableId(ReplicationOutboxEntry entry) {
        final UUID id = entry.getRemoteTableId() != null ? entry.getRemoteTableId()
                : replicaId(fetchTable(entry.getLocalDatabaseId(), entry.getLocalTableId()).getReplicaUrls(),
                        site(entry.getTargetSiteUrl()));
        if (id == null) {
            throw new ReplicaDependencyPendingException("Waiting for replica table mapping");
        }
        return id;
    }

    private static class ReplicaDependencyPendingException extends IllegalStateException {
        ReplicaDependencyPendingException(String message) {
            super(message);
        }
    }

    private Map<String, UUID> databaseReplicaIds(UUID localDatabaseId) {
        final Map<String, UUID> siteDatabaseIds = new HashMap<>();
        siteDatabaseIds.put(normalizedBaseUrl(), localDatabaseId);
        final DatabaseDto database = fetchDatabase(localDatabaseId);
        if (database.getReplicaUrls() != null) {
            database.getReplicaUrls().forEach((siteUrl, databaseId) -> {
                if (siteUrl != null && databaseId != null) {
                    siteDatabaseIds.put(site(siteUrl), databaseId);
                }
            });
        }
        return siteDatabaseIds;
    }

    private Map<String, UUID> tableReplicaIds(UUID localDatabaseId, UUID localTableId) {
        final Map<String, UUID> siteTableIds = new HashMap<>();
        siteTableIds.put(normalizedBaseUrl(), localTableId);
        final TableDto table = fetchTable(localDatabaseId, localTableId);
        if (table.getReplicaUrls() != null) {
            table.getReplicaUrls().forEach((siteUrl, tableId) -> {
                if (siteUrl != null && tableId != null) {
                    siteTableIds.put(site(siteUrl), tableId);
                }
            });
        }
        return siteTableIds;
    }

    private <T> T readPayload(ReplicationOutboxEntry entry, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(entry.getPayloadJson(), type);
    }

    private <T> T readPayload(ReplicationOutboxEntry entry, TypeReference<T> type) throws JsonProcessingException {
        return objectMapper.readValue(entry.getPayloadJson(), type);
    }

    private void enqueueFailure(ReplicationOutboxOperationType operationType, String targetSiteUrl,
                                HttpMethod httpMethod, Object payload, UUID localDatabaseId, UUID localTableId,
                                UUID remoteDatabaseId, UUID remoteTableId, String lastError) {
        outboxService.enqueue(operationType, site(targetSiteUrl), httpMethod, payload, localDatabaseId, localTableId,
                remoteDatabaseId, remoteTableId, lastError);
    }

    private ReplicationOutboxOperationType dataOperationType(HttpMethod method) {
        if (HttpMethod.PUT.equals(method)) {
            return ReplicationOutboxOperationType.DATA_UPDATE;
        }
        if (HttpMethod.DELETE.equals(method)) {
            return ReplicationOutboxOperationType.DATA_DELETE;
        }
        return ReplicationOutboxOperationType.DATA_CREATE;
    }

    private boolean isRecoverable(Exception error) {
        if (error instanceof ResourceAccessException) {
            return true;
        }
        if (error instanceof RestClientResponseException response) {
            final int status = response.getStatusCode().value();
            return status == 408 || status == 429 || status >= 500 && status <= 599;
        }
        return false;
    }

    private Duration retryDelayFor(int previousAttempts) {
        final long baseSeconds = Math.max(1, retryDelaySeconds);
        final long cappedMaxSeconds = Math.max(baseSeconds, maxRetryDelaySeconds);
        final long multiplier = 1L << Math.min(Math.max(0, previousAttempts), 62);
        final long seconds = baseSeconds > cappedMaxSeconds / multiplier ? cappedMaxSeconds : baseSeconds * multiplier;
        return Duration.ofSeconds(seconds);
    }

    private void synchronizeDatabaseReplicaIds(Map<String, UUID> siteDatabaseIds) {
        for (Map.Entry<String, UUID> localSite : siteDatabaseIds.entrySet()) {
            for (Map.Entry<String, UUID> remoteSite : siteDatabaseIds.entrySet()) {
                if (localSite.getKey().equals(remoteSite.getKey())) {
                    continue;
                }
                try {
                    updateDatabaseReplica(localSite.getKey(), localSite.getValue(), remoteSite.getKey(),
                            remoteSite.getValue());
                } catch (Exception e) {
                    log.error("Failed to update database replica id on {} for {}: {}", localSite.getKey(),
                            remoteSite.getKey(), e.getMessage(), e);
                    enqueueFailure(ReplicationOutboxOperationType.DATABASE_REPLICA_SYNC, localSite.getKey(),
                            HttpMethod.PUT, DatabaseUpdateReplicationUrlDto.builder()
                                    .replicaUrl(remoteSite.getKey())
                                    .replicaDatabaseId(remoteSite.getValue())
                                    .build(), localSite.getValue(), null, remoteSite.getValue(), null,
                            e.getMessage());
                }
            }
        }
    }

    private void synchronizeTableReplicaIds(Map<String, UUID> siteDatabaseIds, Map<String, UUID> siteTableIds) {
        for (Map.Entry<String, UUID> localSite : siteTableIds.entrySet()) {
            final UUID localDatabaseId = siteDatabaseIds.get(localSite.getKey());
            if (localDatabaseId == null) {
                continue;
            }
            for (Map.Entry<String, UUID> remoteSite : siteTableIds.entrySet()) {
                if (localSite.getKey().equals(remoteSite.getKey())) {
                    continue;
                }
                try {
                    updateTableReplica(localSite.getKey(), localDatabaseId, localSite.getValue(), remoteSite.getKey(),
                            remoteSite.getValue());
                } catch (Exception e) {
                    log.error("Failed to update table replica id on {} for {}: {}", localSite.getKey(),
                            remoteSite.getKey(), e.getMessage(), e);
                    enqueueFailure(ReplicationOutboxOperationType.TABLE_REPLICA_SYNC, localSite.getKey(),
                            HttpMethod.PUT, TableUpdateReplicationUrlDto.builder()
                                    .replicaUrl(remoteSite.getKey())
                                    .replicaTableId(remoteSite.getValue())
                                    .build(), localDatabaseId, localSite.getValue(),
                            siteDatabaseIds.get(remoteSite.getKey()), remoteSite.getValue(), e.getMessage());
                }
            }
        }
    }

    private void updateDatabaseReplica(String localSiteUrl, UUID localDatabaseId, String replicaUrl,
                                       UUID remoteDatabaseId) {
        if (localDatabaseId == null) {
            log.warn("Skip database replica update on {}: local database id is missing", localSiteUrl);
            return;
        }
        final DatabaseUpdateReplicationUrlDto payload = DatabaseUpdateReplicationUrlDto.builder()
                .replicaUrl(replicaUrl)
                .replicaDatabaseId(remoteDatabaseId)
                .build();
        restTemplateFor(localSiteUrl).exchange(pathFor(localSiteUrl, "/api/v1/database/" + localDatabaseId
                        + "/replication-url"), HttpMethod.PUT, new HttpEntity<>(payload), DatabaseBriefDto.class);
    }

    private void updateTableReplica(String localSiteUrl, UUID localDatabaseId, UUID localTableId, String replicaUrl,
                                    UUID remoteTableId) {
        if (localDatabaseId == null || localTableId == null) {
            log.warn("Skip table replica update on {}: local database or table id is missing", localSiteUrl);
            return;
        }
        final TableUpdateReplicationUrlDto payload = TableUpdateReplicationUrlDto.builder()
                .replicaUrl(replicaUrl)
                .replicaTableId(remoteTableId)
                .build();
        restTemplateFor(localSiteUrl).exchange(pathFor(localSiteUrl, "/api/v1/database/" + localDatabaseId
                        + "/table/" + localTableId + "/replication-url"), HttpMethod.PUT, new HttpEntity<>(payload),
                TableBriefDto.class);
    }

    private void synchronizeTimestamps(DataReplicationDto request, HttpMethod method,
                                       List<TupleReplicationTimestampDto> timestamps) {
        if (timestamps.isEmpty()) {
            return;
        }
        persistLocalTimestamps(request, method, timestamps);
        for (String replicaUrl : targetSites(request)) {
            final UUID remoteDatabaseId = replicaId(request.getDatabase().getReplicaUrls(), site(replicaUrl));
            final UUID remoteTableId = replicaId(request.getTable().getReplicaUrls(), site(replicaUrl));
            if (isLocalSite(replicaUrl)) {
                continue;
            }
            try {
                sendTimestamps(replicaUrl, remoteDatabaseId, remoteTableId, timestampHttpMethod(method), timestamps);
            } catch (Exception e) {
                log.error("Failed to synchronize tuple timestamps to {}: {}", replicaUrl, e.getMessage(), e);
                enqueueFailure(ReplicationOutboxOperationType.TIMESTAMP_SYNC, replicaUrl, timestampHttpMethod(method),
                        timestamps, request.getDatabase().getId(), request.getTable().getId(), remoteDatabaseId,
                        remoteTableId, e.getMessage());
            }
        }
    }

    private void sendTimestamps(String siteUrl, UUID databaseId, UUID tableId, HttpMethod method,
                                List<TupleReplicationTimestampDto> timestamps) {
        if (databaseId == null || tableId == null) {
            throw new IllegalArgumentException("Cannot synchronize tuple timestamps without database and table ids");
        }
        final String path = "/api/v1/database/" + databaseId + "/table/" + tableId + "/timestamps";
        final RestTemplate restTemplate = isLocalSite(siteUrl) ? dataServiceRestTemplate : externalReplicationRestTemplate;
        restTemplate.exchange(isLocalSite(siteUrl) ? path : site(siteUrl) + path, method, new HttpEntity<>(timestamps),
                Map.class);
    }

    private void persistLocalTimestamps(DataReplicationDto request, HttpMethod method,
                                        List<TupleReplicationTimestampDto> timestamps) {
        try {
            sendTimestamps(normalizedBaseUrl(), request.getDatabase().getId(), request.getTable().getId(),
                    timestampHttpMethod(method), timestamps);
        } catch (Exception e) {
            log.error("Failed to persist tuple replication timestamps locally: {}", e.getMessage(), e);
            enqueueFailure(ReplicationOutboxOperationType.TIMESTAMP_SYNC, normalizedBaseUrl(),
                    timestampHttpMethod(method), timestamps, request.getDatabase().getId(), request.getTable().getId(),
                    request.getDatabase().getId(), request.getTable().getId(), e.getMessage());
        }
    }

    private HttpMethod timestampHttpMethod(HttpMethod dataMethod) {
        if (HttpMethod.PUT.equals(dataMethod)) {
            return HttpMethod.PUT;
        }
        if (HttpMethod.DELETE.equals(dataMethod)) {
            return HttpMethod.PATCH;
        }
        return HttpMethod.POST;
    }

    private TupleReplicationTimestampDto timestamp(String siteUrl, String replicationId, UUID databaseId, UUID tableId,
                                                   Instant rowStart, Instant rowEnd) {
        return TupleReplicationTimestampDto.builder()
                .siteUrl(site(siteUrl))
                .replicationId(replicationId)
                .databaseId(databaseId)
                .tableId(tableId)
                .rowStart(rowStart)
                .rowEnd(rowEnd)
                .build();
    }

    private boolean isLocalSite(String siteUrl) {
        return site(siteUrl).equals(normalizedBaseUrl());
    }

    private String normalizedBaseUrl() {
        return site(baseUrl);
    }

    private RestTemplate restTemplateFor(String siteUrl) {
        return isLocalSite(siteUrl) ? metadataServiceRestTemplate : externalReplicationRestTemplate;
    }

    private String pathFor(String siteUrl, String path) {
        return isLocalSite(siteUrl) ? path : site(siteUrl) + path;
    }

    private String site(String url) {
        if (url == null) {
            return "";
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
