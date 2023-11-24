package at.tuwien.listener;

import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.QueueService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
@RabbitListener(queues = "dbrepo")
public class DefaultListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final QueueService queueService;

    @Autowired
    public DefaultListener(ObjectMapper objectMapper, QueueService queueService) {
        this.objectMapper = objectMapper;
        this.queueService = queueService;
    }

    @Override
    @Observed(name = "dbr_message_receive")
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
        log.trace("received message with id {} and content length: {} bytes", message.getMessageProperties().getMessageId(), message.getMessageProperties().getContentLength());
        final String database = parts[1];
        final String table = parts[2];
        final Map<String, Object> body;
        try {
            body = objectMapper.readValue(message.getBody(), typeRef);
            queueService.insert(database, table, body);
        } catch (IOException e) {
            log.error("Failed to read object: {}", e.getMessage());
        } catch (TableNotFoundException | QueryMalformedException | DatabaseNotFoundException | SQLException e) {
            log.error("Failed to insert tuple: {}", e.getMessage());
        }
    }
}
