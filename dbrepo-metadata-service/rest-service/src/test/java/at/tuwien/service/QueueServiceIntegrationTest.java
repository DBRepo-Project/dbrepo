package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.amqp.RabbitMqConsumer;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.exception.AmqpException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.sdb.ViewIdxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockOpensearch
public class QueueServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private RabbitMqConsumer rabbitMqConsumer;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private AmqpConfig amqpConfig;

    @Autowired
    private RabbitMqConfig rabbitMqConfig;

    @Autowired
    private Channel channel;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageQueueService messageQueueService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

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

    @BeforeAll
    public static void beforeAll() throws SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @BeforeEach
    public void beforeEach() throws IOException, TimeoutException {
        TABLE_8.setDatabase(DATABASE_3);
        /* rabbitmq */
        final Connection connection = amqpConfig.connectionFactory().newConnection();
        this.channel = connection.createChannel();
        channel.exchangeDeclare(DATABASE_3_EXCHANGE, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(TABLE_8_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_8_QUEUE_NAME, DATABASE_3_EXCHANGE, TABLE_8_ROUTING_KEY);
    }

    @Test
    public void createConsumer_succeeds() throws AmqpException {

        /* test */
        messageQueueService.createConsumer(TABLE_8_QUEUE_NAME, DATABASE_3_ID, TABLE_8_ID);
    }

    @Test
    public void createConsumer_channelClosed_succeeds() throws AmqpException, IOException, TimeoutException {

        /* mock */
        channel.close();

        /* test */
        messageQueueService.createConsumer(TABLE_8_QUEUE_NAME, DATABASE_3_ID, TABLE_8_ID);
    }

    @Test
    public void insert_succeeds() throws IOException, AmqpException {
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .userId(USER_BROKER_USERNAME)
                .build();
        final TableCsvDto payload = TableCsvDto.builder()
                .data(new HashMap<>() {{
                    put("id", 1);
                    put("date", "2022-12-20");
                    put("location", "Vienna");
                    put("mintemp", -2.3);
                    put("rainfall", 34.3);
                }}).build();

        /* mock */
        when(databaseRepository.findByDatabaseId(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));
        when(tableRepository.find(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(Optional.of(TABLE_8));
        doThrow(IOException.class)
                .when(rabbitMqConsumer)
                .handleDelivery(anyString(), any(Envelope.class), any(AMQP.BasicProperties.class), any());
        messageQueueService.createConsumer(TABLE_8_QUEUE_NAME, DATABASE_3_ID, TABLE_8_ID);

        /* test */
        channel.basicPublish(DATABASE_3_EXCHANGE, TABLE_8_ROUTING_KEY, basicProperties, objectMapper.writeValueAsBytes(payload));
        final GetResponse response = channel.basicGet(TABLE_8_QUEUE_NAME, false);
        assertNull(response) /* queue is empty */;
    }

    @Test
    @Disabled("not reproducible")
    public void insert_noUserId_fails() throws IOException, AmqpException {
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .userId(null)
                .build();

        /* mock */
        when(databaseRepository.findByDatabaseId(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));
        when(tableRepository.find(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(Optional.of(TABLE_8));
        doThrow(IOException.class)
                .when(rabbitMqConsumer)
                .handleDelivery(anyString(), any(Envelope.class), any(AMQP.BasicProperties.class), any());
        messageQueueService.createConsumer(TABLE_8_QUEUE_NAME, DATABASE_3_ID, TABLE_8_ID);

        /* test */
        channel.basicPublish(DATABASE_3_EXCHANGE, TABLE_8_ROUTING_KEY, basicProperties, objectMapper.writeValueAsBytes(TABLE_8_CSV_DTO));
    }

    @Test
    @Disabled("not reproducible")
    public void insert_wrongUserId_fails() throws IOException, AmqpException {
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .userId(USER_2_USERNAME)
                .build();

        /* mock */
        when(databaseRepository.findByDatabaseId(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));
        when(tableRepository.find(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(Optional.of(TABLE_8));
        doThrow(IOException.class)
                .when(rabbitMqConsumer)
                .handleDelivery(anyString(), any(Envelope.class), any(AMQP.BasicProperties.class), any());
        messageQueueService.createConsumer(TABLE_8_QUEUE_NAME, DATABASE_3_ID, TABLE_8_ID);

        /* test */
        channel.basicPublish(DATABASE_3_EXCHANGE, TABLE_8_ROUTING_KEY, basicProperties, objectMapper.writeValueAsBytes(TABLE_8_CSV_DTO));
    }

    @Test
    @Disabled("Not testable")
    public void restore_succeeds() throws AmqpException, IOException {

        /* mock */
        when(tableRepository.findAll())
                .thenReturn(List.of(TABLE_1));

        /* test */
        messageQueueService.restore();
        final List<ConsumerDto> response = rabbitMqConfig.findAllConsumers();
        assertEquals(amqpConfig.getAmqpConsumers(), (int) response.stream().filter(c -> c.getQueue().getName().equals(TABLE_1_QUEUE_NAME)).count());
    }

}
