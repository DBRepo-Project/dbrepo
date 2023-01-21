package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.DatabaseEndpoint;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.repository.elastic.DatabaseidxRepository;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;

import static at.tuwien.config.DockerConfig.dockerClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class DatabaseEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private Channel channel;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseidxRepository databaseidxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    private final static String BIND = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() {
        afterAll();
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
    }

    @AfterAll
    public static void afterAll() {
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
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
    }

    @AfterEach
    public void afterEach() {
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
    }

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* metadata database */
        imageRepository.save(IMAGE_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcher_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);
        userRepository.save(USER_1);

        /* test */
        create_generic(CONTAINER_1_ID, CONTAINER_1, null, null, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcherExists_fails() throws InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1, DATABASE_1_OWNER_ACCESS, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developer_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_2_NAME)
                .isPublic(DATABASE_2_PUBLIC)
                .build();

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);

        /* test */
        create_generic(CONTAINER_1_ID, CONTAINER_1, null, null, request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void transfer_succeeds() throws InterruptedException, UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_2_USERNAME)
                .build();

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);

        /* test */
        transfer_generic(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void transfer_noRole_succeeds() throws InterruptedException, UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_2_USERNAME)
                .build();

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);

        /* test */
        transfer_generic(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developerForeignContainer_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_2_NAME)
                .isPublic(DATABASE_2_PUBLIC)
                .build();

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);

        /* test */
        create_generic(CONTAINER_1_ID, CONTAINER_1, null, null, request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_developer_succeeds() throws UserNotFoundException, DatabaseConnectionException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, AmqpException,
            BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException,
            InterruptedException, SQLException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_2, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2);
        MariaDbConfig.mockQuery(CONTAINER_2_INTERNALNAME, "CREATE DATABASE `" + DATABASE_2_INTERNALNAME + "`", "root", "mariadb");
        userRepository.save(USER_1);
        userRepository.save(USER_2);

        /* test */
        delete_generic(CONTAINER_2_ID, DATABASE_2_ID, USER_2_PRINCIPAL);
    }
    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void create_generic(Long containerId, Container container, Database database, DatabaseAccess access,
                               DatabaseCreateDto data, Principal principal) throws UserNotFoundException,
            DatabaseNameExistsException, NotAllowedException, ContainerConnectionException, DatabaseMalformedException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException {

        /* mock */
        containerRepository.save(container);
        if (database != null) {
            databaseRepository.save(database);
        }
        if (access != null) {
            databaseAccessRepository.save(access);
        }

        /* test */
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.create(containerId, data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void delete_generic(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException {

        /* mock */
        doNothing()
                .when(brokerServiceGateway)
                .grantPermission(anyString(), any(GrantVirtualHostPermissionsDto.class));
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_1);
        databaseRepository.save(DATABASE_2);
        databaseidxRepository.save(DATABASE_1);
        databaseidxRepository.save(DATABASE_2);

        /* test */
        final ResponseEntity<?> response = databaseEndpoint.delete(containerId, databaseId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    public void transfer_generic(Long containerId, Long databaseId, DatabaseTransferDto data, Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException {

        /* mock */
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        databaseidxRepository.save(DATABASE_1);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.transfer(containerId, databaseId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        final DatabaseDto body = response.getBody();
        assertEquals(principal.getName(), body.getCreator().getUsername());
        assertEquals(data.getUsername(), body.getOwner().getUsername());
    }
}
