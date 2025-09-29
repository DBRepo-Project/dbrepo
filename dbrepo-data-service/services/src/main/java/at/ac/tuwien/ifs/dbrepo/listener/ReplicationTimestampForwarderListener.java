package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationTimestampService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Slf4j
@Component
public class ReplicationTimestampForwarderListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final CacheService cacheService;
    private final ReplicationTimestampService replicationTimestampService;

    @Autowired
    public ReplicationTimestampForwarderListener(ObjectMapper objectMapper, CacheService cacheService, ReplicationTimestampService replicationTimestampService) {
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.replicationTimestampService = replicationTimestampService;
    }

    @Override
    @Observed(name = "dbrepo_replication_timestamp_forwarding")
    @Operation(summary = "Process replication timestamp from forwarding queue")
    public void onMessage(Message message) {
        final MessageProperties properties = message.getMessageProperties();
        try {
            final TupleReplicationTimestampDto dto = objectMapper.readValue(message.getBody(), TupleReplicationTimestampDto.class);
            log.info("timestamp-forwarding: received message routingKey={}, replicationId={}, siteUrl={}, dbId={}, tableId={}",
                    properties.getReceivedRoutingKey(),
                    dto.getReplicationId(),
                    dto.getSiteUrl(),
                    dto.getDatabaseId(),
                    dto.getTableId());
            
            // Extract source site ID from the routing key (dbrepo.timestamp-forwarding.{sourceSiteId}.{dbId}.{tableId})
            final String sourceSiteId = extractSourceSiteId(properties.getReceivedRoutingKey());
            if (sourceSiteId == null) {
                log.error("Could not extract source site ID from routing key: {}", properties.getReceivedRoutingKey());
                return;
            }

            // Persist the timestamp into the local database's tuple_replication_timestamps table
            // Note: In the forwarding queue, the databaseId is already the LOCAL database id
            final DatabaseDto database = cacheService.getDatabase(dto.getDatabaseId());
            // Ensure the target table exists (idempotent)
            replicationTimestampService.ensureTableExists(database);
            final TupleReplicationTimestamp timestamp = TupleReplicationTimestamp.builder()
                    .siteUrl(dto.getSiteUrl())
                    .replicationId(dto.getReplicationId())
                    .databaseId(dto.getDatabaseId())
                    .tableId(dto.getTableId())
                    .rowStart(dto.getRowStart() != null ? Timestamp.from(dto.getRowStart()) : null)
                    .rowEnd(dto.getRowEnd() != null ? Timestamp.from(dto.getRowEnd()) : null)
                    .build();

            replicationTimestampService.saveReplicationTimestamp(database, timestamp);

            log.info("Persisted forwarded timestamp: replicationId={}, sourceSiteId={}, siteUrl={}",
                    dto.getReplicationId(), sourceSiteId, dto.getSiteUrl());
            
        } catch (Exception e) {
            log.error("timestamp-forwarding: failed to process message routingKey={}: {}", 
                    properties.getReceivedRoutingKey(), e.getMessage());
        }
    }
    
    /**
     * Extract source site ID from routing key format: dbrepo.timestamp-forwarding.{sourceSiteId}.{databaseId}.{tableId}
     */
    private String extractSourceSiteId(String routingKey) {
        if (routingKey == null || !routingKey.contains(".")) {
            return null;
        }
        final String[] parts = routingKey.split("\\.");
        if (parts.length >= 3) {
            return parts[2]; // sourceSiteId is the 3rd part
        }
        return null;
    }
    
}
