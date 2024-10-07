package at.tuwien.service;

import at.tuwien.api.amqp.GrantExchangePermissionsDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.amqp.TopicPermissionDto;
import at.tuwien.api.amqp.VirtualHostPermissionDto;
import at.tuwien.config.RabbitConfig;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerServiceConnectionException;
import at.tuwien.exception.BrokerServiceException;
import at.tuwien.test.AbstractUnitTest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BrokerServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private RabbitConfig rabbitConfig;

    @Autowired
    private BrokerService brokerService;

    @Container
    private static final RabbitMQContainer rabbitContainer = new RabbitMQContainer("rabbitmq:3-management")
            .withUser(USER_1_USERNAME, USER_1_PASSWORD, Set.of("administrator"))
            .withVhost("dbrepo");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.endpoints.brokerService", rabbitContainer::getHttpUrl);
    }

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void updatePermissions_empty_succeeds() throws BrokerServiceException, BrokerServiceConnectionException {

        /* test */
        final VirtualHostPermissionDto permissions = setVirtualHostPermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals(".*", permissions.getRead());
        assertEquals(".*", permissions.getWrite());
    }

    @Test
    public void updatePermissions_writeAll_succeeds() throws BrokerServiceException, BrokerServiceConnectionException {

        /* test */
        final VirtualHostPermissionDto permissions = setVirtualHostPermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals(".*", permissions.getRead());
        assertEquals(".*", permissions.getWrite());
    }

    @Test
    public void updatePermissions_writeOwn_succeeds() throws BrokerServiceException, BrokerServiceConnectionException {

        /* test */
        final VirtualHostPermissionDto permissions = setVirtualHostPermissions_generic();
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals("", permissions.getConfigure());
        assertEquals(".*", permissions.getRead());
        assertEquals(".*", permissions.getWrite());
    }

    @Test
    public void updatePermissions_read_succeeds() throws BrokerServiceException, BrokerServiceConnectionException {

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
    public void setTopicExchangePermissions_empty_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {

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
    public void setTopicExchangePermissions_writeAll_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {

        /* test */
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of(DATABASE_1_USER_1_WRITE_ALL_ACCESS));
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals(DATABASE_1_EXCHANGE, permissions.getExchange());
        assertEquals("^(dbrepo\\." + DATABASE_1_ID + "\\..*)$", permissions.getRead());
        assertEquals("^(dbrepo\\." + DATABASE_1_ID + "\\..*)$", permissions.getWrite());
    }

    @Test
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions_writeOwn_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {

        /* test */
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of(DATABASE_1_USER_1_WRITE_OWN_ACCESS));
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals(DATABASE_1_EXCHANGE, permissions.getExchange());
        assertEquals("^(dbrepo\\." + DATABASE_1_ID + "\\..*)$", permissions.getRead());
        assertEquals("^(dbrepo\\." + DATABASE_1_ID + "\\." + TABLE_1_ID + "|dbrepo\\." + DATABASE_1_ID + "\\." + TABLE_4_ID + ")$", permissions.getWrite());
    }

    @Test
    @Transactional(readOnly = true)
    public void setTopicExchangePermissions_read_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {

        /* test */
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of(DATABASE_1_USER_1_READ_ACCESS));
        assertEquals(USER_1_USERNAME, permissions.getUser());
        assertEquals(REALM_DBREPO_NAME, permissions.getVhost());
        assertEquals(DATABASE_1_EXCHANGE, permissions.getExchange());
        assertEquals("^(dbrepo\\." + DATABASE_1_ID + "\\..*)$", permissions.getRead());
        assertEquals("", permissions.getWrite());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected VirtualHostPermissionDto setVirtualHostPermissions_generic() throws BrokerServiceException,
            BrokerServiceConnectionException {
        final GrantVirtualHostPermissionsDto permissions = GrantVirtualHostPermissionsDto.builder()
                .configure("")
                .read("")
                .write("")
                .build();
        final AmqpUtils amqpUtils = new AmqpUtils(rabbitContainer.getHttpUrl());

        /* mock */
        amqpUtils.setVirtualHostPermissions(REALM_DBREPO_NAME, USER_1_USERNAME, permissions);

        /* test */
        brokerService.setVirtualHostPermissions(USER_1);
        return amqpUtils.getVirtualHostPermissions(USER_1_USERNAME);
    }

    @Transactional(readOnly = true)
    protected TopicPermissionDto setTopicExchangePermissions_generic(List<DatabaseAccess> accesses)
            throws BrokerServiceException, BrokerServiceConnectionException {
        final AmqpUtils amqpUtils = new AmqpUtils(rabbitContainer.getHttpUrl());
        final GrantExchangePermissionsDto request = GrantExchangePermissionsDto.builder()
                .exchange(rabbitConfig.getExchangeName())
                .read("")
                .write("")
                .build();
        final User user = User.builder()
                .id(USER_1_ID)
                .username(USER_1_USERNAME)
                .accesses(accesses)
                .build();

        /* mock */
        amqpUtils.setVirtualHostPermissions(REALM_DBREPO_NAME, USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        amqpUtils.setTopicPermissions(REALM_DBREPO_NAME, USER_1_USERNAME, request);

        /* test */
        brokerService.setTopicExchangePermissions(user);
        return amqpUtils.getTopicPermissions(USER_1_USERNAME);
    }

}
