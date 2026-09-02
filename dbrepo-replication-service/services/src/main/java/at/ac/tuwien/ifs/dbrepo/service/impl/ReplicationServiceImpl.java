package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate metadataServiceRestTemplate;
    private final RestTemplate dataServiceRestTemplate;
    private final RestTemplate externalReplicationRestTemplate;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    public ReplicationServiceImpl(@Qualifier("metadataServiceRestTemplate") RestTemplate metadataServiceRestTemplate,
                                  @Qualifier("dataServiceRestTemplate") RestTemplate dataServiceRestTemplate,
                                  @Qualifier("externalReplicationRestTemplate") RestTemplate externalReplicationRestTemplate) {
        this.metadataServiceRestTemplate = metadataServiceRestTemplate;
        this.dataServiceRestTemplate = dataServiceRestTemplate;
        this.externalReplicationRestTemplate = externalReplicationRestTemplate;
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
                    continue;
                }
                siteDatabaseIds.put(site(replicaUrl), body.getId());
            } catch (Exception e) {
                log.error("Failed to replicate database {} to {}: {}", notification.getCreationId(), replicaUrl,
                        e.getMessage(), e);
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
                    continue;
                }
                siteTableIds.put(siteUrl, body.getId());
            } catch (Exception e) {
                log.error("Failed to replicate table {} to {}: {}", notification.getCreationId(), replica.getUrl(),
                        e.getMessage(), e);
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
                }
            } catch (Exception e) {
                log.error("Failed to replicate view {} to {}: {}", notification.getCreationId(), replica.getUrl(),
                        e.getMessage(), e);
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
                    continue;
                }
                timestamps.add(timestamp(replicaUrl, tuple.getReplicationKey(), remoteDatabaseId, remoteTableId,
                        tuple.getInsertedAt(), tuple.getDeletedAt()));
                successfulReplicaUrls.add(replicaUrl);
                successful++;
            } catch (Exception e) {
                log.error("Failed to replicate {} tuple {} to {}: {}", method, request.getTuple().getReplicationKey(),
                        replicaUrl, e.getMessage(), e);
            }
        }
        timestamps.add(timestamp(normalizedBaseUrl(), request.getTuple().getReplicationKey(), request.getDatabase().getId(),
                request.getTable().getId(), request.getTuple().getInsertedAt(), request.getTuple().getDeletedAt()));
        synchronizeTimestamps(request, method, timestamps, successfulReplicaUrls);
        return successful;
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
                final String path = site(replicaUrl) + "/api/v1/database/" + remoteDatabaseId + "/table/"
                        + remoteTableId + "/timestamps";
                externalReplicationRestTemplate.exchange(path, timestampHttpMethod(method), new HttpEntity<>(timestamps),
                        Map.class);
            } catch (Exception e) {
                log.error("Failed to synchronize tuple timestamps to {}: {}", replicaUrl, e.getMessage(), e);
            }
        }
    }

    private void persistLocalTimestamps(DataReplicationDto request, HttpMethod method,
                                        List<TupleReplicationTimestampDto> timestamps) {
        try {
            dataServiceRestTemplate.exchange("/api/v1/database/" + request.getDatabase().getId() + "/table/"
                    + request.getTable().getId() + "/timestamps", timestampHttpMethod(method),
                    new HttpEntity<>(timestamps), Map.class);
        } catch (Exception e) {
            log.error("Failed to persist tuple replication timestamps locally: {}", e.getMessage(), e);
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
