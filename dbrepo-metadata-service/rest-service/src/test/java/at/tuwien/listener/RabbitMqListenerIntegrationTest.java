package at.tuwien.listener;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.ViewIdxRepository;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Log4j2
@ActiveProfiles(profiles = "junit")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockOpensearch
public class RabbitMqListenerIntegrationTest extends BaseUnitTest {

    @Autowired
    private Channel channel;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private RabbitMqConfig rabbitMqConfig;

    @Autowired
    private AmqpConfig amqpConfig;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(300);

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

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        imageRepository.save(IMAGE_1_SIMPLE);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        tableRepository.save(TABLE_1_SIMPLE);
        tableRepository.save(TABLE_2_SIMPLE);
        tableRepository.save(TABLE_3_SIMPLE);
    }

    @Test
    @Disabled("Not testable")
    public void updateConsumers_succeeds() throws IOException, InterruptedException {

        /* pre-condition */
        assertEquals(0, getConsumers().size());
        assertEquals(2, amqpConfig.getAmqpConsumers());

        /* mock */
        channel.exchangeDeclare(DATABASE_1_EXCHANGE, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(TABLE_1_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_1_QUEUE_NAME, DATABASE_1_EXCHANGE, TABLE_1_ROUTING_KEY);
        channel.queueDeclare(TABLE_2_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_2_QUEUE_NAME, DATABASE_1_EXCHANGE, TABLE_2_ROUTING_KEY);
        channel.queueDeclare(TABLE_3_QUEUE_NAME, true, false, false, null);
        channel.queueBind(TABLE_3_QUEUE_NAME, DATABASE_1_EXCHANGE, TABLE_3_ROUTING_KEY);

        /* test */
        Thread.sleep(30 * 1000) /* wait for scheduled insert */;
        final List<ConsumerDto> response = getConsumers();
        final List<ConsumerDto> consumers1 = response.stream().filter(c -> c.getQueue().getName().equals(TABLE_1_QUEUE_NAME)).collect(Collectors.toList());
        assertEquals(2, consumers1.size());
        final List<ConsumerDto> consumers2 = response.stream().filter(c -> c.getQueue().getName().equals(TABLE_2_QUEUE_NAME)).collect(Collectors.toList());
        assertEquals(2, consumers2.size());
        final List<ConsumerDto> consumers3 = response.stream().filter(c -> c.getQueue().getName().equals(TABLE_3_QUEUE_NAME)).collect(Collectors.toList());
        assertEquals(2, consumers3.size());
    }

    private List<ConsumerDto> getConsumers() throws IOException {
        return rabbitMqConfig.findAllConsumers()
                .stream()
                .filter(c -> List.of(TABLE_1_QUEUE_NAME, TABLE_2_QUEUE_NAME, TABLE_3_QUEUE_NAME).contains(c.getQueue().getName()))
                .collect(Collectors.toList());
    }

}
