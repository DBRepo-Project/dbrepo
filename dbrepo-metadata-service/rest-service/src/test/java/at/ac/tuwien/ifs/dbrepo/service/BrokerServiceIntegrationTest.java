package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.GrantExchangePermissionsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.GrantVirtualHostPermissionsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.TopicPermissionDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.VirtualHostPermissionDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.AmqpUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BrokerServiceIntegrationTest extends BaseTest {

    @Autowired
    private RabbitConfig rabbitConfig;

    @Autowired
    private BrokerService brokerService;

    @Autowired
    @Qualifier("brokerRestTemplate")
    private RestTemplate restTemplate;

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
        restTemplate.setInterceptors(List.of(new BasicAuthenticationInterceptor(rabbitContainer.getAdminUsername(),
                rabbitContainer.getAdminPassword())));
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
        final TopicPermissionDto permissions = setTopicExchangePermissions_generic(List.of(DATABASE_1.getAccesses().get(0)));
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

        /* mock */
        AmqpUtils.setVirtualHostPermissions(restTemplate, REALM_DBREPO_NAME, USER_1_USERNAME, permissions);

        /* test */
        brokerService.setVirtualHostPermissions(USER_1_USERNAME);
        return AmqpUtils.getVirtualHostPermissions(restTemplate, USER_1_USERNAME);
    }

    @Transactional(readOnly = true)
    protected TopicPermissionDto setTopicExchangePermissions_generic(List<DatabaseAccess> accesses)
            throws BrokerServiceException, BrokerServiceConnectionException {
        final GrantExchangePermissionsDto request = GrantExchangePermissionsDto.builder()
                .exchange(rabbitConfig.getExchangeName())
                .read("")
                .write("")
                .build();

        /* mock */
        AmqpUtils.setVirtualHostPermissions(restTemplate, REALM_DBREPO_NAME, USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        AmqpUtils.setTopicPermissions(restTemplate, REALM_DBREPO_NAME, USER_1_USERNAME, request);

        /* test */
        brokerService.setTopicExchangePermissions(USER_1_USERNAME, accesses);
        return AmqpUtils.getTopicPermissions(restTemplate, USER_1_USERNAME);
    }

}
