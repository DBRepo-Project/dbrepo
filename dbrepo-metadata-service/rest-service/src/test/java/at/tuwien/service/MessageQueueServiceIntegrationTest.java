package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.amqp.GrantExchangePermissionsDto;
import at.tuwien.api.amqp.TopicPermissionDto;
import at.tuwien.api.amqp.VirtualHostPermissionDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostModificationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.repository.mdb.*;
import at.tuwien.service.impl.RabbitMqServiceImpl;
import at.tuwien.utils.AmqpUtils;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@MockOpensearch
public class MessageQueueServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private RabbitMqServiceImpl messageQueueService;

    @Autowired
    private AmqpUtils amqpUtils;

    @Container
    private static final RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3-management")
            .withUser(USER_1_USERNAME, USER_1_PASSWORD, Set.of("administrator"))
            .withVhost("dbrepo");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("fda.broker.endpoint", rabbitContainer::getHttpUrl);
        registry.add("spring.rabbitmq.host", rabbitContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitContainer::getAdminPassword);
    }

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setAccesses(List.of());
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void createUser_succeeds() throws BrokerRemoteException, BrokerVirtualHostModificationException {

        /* test */
        messageQueueService.createUser(USER_2_USERNAME, USER_2_PASSWORD);
    }

    @Test
    public void updatePermissions_empty_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {

        /* test */
        final VirtualHostPermissionDto permissions = setVirtualHostPermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals(".*", permissions.getRead());
        assertEquals(".*", permissions.getWrite());
    }

    @Test
    public void updatePermissions_writeAll_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {

        /* test */
        final VirtualHostPermissionDto permissions = setVirtualHostPermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals(".*", permissions.getRead());
        assertEquals(".*", permissions.getWrite());
    }

    @Test
    public void updatePermissions_writeOwn_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {

        /* test */
        final VirtualHostPermissionDto permissions = setVirtualHostPermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals(".*", permissions.getRead());
        assertEquals(".*", permissions.getWrite());
    }

    @Test
    public void updatePermissions_read_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {

        /* test */
        final VirtualHostPermissionDto permissions = setVirtualHostPermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals(".*", permissions.getRead());
        assertEquals(".*", permissions.getWrite());
    }

    @Test
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions_empty_succeeds() throws BrokerRemoteException,
            BrokerVirtualHostGrantException {

        /* test */
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of());
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals(DATABASE_1_EXCHANGE, permissions.getExchange());
        assertEquals("", permissions.getRead());
        assertEquals("", permissions.getWrite());
    }

    @Test
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions_writeAll_succeeds() throws BrokerRemoteException,
            BrokerVirtualHostGrantException {

        /* test */
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of(DATABASE_1_USER_1_WRITE_ALL_ACCESS));
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals(DATABASE_1_EXCHANGE, permissions.getExchange());
        assertEquals("^(dbrepo\\.weather\\..*)$", permissions.getRead());
        assertEquals("^(dbrepo\\.weather\\..*)$", permissions.getWrite());
    }

    @Test
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions_writeOwn_succeeds() throws BrokerRemoteException,
            BrokerVirtualHostGrantException {

        /* test */
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of(DATABASE_1_USER_1_WRITE_OWN_ACCESS));
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals(DATABASE_1_EXCHANGE, permissions.getExchange());
        assertEquals("^(dbrepo\\.weather\\..*)$", permissions.getRead());
        assertEquals("^(dbrepo\\.dbrepo\\.weather_aus|dbrepo\\.dbrepo\\.sensor)$", permissions.getWrite());
    }

    @Test
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions_read_succeeds() throws BrokerRemoteException,
            BrokerVirtualHostGrantException {

        /* test */
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of(DATABASE_1_USER_1_READ_ACCESS));
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals(DATABASE_1_EXCHANGE, permissions.getExchange());
        assertEquals("^(dbrepo\\.weather\\..*)$", permissions.getRead());
        assertEquals("", permissions.getWrite());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected VirtualHostPermissionDto setVirtualHostPermissions_generic() throws BrokerRemoteException,
            BrokerVirtualHostGrantException {

        /* mock */
        amqpUtils.setVirtualHostPermissions(REALM_DBREPO_NAME, USER_1_USERNAME, USER_1_RABBITMQ_GRANT_DTO);

        /* test */
        messageQueueService.setVirtualHostPermissions(USER_1_USERNAME);
        return amqpUtils.getVirtualHostPermissions(USER_1_USERNAME);
    }

    @Transactional(readOnly = true)
    protected TopicPermissionDto setTopicExchangePermissions_generic(List<DatabaseAccess> accesses)
            throws BrokerRemoteException, BrokerVirtualHostGrantException {
        final GrantExchangePermissionsDto request = GrantExchangePermissionsDto.builder()
                .exchange("dbrepo")
                .read("")
                .write("")
                .build();
        final User user1 = User.builder()
                .id(USER_1_ID)
                .username(USER_1_USERNAME)
                .accesses(accesses)
                .build();

        /* mock */
        amqpUtils.setVirtualHostPermissions(REALM_DBREPO_NAME, USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        amqpUtils.setTopicPermissions(REALM_DBREPO_NAME, USER_1_USERNAME, request);

        /* test */
        messageQueueService.setTopicExchangePermissions(user1);
        return amqpUtils.getTopicPermissions(USER_1_USERNAME);
    }

}
