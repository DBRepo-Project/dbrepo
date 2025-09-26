package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
public class ReplicationMasterTimestampListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final TableService tableService;
    private final CacheService cacheService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${dbrepo.replication.timestampForwarding.exchangeName:dbrepo-replication-timestamp-forwarding}")
    private String replicationTimestampForwardingExchangeName;

    @Value("${dbrepo.replication.siteId:}")
    private String localSiteId;

    @Autowired
    public ReplicationMasterTimestampListener(ObjectMapper objectMapper, TableService tableService, CacheService cacheService, RabbitTemplate rabbitTemplate) {
        this.objectMapper = objectMapper;
        this.tableService = tableService;
        this.cacheService = cacheService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Observed(name = "dbrepo_replication_timestamp_receive")
    @Operation(summary = "Received replication timestamp message")
    public void onMessage(Message message) {
        final MessageProperties properties = message.getMessageProperties();
        try {
            final TupleReplicationTimestampDto dto = objectMapper.readValue(message.getBody(), TupleReplicationTimestampDto.class);
            log.info("replication-timestamps: received message routingKey={}, replicationId={}, siteUrl={}, dbId={}, tableId={}, rowStart={}, rowEnd={}",
                    properties.getReceivedRoutingKey(),
                    dto.getReplicationId(),
                    dto.getSiteUrl(),
                    dto.getDatabaseId(),
                    dto.getTableId(),
                    dto.getRowStart(),
                    dto.getRowEnd());
            
            // Get database and table from cache service (which handles remote->local mapping)
            final DatabaseDto database = cacheService.getLocalDatabaseByRemoteDatabaseId(dto.getDatabaseId());
            final TableDto table = cacheService.getLocalTableByRemoteTableId(database.getId(), dto.getTableId());
            
            log.info("Retrieved database={} and table={} from metadata service", 
                    database.getInternalName(), table.getInternalName());
            
            // Process and persist timestamps using the service (same as TableEndpoint)
            List<TupleReplicationTimestampDto> timestamps = List.of(dto);
            tableService.processReplicationTimestamps(database, table, timestamps);
            
            log.info("Successfully processed replication timestamp for databaseId={}, tableId={}, replicationId={}", 
                    dto.getDatabaseId(), dto.getTableId(), dto.getReplicationId());
            
            // Forward timestamp to other replicas via forwarding queue
            forwardTimestampToForwardingQueue(dto, database, properties.getReceivedRoutingKey());
            
        } catch (DatabaseNotFoundException e) {
            log.error("Database not found for replication timestamp databaseId={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (TableNotFoundException e) {
            log.error("Table not found for replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (RemoteUnavailableException e) {
            log.error("Metadata service unavailable for replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (MetadataServiceException e) {
            log.error("Metadata service error for replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (SQLException e) {
            log.error("SQL error processing replication timestamp routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        } catch (Exception e) {
            log.error("replication-timestamps: failed to process message routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        }
    }
    
    /**
     * Forward timestamp to forwarding queue for each replica
     */
    private void forwardTimestampToForwardingQueue(TupleReplicationTimestampDto dto, DatabaseDto database, String originalRoutingKey) {
        try {
            // Extract source site ID from original routing key
            final String sourceSiteId = extractSourceSiteId(originalRoutingKey);
            if (sourceSiteId == null) {
                log.warn("Could not extract source site ID from routing key: {}, skipping forwarding", originalRoutingKey);
                return;
            }
            
            if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
                log.debug("No replica URLs configured for database {}, skipping timestamp forwarding", database.getInternalName());
                return;
            }
            
            int forwardedCount = 0;
            for (var entry : database.getReplicaUrls().entrySet()) {
                final String replicaUrl = entry.getKey();
                final String replicaSiteId = extractSiteIdFromUrl(replicaUrl);
                
                // Skip if this is the source replica or local replica
                if (replicaSiteId.equals(sourceSiteId) || replicaSiteId.equals(localSiteId)) {
                    log.debug("Skipping timestamp forwarding to source site {} or local site {}", sourceSiteId, localSiteId);
                    continue;
                }
                
                try {
                    // Create forwarding routing key: dbrepo.timestamp-forwarding.{replicaSiteId}.{databaseId}.{tableId}
                    final String forwardingRoutingKey = "dbrepo.timestamp-forwarding." + replicaSiteId + "." + dto.getDatabaseId() + "." + dto.getTableId();
                    
                    // Send to forwarding exchange
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
    
    /**
     * Extract source site ID from routing key format: dbrepo.{siteId}.{databaseId}.{tableId}
     */
    private String extractSourceSiteId(String routingKey) {
        if (routingKey == null || !routingKey.contains(".")) {
            return null;
        }
        final String[] parts = routingKey.split("\\.");
        if (parts.length >= 3) {
            return parts[1]; // siteId is the 2nd part
        }
        return null;
    }
    
    /**
     * Extract site ID from replica URL format: https://<key>.datalab.tuwien.ac.at
     */
    private String extractSiteIdFromUrl(String replicaUrl) {
        if (replicaUrl == null || replicaUrl.isEmpty()) {
            return "unknown";
        }
        
        try {
            // Extract the key from URL format: https://<key>.datalab.tuwien.ac.at
            if (replicaUrl.contains("://") && replicaUrl.contains(".datalab.tuwien.ac.at")) {
                String hostname = replicaUrl.substring(replicaUrl.indexOf("://") + 3);
                if (hostname.contains(".")) {
                    return hostname.substring(0, hostname.indexOf("."));
                }
            }
            
            // Fallback: use the URL as-is, sanitized
            return replicaUrl.replaceAll("[^a-zA-Z0-9]", "_");
        } catch (Exception e) {
            log.warn("Could not extract site ID from URL: {}, using 'unknown'", replicaUrl);
            return "unknown";
        }
    }
    
}


