package at.tuwien.listener;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.exception.MetadataServiceException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.CredentialService;
import at.tuwien.service.QueueService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
@Component
public class DefaultListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final QueueService queueService;
    private final CredentialService credentialService;

    @Autowired
    public DefaultListener(ObjectMapper objectMapper, QueueService queueService, CredentialService credentialService) {
        this.objectMapper = objectMapper;
        this.queueService = queueService;
        this.credentialService = credentialService;
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
        log.trace("received message for table with id {} of database id {}: {} bytes", tableId, databaseId, message.getMessageProperties().getContentLength());
        final Map<String, Object> body;
        try {
            final TableDto table = credentialService.getTable(databaseId, tableId);
            body = objectMapper.readValue(message.getBody(), typeRef);
            queueService.insert(table, body);
        } catch (IOException e) {
            log.error("Failed to read object: {}", e.getMessage());
        } catch (SQLException | RemoteUnavailableException e) {
            log.error("Failed to insert tuple: {}", e.getMessage());
        } catch (TableNotFoundException | MetadataServiceException e) {
            log.error("Failed to find table: {}", e.getMessage());
        }
    }
}
