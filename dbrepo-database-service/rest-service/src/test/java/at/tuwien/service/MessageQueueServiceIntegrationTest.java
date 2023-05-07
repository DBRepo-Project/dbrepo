package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.AmqpException;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.service.impl.RabbitMqServiceImpl;
import at.tuwien.utils.AmqpUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
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
    private IndexConfig indexConfig;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private Channel channel;

    @Autowired
    private RabbitMqServiceImpl messageQueueService;

    @Autowired
    private AmqpUtils amqpUtils;

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        DockerConfig.createAllNetworks();
        /* create amqp */
        DockerConfig.createContainer(null, CONTAINER_BROKER, CONTAINER_BROKER_ENV);
        DockerConfig.startContainer(CONTAINER_BROKER);
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void createExchange_succeeds() throws AmqpException {

        /* test */
        messageQueueService.createExchange(DATABASE_1, USER_1_PRINCIPAL);
        assertTrue(amqpUtils.exchangeExists(DATABASE_1_EXCHANGE));
    }

    @Test
    public void deleteExchange_succeeds() throws AmqpException {

        /* test */
        messageQueueService.deleteExchange(DATABASE_1);
        assertFalse(amqpUtils.exchangeExists(DATABASE_1_EXCHANGE));
    }

    @Test
    public void createUser_succeeds() throws BrokerVirtualHostCreationException {

        /* test */
        messageQueueService.createUser(USER_1);
    }

    @Test
    public void updatePermissions_succeeds() throws BrokerVirtualHostGrantException {

        /* test */
        messageQueueService.updatePermissions(USER_1_PRINCIPAL);
    }

    @Test
    public void init_succeeds() throws AmqpException {

        /* mock */
        when(databaseRepository.findAll())
                .thenReturn(List.of(DATABASE_1));

        /* test */
        assertFalse(amqpUtils.exchangeExists(DATABASE_1_EXCHANGE));
        messageQueueService.init();
        assertTrue(amqpUtils.exchangeExists(DATABASE_1_EXCHANGE));
    }

}
