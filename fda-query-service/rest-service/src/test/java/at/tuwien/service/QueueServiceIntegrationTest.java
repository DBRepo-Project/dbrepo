package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.amqp.RabbitMqConsumer;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.AmqpException;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.TableRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static at.tuwien.config.DockerConfig.dockerClient;
import static at.tuwien.config.DockerConfig.hostConfig;
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

    @Autowired
    private AmqpConfig amqpConfig;

    @Autowired
    private Channel channel;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageQueueService messageQueueService;

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        /* create network */
        dockerClient.createNetworkCmd()
                .withName("fda-userdb")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.28.0.0/16")))
                .withEnableIpv6(false)
                .exec();
        dockerClient.createNetworkCmd()
                .withName("fda-public")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.29.0.0/16")))
                .withEnableIpv6(false)
                .exec();
        /* create container */
        final String bind = new File(
                "./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb").withBinds(Bind.parse(bind)))
                .withName(CONTAINER_1_INTERNALNAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withHealthcheck(CONTAINER_1_HEALTHCHECK)
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb",
                        "MARIADB_DATABASE=weather")
                .exec();
        final CreateContainerResponse response2 = dockerClient.createContainerCmd(IMAGE_BROKER_REPOSITORY + ":" + IMAGE_BROKER_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-public"))
                .withName(CONTAINER_BROKER_NAME)
                .withIpv4Address(CONTAINER_BROKER_IP)
                .withHostName(CONTAINER_BROKER_INTERNAL_NAME)
                .withHealthcheck(CONTAINER_BROKER_HEALTHCHECK)
                .exec();
        /* start */
        CONTAINER_1.setHash(response1.getId());
        DockerConfig.startContainer(CONTAINER_1);
        CONTAINER_BROKER.setHash(response2.getId());
        DockerConfig.startContainer(CONTAINER_BROKER);
    }

    @AfterAll
    public static void afterAll() {
        /* stop containers and remove them */
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", Arrays.asList(container.getNames()));
                    try {
                        dockerClient.stopContainerCmd(container.getId()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });
        /* remove networks */
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
    }

    @BeforeEach
    public void beforeEach() throws IOException, TimeoutException {
        TABLE_1.setDatabase(DATABASE_1);
        /* rabbitmq */
        final Connection connection = amqpConfig.connectionFactory().newConnection();
        this.channel = connection.createChannel();
        channel.exchangeDeclare(DATABASE_1_EXCHANGE, BuiltinExchangeType.DIRECT);
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

}
