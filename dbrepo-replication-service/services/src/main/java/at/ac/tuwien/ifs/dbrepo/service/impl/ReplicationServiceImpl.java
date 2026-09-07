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
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
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
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            if (replica == null || replica.getUrl() == null || replica.getReplicaDatabaseId() == null
                    || isLocalSite(replica.getUrl())) {
                continue;
            }
            final String siteUrl = site(replica.getUrl());
            siteDatabaseIds.put(siteUrl, replica.getReplicaDatabaseId());
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
    public int replicateView(ViewNotificationDto notification) {
        if (notification == null || notification.getViewDto() == null
                || notification.getReplicas() == null || notification.getReplicas().isEmpty()) {
            log.info("Skip view replication: missing replica locations");
            return 0;
        }
        int successful = 0;
        for (ReplicaLocation replica : notification.getReplicas()) {
            if (replica == null || replica.getUrl() == null || replica.getReplicaDatabaseId() == null
                    || isLocalSite(replica.getUrl())) {
                continue;
            }
            try {
                final String path = site(replica.getUrl()) + "/api/v1/database/" + replica.getReplicaDatabaseId()
                        + "/view/replicate";
                final ResponseEntity<ViewBriefDto> response = externalReplicationRestTemplate.exchange(path,
                        HttpMethod.POST, new HttpEntity<>(notification), ViewBriefDto.class);
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
                || request.getTuple() == null || request.getTuple().getReplicationKey() == null
                || request.getDatabase().getReplicaUrls() == null || request.getTable().getReplicaUrls() == null) {
            log.info("Skip tuple replication: missing database, table, tuple, replication key, or replica maps");
            return 0;
        }
        final List<TupleReplicationTimestampDto> timestamps = new ArrayList<>();
        final List<String> successfulReplicaUrls = new ArrayList<>();
        int successful = 0;
        for (Map.Entry<String, UUID> replica : request.getDatabase().getReplicaUrls().entrySet()) {
            final String replicaUrl = replica.getKey();
            final UUID remoteDatabaseId = replica.getValue();
            final UUID remoteTableId = request.getTable().getReplicaUrls().get(replicaUrl);
            if (isLocalSite(replicaUrl) || remoteDatabaseId == null || remoteTableId == null) {
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
                timestamps.add(timestamp(replicaUrl, tuple.getReplicationKey(), remoteDatabaseId, remoteTableId,
                        tuple.getInsertedAt(), tuple.getDeletedAt()));
                successfulReplicaUrls.add(replicaUrl);
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
        synchronizeTimestamps(request, method, timestamps, successfulReplicaUrls);
        return successful;
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
        if (entry == null) {
            return false;
        }
        if (ReplicationOutboxStatus.SUCCEEDED.equals(entry.getStatus())) {
            return true;
        }
        try {
            retry(entry);
            outboxService.markSucceeded(entry.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to retry replication outbox entry {}: {}", id, e.getMessage(), e);
            outboxService.markFailed(entry.getId(), e.getMessage(), retryDelayFor(entry.getAttempts() + 1),
                    maxAttempts);
            return false;
        }
    }

    private void retry(ReplicationOutboxEntry entry) throws JsonProcessingException {
        switch (entry.getOperationType()) {
            case DATABASE_CREATE -> retryDatabaseCreate(entry);
            case DATABASE_REPLICA_SYNC -> retryDatabaseReplicaSync(entry);
            case TABLE_CREATE -> retryTableCreate(entry);
            case TABLE_REPLICA_SYNC -> retryTableReplicaSync(entry);
            case VIEW_CREATE -> retryViewCreate(entry);
            case DATA_CREATE, DATA_UPDATE, DATA_DELETE -> retryData(entry);
            case TIMESTAMP_SYNC -> retryTimestampSync(entry);
            default -> throw new IllegalArgumentException("Unsupported outbox operation " + entry.getOperationType());
        }
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
        final UUID remoteDatabaseId = entry.getRemoteDatabaseId();
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
    }

    private void retryTableReplicaSync(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final TableUpdateReplicationUrlDto payload = readPayload(entry, TableUpdateReplicationUrlDto.class);
        updateTableReplica(entry.getTargetSiteUrl(), entry.getLocalDatabaseId(), entry.getLocalTableId(),
                payload.getReplicaUrl(), payload.getReplicaTableId());
    }

    private void retryViewCreate(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final ViewNotificationDto notification = readPayload(entry, ViewNotificationDto.class);
        final String path = site(entry.getTargetSiteUrl()) + "/api/v1/database/" + entry.getRemoteDatabaseId()
                + "/view/replicate";
        final ResponseEntity<ViewBriefDto> response = externalReplicationRestTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(notification), ViewBriefDto.class);
        requireBody(response, "view replication retry");
    }

    private void retryData(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final DataReplicationDto request = readPayload(entry, DataReplicationDto.class);
        final HttpMethod method = HttpMethod.valueOf(entry.getHttpMethod());
        final TupleWithTimestampsDto tuple = replicateRemoteData(entry.getTargetSiteUrl(), entry.getRemoteDatabaseId(),
                entry.getRemoteTableId(), request, method);
        final List<TupleReplicationTimestampDto> timestamps = new ArrayList<>();
        timestamps.add(timestamp(entry.getTargetSiteUrl(), tuple.getReplicationKey(), entry.getRemoteDatabaseId(),
                entry.getRemoteTableId(), tuple.getInsertedAt(), tuple.getDeletedAt()));
        timestamps.add(timestamp(normalizedBaseUrl(), request.getTuple().getReplicationKey(), request.getDatabase().getId(),
                request.getTable().getId(), request.getTuple().getInsertedAt(), request.getTuple().getDeletedAt()));
        synchronizeTimestamps(request, method, timestamps, List.of(entry.getTargetSiteUrl()));
    }

    private void retryTimestampSync(ReplicationOutboxEntry entry) throws JsonProcessingException {
        final List<TupleReplicationTimestampDto> timestamps = readPayload(entry, TIMESTAMP_LIST_TYPE);
        sendTimestamps(entry.getTargetSiteUrl(), entry.getRemoteDatabaseId(), entry.getRemoteTableId(),
                HttpMethod.valueOf(entry.getHttpMethod()), timestamps);
    }

    private <T> T requireBody(ResponseEntity<T> response, String operation) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException(operation + " returned " + response.getStatusCode());
        }
        return response.getBody();
    }

    private TupleWithTimestampsDto replicateRemoteData(String replicaUrl, UUID remoteDatabaseId, UUID remoteTableId,
                                                       DataReplicationDto request, HttpMethod method) {
        final String path = site(replicaUrl) + "/api/v1/database/" + remoteDatabaseId + "/table/"
                + remoteTableId + "/data/replicate";
        final ResponseEntity<TupleWithTimestampsDto> response = externalReplicationRestTemplate.exchange(path, method,
                new HttpEntity<>(request), TupleWithTimestampsDto.class);
        return requireBody(response, "tuple replication retry");
    }

    private Map<String, UUID> databaseReplicaIds(UUID localDatabaseId) {
        final Map<String, UUID> siteDatabaseIds = new HashMap<>();
        siteDatabaseIds.put(normalizedBaseUrl(), localDatabaseId);
        try {
            final DatabaseDto database = metadataServiceRestTemplate.getForObject("/api/v1/database/"
                    + localDatabaseId, DatabaseDto.class);
            if (database != null && database.getReplicaUrls() != null) {
                database.getReplicaUrls().forEach((siteUrl, databaseId) -> {
                    if (siteUrl != null && databaseId != null) {
                        siteDatabaseIds.put(site(siteUrl), databaseId);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to fetch current database replica ids for {}: {}", localDatabaseId, e.getMessage());
        }
        return siteDatabaseIds;
    }

    private Map<String, UUID> tableReplicaIds(UUID localDatabaseId, UUID localTableId) {
        final Map<String, UUID> siteTableIds = new HashMap<>();
        siteTableIds.put(normalizedBaseUrl(), localTableId);
        try {
            final TableDto table = metadataServiceRestTemplate.getForObject("/api/v1/database/" + localDatabaseId
                    + "/table/" + localTableId, TableDto.class);
            if (table != null && table.getReplicaUrls() != null) {
                table.getReplicaUrls().forEach((siteUrl, tableId) -> {
                    if (siteUrl != null && tableId != null) {
                        siteTableIds.put(site(siteUrl), tableId);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to fetch current table replica ids for {}: {}", localTableId, e.getMessage());
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
        try {
            outboxService.enqueue(operationType, site(targetSiteUrl), httpMethod, payload, localDatabaseId, localTableId,
                    remoteDatabaseId, remoteTableId, lastError);
        } catch (Exception e) {
            log.error("Failed to persist replication outbox entry for {} to {}: {}", operationType, targetSiteUrl,
                    e.getMessage(), e);
        }
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

    private Duration retryDelayFor(int attempt) {
        final long baseSeconds = Math.max(1, retryDelaySeconds);
        final long cappedMaxSeconds = Math.max(baseSeconds, maxRetryDelaySeconds);
        final int exponent = Math.min(Math.max(0, attempt - 1), 10);
        final long multiplier = 1L << exponent;
        final long seconds = Math.min(cappedMaxSeconds, baseSeconds * multiplier);
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
                                       List<TupleReplicationTimestampDto> timestamps,
                                       List<String> successfulReplicaUrls) {
        if (timestamps.isEmpty()) {
            return;
        }
        persistLocalTimestamps(request, method, timestamps);
        for (String replicaUrl : successfulReplicaUrls) {
            final UUID remoteDatabaseId = request.getDatabase().getReplicaUrls().get(replicaUrl);
            final UUID remoteTableId = request.getTable().getReplicaUrls().get(replicaUrl);
            if (isLocalSite(replicaUrl) || remoteDatabaseId == null || remoteTableId == null) {
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
