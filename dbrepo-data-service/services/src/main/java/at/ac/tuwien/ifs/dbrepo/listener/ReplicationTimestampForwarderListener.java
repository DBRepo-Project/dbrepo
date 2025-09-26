package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReplicationTimestampForwarderListener implements MessageListener {

    private final ObjectMapper objectMapper;

    @Autowired
    public ReplicationTimestampForwarderListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
            
            // Extract source site ID from the routing key
            final String sourceSiteId = extractSourceSiteId(properties.getReceivedRoutingKey());
            if (sourceSiteId == null) {
                log.error("Could not extract source site ID from routing key: {}", properties.getReceivedRoutingKey());
                return;
            }
            
            // Process the timestamp (no forwarding to other replicas)
            log.info("Processed timestamp from forwarding queue: replicationId={}, sourceSiteId={}", 
                    dto.getReplicationId(), sourceSiteId);
            
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
