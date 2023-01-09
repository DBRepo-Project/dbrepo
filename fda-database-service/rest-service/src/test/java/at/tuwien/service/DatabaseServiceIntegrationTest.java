package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.DatabaseidxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.impl.MariaDbServiceImpl;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Optional;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DatabaseServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private DatabaseidxRepository databaseidxRepository;

    @MockBean
    private Channel channel;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @BeforeEach
    public void beforeEach() throws InterruptedException {
        afterEach();
        /* create networks */
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
        /* create containers */
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                .withName(CONTAINER_1_NAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withEnv(IMAGE_1_ENV)
                .exec();
        CONTAINER_1.setHash(response1.getId());
        final CreateContainerResponse response2 = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                .withName(CONTAINER_2_NAME)
                .withIpv4Address(CONTAINER_2_IP)
                .withHostName(CONTAINER_2_INTERNALNAME)
                .withEnv(IMAGE_1_ENV)
                .exec();
        CONTAINER_2.setHash(response2.getId());
        /* start containers */
        startContainer(CONTAINER_1);
        startContainer(CONTAINER_2);
    }

    @AfterEach
    public void afterEach() {
        /* stop containers and remove them */
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", container.getNames()[0]);
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

    @Test
    public void create_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException, AmqpException,
            ContainerNotFoundException, ContainerConnectionException, DatabaseMalformedException {

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_CREATE, CONTAINER_1, DATABASE_1);
    }

    @Test
    public void create_inSequence_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException, AmqpException,
            ContainerNotFoundException, ContainerConnectionException, DatabaseMalformedException {

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_CREATE, CONTAINER_1, DATABASE_1);
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, CONTAINER_2, DATABASE_2);
    }

    @Test
    public void create_outOfSequence_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException, AmqpException,
            ContainerNotFoundException, ContainerConnectionException, DatabaseMalformedException {

        /* test */
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, CONTAINER_2, DATABASE_2);
        generic_create(CONTAINER_1_ID, DATABASE_1_CREATE, CONTAINER_1, DATABASE_1);
    }

    @Test
    public void create_queryStore_succeeds() throws SQLException, InterruptedException {

        /* test */
        generic_create(QUERY_1_STATEMENT, 1L, true);
    }

    @Test
    public void create_queryStoreSameQueryHash_succeeds() throws SQLException, InterruptedException {

        /* test */
        generic_create(QUERY_1_STATEMENT, 1L, true);
        generic_create(QUERY_2_STATEMENT, 2L, false);
        generic_create(QUERY_1_STATEMENT, 1L, false);
    }

    @Test
    public void create_systemProcedure_succeeds() throws SQLException, InterruptedException {

        /* test */
        generic_system_create("root", "mariadb");
    }

    @Test
    public void create_systemProcedure_fails() {

        /* test */
        assertThrows(SQLException.class, () -> {
            generic_system_create("junit", "junit");
        });
    }

    @Test
    public void create_userProcedureRoot_succeeds() throws SQLException, InterruptedException {

        /* test */
        generic_user_create("root", "mariadb");
    }

    @Test
    public void create_userProcedureUser_succeeds() throws SQLException, InterruptedException {

        /* test */
        generic_user_create("junit", "junit");
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(String query, Long assertQueryId, boolean create) throws InterruptedException,
            SQLException {

        /* mock */
        if (create) {
            final String bind = new File(
                    "./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
            log.trace("container bind {}", bind);
            containerRepository.save(CONTAINER_3);
            createContainer(bind, CONTAINER_3, CONTAINER_3_ENV);
            startContainer(CONTAINER_3);
        }

        /* test */
        final Long response = MariaDbConfig.mockSystemQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, query);
        assertNotNull(response);
        assertEquals(assertQueryId, response);
    }

    protected void generic_create(Long containerId, DatabaseCreateDto createDto, Container container, Database database)
            throws UserNotFoundException, DatabaseNameExistsException, DatabaseConnectionException,
            QueryMalformedException, ImageNotSupportedException, AmqpException, ContainerNotFoundException,
            ContainerConnectionException, DatabaseMalformedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseidxRepository.save(any(Database.class)))
                .thenReturn(database);
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final Database response = databaseService.create(containerId, createDto, principal);
        assertEquals(database.getName(), response.getName());
    }

    protected void generic_system_create(String username, String password) throws InterruptedException, SQLException {

        /* mock */
        final String bind = new File(
                "./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        containerRepository.save(CONTAINER_3);
        createContainer(bind, CONTAINER_3, CONTAINER_3_ENV);
        startContainer(CONTAINER_3);

        /* test */
        final Long queryId = MariaDbConfig.mockSystemQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME,
                QUERY_1_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

    protected void generic_user_create(String username, String password) throws InterruptedException, SQLException {

        /* mock */
        final String bind = new File(
                "./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        containerRepository.save(CONTAINER_3);
        createContainer(bind, CONTAINER_3, CONTAINER_3_ENV);
        startContainer(CONTAINER_3);

        /* test */
        final Long queryId = MariaDbConfig.mockUserQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME,
                QUERY_1_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

}
