package at.ac.tuwien.ifs.dbrepo.util;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RabbitMqUtils {

    public static Message buildMessage(String routingKey, String payload, Map<String, Object> headers) {
        final MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        properties.setHeaders(headers);
        return new Message(payload.getBytes(StandardCharsets.UTF_8), properties);
    }
}
