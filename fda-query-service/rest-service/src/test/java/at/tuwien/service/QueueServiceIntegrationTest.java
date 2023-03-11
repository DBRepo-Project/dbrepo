package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.amqp.RabbitMqConsumer;
import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.*;
import at.tuwien.exception.AmqpException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.MessageQueueListener;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.TableRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueueServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private RabbitMqConsumer rabbitMqConsumer;

    /* keep */
    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    /* keep */
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

    private final static String BIND = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        DockerConfig.createAllNetworks();
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        DockerConfig.createContainer(null, CONTAINER_BROKER, 15672, CONTAINER_BROKER_ENV);
        DockerConfig.startContainer(CONTAINER_BROKER);
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() throws IOException, TimeoutException {
        TABLE_1.setDatabase(DATABASE_1);
        /* rabbitmq */
        final Connection connection = amqpConfig.connectionFactory().newConnection();
        this.channel = connection.createChannel();
        channel.exchangeDeclare(DATABASE_1_EXCHANGE, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(TABLE_1_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_1_QUEUE_NAME, DATABASE_1_EXCHANGE, TABLE_1_ROUTING_KEY);
    }

    @Test
    public void createConsumer_succeeds() throws AmqpException {

        /* test */
        messageQueueService.createConsumer(TABLE_1_QUEUE_NAME, CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);
    }

    @Test
    public void createConsumer_channelClosed_succeeds() throws AmqpException, IOException, TimeoutException {

        /* mock */
        channel.close();

        /* test */
        messageQueueService.createConsumer(TABLE_1_QUEUE_NAME, CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);
    }

    @Test
    public void insert_succeeds() throws IOException, AmqpException {
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .userId(USER_1_USERNAME)
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
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));
        doThrow(IOException.class)
                .when(rabbitMqConsumer)
                .handleDelivery(anyString(), any(Envelope.class), any(AMQP.BasicProperties.class), any());
        messageQueueService.createConsumer(TABLE_1_QUEUE_NAME, CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);

        /* test */
        channel.basicPublish(DATABASE_1_EXCHANGE, TABLE_1_ROUTING_KEY, basicProperties, objectMapper.writeValueAsBytes(payload));
        final GetResponse response = channel.basicGet(TABLE_1_QUEUE_NAME, false);
        assertNull(response) /* queue is empty */;
    }

    @Test
    public void insert_noUserId_fails() throws IOException, AmqpException {
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .userId(null)
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));
        doThrow(IOException.class)
                .when(rabbitMqConsumer)
                .handleDelivery(anyString(), any(Envelope.class), any(AMQP.BasicProperties.class), any());
        messageQueueService.createConsumer(TABLE_1_QUEUE_NAME, CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);

        /* test */
        channel.basicPublish(DATABASE_1_EXCHANGE, TABLE_1_ROUTING_KEY, basicProperties, objectMapper.writeValueAsBytes(TABLE_1_CSV_DTO));
    }

    @Test
    public void insert_wrongUserId_fails() throws IOException, AmqpException {
        final AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .userId(USER_2_USERNAME)
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));
        doThrow(IOException.class)
                .when(rabbitMqConsumer)
                .handleDelivery(anyString(), any(Envelope.class), any(AMQP.BasicProperties.class), any());
        messageQueueService.createConsumer(TABLE_1_QUEUE_NAME, CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);

        /* test */
        channel.basicPublish(DATABASE_1_EXCHANGE, TABLE_1_ROUTING_KEY, basicProperties, objectMapper.writeValueAsBytes(TABLE_1_CSV_DTO));
    }

    @Test
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
