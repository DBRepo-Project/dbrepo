package at.tuwien.listener;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbContainerConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest
@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@Testcontainers(disabledWithoutDocker = true)
@MockOpensearch
public class DefaultListenerUnitTest extends BaseUnitTest {

    @Autowired
    private DefaultListener defaultListener;

    @Container
    private static RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3.10");

    @Test
    public void onMessage_routingKeyDatabaseAndTableMissing_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo", "{}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to map database and table"));
    }

    @Test
    public void onMessage_routingKeyTableMissing_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo.database", "{}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to map database and table"));
    }

    @Test
    public void onMessage_messageMalformed_fails(CapturedOutput output) {
        final Message request = buildMessage("dbrepo.database.table", "{,}", new HashMap<>());

        /* test */
        defaultListener.onMessage(request);
        assertTrue(output.getAll().contains("Failed to read object"));
    }

    protected Message buildMessage(String routingKey, String payload, Map<String, Object> headers) {
        final MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        properties.setHeaders(headers);
        return new Message(payload.getBytes(StandardCharsets.UTF_8), properties);
    }

}
