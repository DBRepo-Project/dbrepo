package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.QueueService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationForwardingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.net.URI;

@Slf4j
@Component
public class DefaultListener implements MessageListener {

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final QueueService queueService;
    private final RabbitTemplate rabbitTemplate;
    private final ReplicationForwardingService replicationForwardingService;

    @Value("${dbrepo.replication.exchangeName:dbrepo-replication}")
    private String replicationExchangeName;

    @Value("${dbrepo.replication.siteId:}")
    private String localSiteId;

    @Autowired
    public DefaultListener(CacheService cacheService, ObjectMapper objectMapper, QueueService queueService, RabbitTemplate rabbitTemplate, ReplicationForwardingService replicationForwardingService) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.queueService = queueService;
        this.rabbitTemplate = rabbitTemplate;
        this.replicationForwardingService = replicationForwardingService;
    }

    @Override
    @Observed(name = "dbrepo_message_receive")
    @Operation(summary = "Received AMQP message from Broker Service")
    public void onMessage(Message message) {
        final MessageProperties properties = message.getMessageProperties();
        final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        if (!properties.getReceivedRoutingKey().contains(".")) {
            log.error("Failed to map database and table names from routing key: {}", properties.getReceivedRoutingKey());
            return;
        }
        final String[] parts = properties.getReceivedRoutingKey().split("\\.");
        if (parts.length != 3) {
            log.error("Failed to map database and table names from routing key: is not 3-part");
            return;
        }
        final UUID databaseId = UUID.fromString(parts[1]);
        final UUID tableId = UUID.fromString(parts[2]);
        final Map<String, Object> body;
        try {
            final DatabaseDto database = cacheService.getDatabase(databaseId);
            final TableDto table = cacheService.getTable(databaseId, tableId);
            body = objectMapper.readValue(message.getBody(), typeRef);
            log.trace("received message of {} bytes with keys: {}", message.getMessageProperties().getContentLength(), body.keySet());
            // Ensure replication_key is stable across sites: generate on publisher if column exists and missing in payload
            final boolean hasReplicationKeyColumn = table.getColumns().stream().anyMatch(c -> "replication_key".equalsIgnoreCase(c.getInternalName()));
            if (hasReplicationKeyColumn && !body.containsKey("replication_key")) {
                final String replicationKey = java.util.UUID.randomUUID().toString();
                body.put("replication_key", replicationKey);
                log.debug("generated replication_key on ingest: {}", replicationKey);
            }
            // Use insertWithTimestamps to get timestamp information for replication
            final TupleWithTimestampsDto created = queueService.insertWithTimestamps(database, table, body);

            // Fan-out to replication exchange for each replica (routing key: dbrepo.<siteId>.<remoteDatabaseId>.<remoteTableId>)
            if (database.getReplicaUrls() != null && !database.getReplicaUrls().isEmpty() && table.getReplicaUrls() != null) {
                for (var entry : database.getReplicaUrls().entrySet()) {
                    final String replicaUrl = entry.getKey();
                    final java.util.UUID remoteDatabaseId = entry.getValue();
                    final java.util.UUID remoteTableId = table.getReplicaUrls().get(replicaUrl);
                    if (remoteTableId == null) {
                        log.warn("replication: missing remoteTableId for replicaUrl={}, skipping", replicaUrl);
                        continue;
                    }
                    try {
                        final String host = new URI(replicaUrl).getHost();
                        final String siteId = host != null && host.contains(".") ? host.substring(0, host.indexOf('.')) : host;
                        final String routingKey = "dbrepo." + siteId + "." + remoteDatabaseId + "." + remoteTableId;
                        log.info("replicated message published to exchange={}, routingkey={} (to replica {})", replicationExchangeName, routingKey, replicaUrl);
                        // Log full payload and identifiers before sending to replication exchange
                        log.info("replication payload before send: id={}, replication_key={}, body={}",
                                body.get("id"), body.get("replication_key"), objectMapper.writeValueAsString(body));
                        rabbitTemplate.convertAndSend(replicationExchangeName, routingKey, body);
                        log.debug("replicated message published to exchange={}, routingkey={} (to replica {})", replicationExchangeName, routingKey, replicaUrl);
                        
                        // Send timestamp information to replication-timestamps exchange
                        if (created != null) {
                            final at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto dto = TupleReplicationTimestampDto.builder()
                                    .siteUrl(localSiteId)
                                    .replicationId(created.getReplicationKey())
                                    .databaseId(databaseId)
                                    .tableId(tableId)
                                    .rowStart(created.getInsertedAt())
                                    .rowEnd(created.getDeletedAt())
                                    .build();
                            replicationForwardingService.forwardTimestampToForwardingQueue(dto, database);
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to publish replicated message to {}: {}", replicaUrl, ex.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to read object: {}", e.getMessage());
        } catch (SQLException | RemoteUnavailableException e) {
            log.error("Failed to insert tuple: {}", e.getMessage());
        } catch (TableNotFoundException | MetadataServiceException e) {
            log.error("Failed to find table: {}", e.getMessage());
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database: {}", e.getMessage());
        }
    }
}