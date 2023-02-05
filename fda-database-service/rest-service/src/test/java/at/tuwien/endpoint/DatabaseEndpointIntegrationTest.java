package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.DatabaseEndpoint;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.repository.elastic.DatabaseIdxRepository;
import at.tuwien.repository.jpa.*;
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

import java.io.File;
import java.security.Principal;
import java.sql.SQLException;

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
    private IndexConfig indexConfig;

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
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    private final static String BIND = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        /* create networks */
        DockerConfig.createAllNetworks();
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
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
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException,
            BrokerVirtualHostGrantException {
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
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException,
            BrokerVirtualHostGrantException {
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
    @WithMockUser(username = USER_1_USERNAME)
    public void transfer_self_succeeds() throws InterruptedException, UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_1_USERNAME)
                .build();

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);

        /* test */
        transfer_generic(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void transfer_notOwner_fails() throws InterruptedException {
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
        assertThrows(NotAllowedException.class, () -> {
            transfer_generic(CONTAINER_1_ID, DATABASE_1_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developerForeignContainer_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException,
            BrokerVirtualHostGrantException {
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
            InterruptedException, SQLException, BrokerVirtualHostGrantException {

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

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findById_succeeds() throws DatabaseNotFoundException, InterruptedException, AccessDeniedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_SEARCH, CONTAINER_SEARCH_ENV);
        DockerConfig.startContainer(CONTAINER_SEARCH);
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final DatabaseDto response = findById_generic(CONTAINER_1_ID, DATABASE_1_ID, USER_1_PRINCIPAL);
        assertEquals(DATABASE_1_ID, response.getId());
        assertEquals(DATABASE_1_NAME, response.getName());
        assertEquals(DATABASE_1_EXCHANGE, response.getExchangeName());
        assertEquals(DATABASE_1_DESCRIPTION, response.getDescription());
        assertEquals(DATABASE_1_INTERNALNAME, response.getInternalName());
        final IdentifierDto identifier = response.getIdentifier();
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        assertEquals(IDENTIFIER_1_TITLE, identifier.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, identifier.getDescription());
        assertEquals(IDENTIFIER_1_PUBLISHER, identifier.getPublisher());
        assertEquals(IDENTIFIER_1_VISIBILITY_DTO, identifier.getVisibility());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void create_generic(Long containerId, Container container, Database database, DatabaseAccess access,
                               DatabaseCreateDto data, Principal principal) throws UserNotFoundException,
            DatabaseNameExistsException, NotAllowedException, ContainerConnectionException, DatabaseMalformedException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException,
            BrokerVirtualHostGrantException {

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
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException,
            BrokerVirtualHostGrantException {

        /* mock */
        doNothing()
                .when(brokerServiceGateway)
                .grantPermission(anyString(), any(GrantVirtualHostPermissionsDto.class));
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_1);
        databaseRepository.save(DATABASE_2);
        databaseIdxRepository.save(DATABASE_1_DTO);
        databaseIdxRepository.save(DATABASE_2_DTO);

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
        databaseIdxRepository.save(DATABASE_1_DTO);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.transfer(containerId, databaseId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        final DatabaseDto body = response.getBody();
        assertEquals(principal.getName(), body.getCreator().getUsername());
        assertEquals(data.getUsername(), body.getOwner().getUsername());
    }

    public DatabaseDto findById_generic(Long containerId, Long databaseId, Principal principal)
            throws DatabaseNotFoundException, AccessDeniedException {

        /* mock */
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        databaseIdxRepository.save(DATABASE_1_DTO);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.findById(containerId, databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }
}
