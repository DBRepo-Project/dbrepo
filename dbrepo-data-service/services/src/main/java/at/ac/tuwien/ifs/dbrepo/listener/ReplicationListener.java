package at.ac.tuwien.ifs.dbrepo.listener;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.QueueService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ReplicationListener implements MessageListener {

    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final QueueService queueService;

    @Autowired
    public ReplicationListener(CacheService cacheService, ObjectMapper objectMapper, QueueService queueService) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.queueService = queueService;
    }

    @Override
    @Observed(name = "dbrepo_replicated_message_receive")
    @Operation(summary = "Received replicated AMQP message from Federation")
    public void onMessage(Message message) {
        final MessageProperties properties = message.getMessageProperties();
        final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() { };
        if (!properties.getReceivedRoutingKey().contains(".")) {
            log.error("Failed to map database and table names from routing key: {}", properties.getReceivedRoutingKey());
            return;
        }
        final String[] parts = properties.getReceivedRoutingKey().split("\\.");
        // Require new 4-part (dbrepo.<siteId>.<dbId>.<tableId>) keys
        if (parts.length != 4) {
            log.error("Invalid routing key format (expected 4 parts: dbrepo.<siteId>.<dbId>.<tableId>): {}", properties.getReceivedRoutingKey());
            return;
        }
        final String siteId = parts[1];
        final UUID databaseId = UUID.fromString(parts[2]);
        final UUID tableId = UUID.fromString(parts[3]);
        log.debug("replication: routing key parts parsed (site={}, db={}, table={})", siteId, databaseId, tableId);
        final Map<String, Object> body;
        log.info("received replicated message from routing key: {}", properties.getReceivedRoutingKey());
        try {
            final DatabaseDto database = cacheService.getDatabase(databaseId);
            final TableDto table = cacheService.getTable(databaseId, tableId);
            body = objectMapper.readValue(message.getBody(), typeRef);
            final Object rk = body.get("replication_key");
            final Object id = body.get("id");
            log.info("received replicated message of {} bytes with keys: {} (site={}, db={}, table={}, id={}, replication_key={})",
                    message.getMessageProperties().getContentLength(), body.keySet(), siteId, databaseId, tableId, id, rk);
            // Do NOT generate replication_key here; it must be preserved across sites
            final boolean hasReplicationKeyColumn = table.getColumns().stream().anyMatch(c -> "replication_key".equalsIgnoreCase(c.getInternalName()));
            if (hasReplicationKeyColumn && !body.containsKey("replication_key")) {
                log.error("replication_key missing in replicated payload for table {}.{}; skipping insert to preserve consistency. bodyKeys={}, routingKey={}",
                        database.getInternalName(), table.getInternalName(), body.keySet(), properties.getReceivedRoutingKey());
                return;
            }
            log.debug("replication: inserting tuple into {}.{} (site={}, id={}, replication_key={})",
                    database.getInternalName(), table.getInternalName(), siteId, id, rk);
            queueService.insert(database, table, body);
            log.info("replication: insert success into {}.{} (site={}, id={}, replication_key={})",
                    database.getInternalName(), table.getInternalName(), siteId, id, rk);
        } catch (IOException e) {
            log.error("Failed to read replicated object (routingKey={}): {}", properties.getReceivedRoutingKey(), e.getMessage());
        } catch (SQLException | RemoteUnavailableException e) {
            log.error("Failed to insert replicated tuple (routingKey={}, site={}, db={}, table={}): {}",
                    properties.getReceivedRoutingKey(), siteId, databaseId, tableId, e.getMessage());
        } catch (TableNotFoundException | MetadataServiceException e) {
            log.error("Failed to find replicated table (routingKey={}, db={}, table={}): {}",
                    properties.getReceivedRoutingKey(), databaseId, tableId, e.getMessage());
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find replicated database (routingKey={}, db={}): {}",
                    properties.getReceivedRoutingKey(), databaseId, e.getMessage());
        }
    }
}


