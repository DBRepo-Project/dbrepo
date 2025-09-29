package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationForwardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ReplicationForwardingServiceImpl implements ReplicationForwardingService {

    private final RabbitTemplate rabbitTemplate;
    private final CacheService cacheService;

    @Value("${dbrepo.replication.timestamps.exchangeName:dbrepo-replication-timestamps}")
    private String replicationTimestampsExchangeName;

    @Value("${dbrepo.replication.timestampForwarding.exchangeName:dbrepo-replication-timestamp-forwarding}")
    private String replicationTimestampForwardingExchangeName;

    @Value("${dbrepo.replication.siteId:}")
    private String localSiteId;

    @Autowired
    public ReplicationForwardingServiceImpl(RabbitTemplate rabbitTemplate, CacheService cacheService) {
        this.rabbitTemplate = rabbitTemplate;
        this.cacheService = cacheService;
    }


    @Override
    public void forwardTimestampToForwardingQueue(TupleReplicationTimestampDto dto, String sourceSiteId, String originalRoutingKey) {
        try {
            final DatabaseDto database = cacheService.getLocalDatabaseByRemoteDatabaseId(dto.getDatabaseId());

            if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
                log.debug("No replica URLs configured for database with remoteId {}, skipping timestamp forwarding", dto.getDatabaseId());
                return;
            }

            int forwardedCount = 0;
            for (var entry : database.getReplicaUrls().entrySet()) {
                final String replicaUrl = entry.getKey();
                final String replicaSiteId = extractSiteIdFromUrl(replicaUrl);

                if (replicaSiteId.equals(sourceSiteId) || replicaSiteId.equals(localSiteId)) {
                    log.debug("Skipping timestamp forwarding to source site {} or local site {}", sourceSiteId, localSiteId);
                    continue;
                }

                try {
                    final String forwardingRoutingKey = "dbrepo.timestamp-forwarding." + replicaSiteId + "." + dto.getDatabaseId() + "." + dto.getTableId();
                    rabbitTemplate.convertAndSend(replicationTimestampForwardingExchangeName, forwardingRoutingKey, dto);

                    log.info("Forwarded timestamp to forwarding queue for replica site={}, routingKey={}, replicationId={}",
                            replicaSiteId, forwardingRoutingKey, dto.getReplicationId());
                    forwardedCount++;

                } catch (Exception e) {
                    log.error("Failed to forward timestamp to replica site {}: {}", replicaSiteId, e.getMessage());
                }
            }

            log.info("Timestamp forwarding completed: forwarded {} out of {} replicas",
                    forwardedCount, database.getReplicaUrls().size());

        } catch (Exception e) {
            log.error("Failed to forward timestamp to forwarding queue: {}", e.getMessage());
        }
    }

    @Override
    public void forwardTimestampToForwardingQueue(TupleReplicationTimestampDto dto, DatabaseDto database) {
        try {
            if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
                log.debug("No replica URLs configured for database {}, skipping timestamp forwarding", database.getInternalName());
                return;
            }

            int forwardedCount = 0;
            for (var entry : database.getReplicaUrls().entrySet()) {
                final String replicaUrl = entry.getKey();
                final String replicaSiteId = extractSiteIdFromUrl(replicaUrl);

                if (replicaSiteId.equals(dto.getSiteUrl()) || replicaSiteId.equals(localSiteId)) {
                    log.info("Skipping timestamp forwarding to source site {} or local site {}", dto.getSiteUrl(), localSiteId);
                    continue;
                }

                try {
                    final String forwardingRoutingKey = "dbrepo.timestamp-forwarding." + replicaSiteId + "." + dto.getDatabaseId() + "." + dto.getTableId();
                    rabbitTemplate.convertAndSend(replicationTimestampForwardingExchangeName, forwardingRoutingKey, dto);

                    log.info("Forwarded timestamp to forwarding queue for replica site={}, routingKey={}, replicationId={}",
                            replicaSiteId, forwardingRoutingKey, dto.getReplicationId());
                    forwardedCount++;

                } catch (Exception e) {
                    log.error("Failed to forward timestamp to replica site {}: {}", replicaSiteId, e.getMessage());
                }
            }

            log.info("Timestamp forwarding completed: forwarded {} out of {} replicas",
                    forwardedCount, database.getReplicaUrls().size());

        } catch (Exception e) {
            log.error("Failed to forward timestamp to forwarding queue: {}", e.getMessage());
        }
    }

    @Override
    public String extractSourceSiteId(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.contains(".")) {
            final String[] parts = value.split("\\.");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return value;
    }

    @Override
    public String extractSiteIdFromUrl(String replicaUrl) {
        if (replicaUrl == null || replicaUrl.isEmpty()) {
            return "unknown";
        }

        try {
            if (replicaUrl.contains("://") && replicaUrl.contains(".datalab.tuwien.ac.at")) {
                String hostname = replicaUrl.substring(replicaUrl.indexOf("://") + 3);
                if (hostname.contains(".")) {
                    return hostname.substring(0, hostname.indexOf("."));
                }
            }

            return replicaUrl.replaceAll("[^a-zA-Z0-9]", "_");
        } catch (Exception e) {
            log.warn("Could not extract site ID from URL: {}, using 'unknown'", replicaUrl);
            return "unknown";
        }
    }
}

