package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.AmqpConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class QueueServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private AmqpConfig amqpConfig;

    private Channel channel;

    @BeforeEach
    public void beforeEach() throws IOException, TimeoutException {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpConfig.getAmpqHost());
        factory.setUsername(USER_1_USERNAME);
        factory.setPassword(USER_1_PASSWORD);
        final Connection connection = factory.newConnection();
        this.channel = connection.createChannel();
    }

    @AfterEach
    public void afterEach() throws IOException, TimeoutException {
        if (this.channel != null) {
            this.channel.close();
        }
    }

    @Test
    public void submit_succeeds() throws IOException {
        final String payload = "{\"timestamp\": \"2022-06-30 13:01:00\", \"location\": \"dummy\", \"value\": 10.0}";
        channel.basicPublish(DATABASE_1_EXCHANGE, TABLE_1_ROUTING_KEY, null, payload.getBytes());
    }

}
