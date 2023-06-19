package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.PermissionDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.AmqpException;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.impl.RabbitMqServiceImpl;
import at.tuwien.utils.AmqpUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MessageQueueServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private Channel channel;

    @Autowired
    private RabbitMqServiceImpl messageQueueService;

    @Autowired
    private AmqpUtils amqpUtils;

    @Container
    private static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management-alpine")
            .withUser("fda", "fda", Set.of("administrator"))
            .withVhost("dbrepo");

    @DynamicPropertySource
    static void rabbitMQProperties(DynamicPropertyRegistry registry) {
        registry.add("fda.gateway.endpoint", () -> "http://172.17.0.3:15672");
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
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
    public void updatePermissions_empty_succeeds() throws BrokerVirtualHostGrantException {

        /* test */
        final PermissionDto permissions = updatePermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals("", permissions.getRead());
        assertEquals("", permissions.getWrite());
    }

    @Test
    public void updatePermissions_owner_succeeds() throws BrokerVirtualHostGrantException {

        /* mock */
        when(databaseRepository.findConfigureAccess(USER_1_ID))
                .thenReturn(List.of(DATABASE_1));
        when(databaseRepository.findWriteAccess(USER_1_ID))
                .thenReturn(List.of(DATABASE_1));
        when(databaseRepository.findReadAccess(USER_1_ID))
                .thenReturn(List.of(DATABASE_1));

        /* test */
        final PermissionDto permissions = updatePermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("^(" + DATABASE_1_EXCHANGE + ")$", permissions.getConfigure());
        assertEquals("^(" + DATABASE_1_EXCHANGE + ")$", permissions.getRead());
        assertEquals("^(" + DATABASE_1_EXCHANGE + ")$", permissions.getWrite());
    }

    @Test
    public void updatePermissions_ownerNoAccess_succeeds() throws BrokerVirtualHostGrantException {

        /* mock */
        when(databaseRepository.findConfigureAccess(USER_1_ID))
                .thenReturn(List.of(DATABASE_1));
        when(databaseRepository.findWriteAccess(USER_1_ID))
                .thenReturn(List.of());
        when(databaseRepository.findReadAccess(USER_1_ID))
                .thenReturn(List.of());

        /* test */
        final PermissionDto permissions = updatePermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("^(" + DATABASE_1_EXCHANGE + ")$", permissions.getConfigure());
        assertEquals("", permissions.getRead());
        assertEquals("", permissions.getWrite());
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

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected PermissionDto updatePermissions_generic() throws BrokerVirtualHostGrantException {

        /* mock */
        amqpUtils.createUser(USER_1_USERNAME, USER_1_RABBITMQ_CREATE_DTO);
        amqpUtils.setPermissions("http://172.17.0.3:15672", REALM_DBREPO_NAME, USER_1_USERNAME, USER_1_RABBITMQ_GRANT_DTO);

        /* test */
        messageQueueService.updatePermissions(USER_1);
        return amqpUtils.getPermissions(USER_1_USERNAME);
    }

}
