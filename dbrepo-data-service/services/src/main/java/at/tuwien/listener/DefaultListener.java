package at.tuwien.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
@RabbitListener(queues = "dbrepo")
public class DefaultListener implements MessageListener {

    private final ObjectMapper objectMapper;

    @Autowired
    public DefaultListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message) {
        final MessageProperties properties = message.getMessageProperties();
        final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        final Map<String, Object> body;
        try {
            body = objectMapper.readValue(message.getBody(), typeRef);
            log.debug("received message: routingKey={}, data={}", properties.getReceivedRoutingKey(), body);
        } catch (IOException e) {
            log.error("Failed to read object: {}", e.getMessage());
        }
    }
}
