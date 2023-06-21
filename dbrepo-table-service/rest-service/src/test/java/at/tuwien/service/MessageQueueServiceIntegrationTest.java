
package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.exception.AmqpException;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import at.tuwien.utils.AmqpUtils;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MessageQueueServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private TableIdxRepository tableidxRepository;

    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @MockBean
    private at.tuwien.repository.mdb.TableRepository tableRepository;

    @Autowired
    private AmqpUtils amqpUtils;

    @Autowired
    private Channel channel;

    @Autowired
    private MessageQueueService messageQueueService;

    @Container
    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management-alpine")
            .withVhost("/");

    @DynamicPropertySource
    static void rabbitMQProperties(DynamicPropertyRegistry registry) {
        registry.add("fda.gateway.endpoint", () -> "http://" + rabbitMQContainer.getHost() + ":" + rabbitMQContainer.getHttpPort());
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    }

    @Test
    public void init_succeeds() throws AmqpException, IOException {

        /* mock */
        channel.exchangeDeclare(DATABASE_1_EXCHANGE, BuiltinExchangeType.FANOUT, true, false, null);
        when(tableRepository.findAll())
                .thenReturn(List.of(TABLE_1, TABLE_2));

        /* test */
        assertTrue(amqpUtils.exchangeExists(DATABASE_1_EXCHANGE));
        messageQueueService.init();
        assertTrue(amqpUtils.queueExists(TABLE_1_QUEUE_NAME));
    }

}
