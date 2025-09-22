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
        // Support legacy 3-part (dbrepo.<dbId>.<tableId>) and new 4-part (dbrepo.<siteId>.<dbId>.<tableId>) keys
        final UUID databaseId;
        final UUID tableId;
        if (parts.length == 3) {
            databaseId = UUID.fromString(parts[1]);
            tableId = UUID.fromString(parts[2]);
        } else if (parts.length == 4) {
            databaseId = UUID.fromString(parts[2]);
            tableId = UUID.fromString(parts[3]);
        } else {
            log.error("Failed to map database and table names from routing key: unexpected parts: {}", parts.length);
            return;
        }
        final Map<String, Object> body;
        log.info("received replicated message from routing key: {}", properties.getReceivedRoutingKey());
        try {
            final DatabaseDto database = cacheService.getDatabase(databaseId);
            final TableDto table = cacheService.getTable(databaseId, tableId);
            body = objectMapper.readValue(message.getBody(), typeRef);
            log.trace("received replicated message of {} bytes with keys: {}", message.getMessageProperties().getContentLength(), body.keySet());
            // Do NOT generate replication_key here; it must be preserved across sites
            final boolean hasReplicationKeyColumn = table.getColumns().stream().anyMatch(c -> "replication_key".equalsIgnoreCase(c.getInternalName()));
            if (hasReplicationKeyColumn && !body.containsKey("replication_key")) {
                log.error("replication_key missing in replicated payload for table {}.{}; skipping insert to preserve consistency", database.getInternalName(), table.getInternalName());
                return;
            }
            queueService.insert(database, table, body);
        } catch (IOException e) {
            log.error("Failed to read object: {}", e.getMessage());
        } catch (SQLException | RemoteUnavailableException e) {
            log.error("Failed to insert replicated tuple: {}", e.getMessage());
        } catch (TableNotFoundException | MetadataServiceException e) {
            log.error("Failed to find replicated table: {}", e.getMessage());
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find replicated database: {}", e.getMessage());
        }
    }
}


