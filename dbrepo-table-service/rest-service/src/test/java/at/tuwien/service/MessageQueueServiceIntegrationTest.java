
package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.config.DockerConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.utils.AmqpUtils;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MessageQueueServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private TableIdxRepository tableidxRepository;

    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @MockBean
    private TableRepository tableRepository;

    @Autowired
    private AmqpUtils amqpUtils;

    @Autowired
    private Channel channel;

    @Autowired
    private MessageQueueService messageQueueService;

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        /* create network */
        DockerConfig.createAllNetworks();
        /* create container */
        DockerConfig.createContainer(null, CONTAINER_BROKER, 15672, CONTAINER_BROKER_ENV);
        DockerConfig.startContainer(CONTAINER_BROKER);
    }

    @AfterAll
    public static void afterAll() {
        /* stop containers and remove them */
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
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
