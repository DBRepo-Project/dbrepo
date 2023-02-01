package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
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
import java.util.Arrays;
import java.util.Optional;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class StoreServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private QueryService queryService;

    @BeforeEach
    public void beforeEach() throws InterruptedException {
        afterEach();
        /* create network */
        dockerClient.createNetworkCmd()
                .withName("fda-userdb")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.28.0.0/16")))
                .withEnableIpv6(false)
                .exec();
        /* create container */
        final String bind = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb").withBinds(Bind.parse(bind)))
                .withName(CONTAINER_1_INTERNALNAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withHealthcheck(CONTAINER_1_HEALTHCHECK)
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=weather")
                .exec();
        CONTAINER_1.setHash(response.getId());
        /* start */
        DockerConfig.startContainer(CONTAINER_1);
        TABLE_1.setDatabase(DATABASE_1);
    }

    @AfterEach
    public void afterEach() {
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

    @Test
    public void findAll_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, ContainerNotFoundException, SQLException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_1, USER_1_USERNAME);

        /* test */
        storeService.findAll(CONTAINER_1_ID, DATABASE_1_ID, null, USER_1_PRINCIPAL);
    }

    @Test
    public void findAll_filterPersisted_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, ContainerNotFoundException, SQLException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_1, USER_1_USERNAME);

        /* test */
        storeService.findAll(CONTAINER_1_ID, DATABASE_1_ID, true, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_same_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final Query response = storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        log.debug("found queries in query store: {}", MariaDbConfig.selectQuery(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME,
                "SELECT `query_normalized`, `query_hash`, `result_hash` FROM `qs_queries`", "query_normalized", "query_hash", "result_hash"));
        assertEquals(QUERY_1_ID, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_different_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException {
        final ExecuteStatementDto mock = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, mock, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(2L, response.getId()) /* new query inserted */;
    }

    @Test
    public void execute_same_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_dataChangeSameQuery_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        MariaDbConfig.execute(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, "INSERT INTO weather_aus (id, `date`, location, mintemp, rainfall) VALUES (4, '2008-12-04', 'Albury', 12.9, 0.2)");

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(2L, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_semicolon_fails() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT + ";")
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        });
    }

    @Test
    public void insert_anonymous_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, null);
    }

    @Test
    public void insert_notFound_succeeds() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.empty());
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_1, USER_1_USERNAME);

        /* test */
        storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void findOne_notFound_succeeds() {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_notFound_fails() {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_2_ID, principal);
        });
    }

}
