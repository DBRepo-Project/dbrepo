package at.tuwien.listener;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.config.*;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.*;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class RabbitMqListenerIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private Channel channel;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private RabbitMqListenerImpl rabbitMqListener;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private H2Utils h2Utils;

    @Autowired
    private RabbitMqConfig rabbitMqConfig;

    @Autowired
    private AmqpConfig amqpConfig;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        DockerConfig.createAllNetworks();
        DockerConfig.createContainer(null, CONTAINER_BROKER, 15672, CONTAINER_BROKER_ENV);
        DockerConfig.startContainer(CONTAINER_BROKER);
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        h2Utils.runScript("schema.sql");
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setTables(List.of());
        databaseRepository.save(DATABASE_1);
        DATABASE_1.setTables(List.of(TABLE_1, TABLE_2, TABLE_3));
        tableRepository.save(TABLE_1_NOCOLS);
        tableRepository.save(TABLE_2_NOCOLS);
        tableRepository.save(TABLE_3_NOCOLS);
    }

    @Test
    public void updateConsumers_succeeds() throws AmqpException, IOException, InterruptedException {

        /* mock */
        channel.exchangeDeclare(DATABASE_1_EXCHANGE, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(TABLE_1_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_1_QUEUE_NAME, DATABASE_1_EXCHANGE, TABLE_1_ROUTING_KEY);
        channel.queueDeclare(TABLE_2_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_2_QUEUE_NAME, DATABASE_1_EXCHANGE, TABLE_2_ROUTING_KEY);
        channel.queueDeclare(TABLE_3_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_3_QUEUE_NAME, DATABASE_1_EXCHANGE, TABLE_3_ROUTING_KEY);
        when(brokerServiceGateway.findAllConsumers())
                .thenReturn(List.of());

        /* pre-condition */
        assertEquals(0, rabbitMqConfig.findAllConsumers().size());
        assertEquals(2, amqpConfig.getAmqpConsumers());

        /* test */
        rabbitMqListener.updateConsumers();
        Thread.sleep(10 * 1000);
        final List<ConsumerDto> response = rabbitMqConfig.findAllConsumers();
        assertEquals(6, response.size());
        assertEquals(2, (int) response.stream().filter(c -> c.getQueue().getName().equals(TABLE_1_QUEUE_NAME)).count());
        assertEquals(2, (int) response.stream().filter(c -> c.getQueue().getName().equals(TABLE_2_QUEUE_NAME)).count());
        assertEquals(2, (int) response.stream().filter(c -> c.getQueue().getName().equals(TABLE_3_QUEUE_NAME)).count());
    }

}
