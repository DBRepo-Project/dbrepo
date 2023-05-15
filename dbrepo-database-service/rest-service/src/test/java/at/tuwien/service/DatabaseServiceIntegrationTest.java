package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.DatabaseIdxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.impl.MariaDbServiceImpl;
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
import java.util.List;
import java.util.Optional;

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
    private Channel channel;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @Autowired
    private MariaDbConfig mariaDbConfig;

    private final static String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
    private final static String BIND_ZOO = new File("../../dbrepo-metadata-db/test/src/test/resources/zoo").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
    private final static String BIND_MUSICOLOGY = new File("../../dbrepo-metadata-db/test/src/test/resources/musicology").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        DockerConfig.createAllNetworks();
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        afterEach();
        DockerConfig.createAllNetworks();
        /* metadata database */
        imageRepository.save(IMAGE_1);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        userRepository.save(USER_3);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void create_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        MariaDbConfig.dropDatabase(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, "root", "mariadb");
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_3_DTO);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_create(CONTAINER_3_ID, DATABASE_3_CREATE, DATABASE_3);
    }

    @Test
    public void create_inSequence_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND_ZOO, CONTAINER_2_SIMPLE, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2_SIMPLE);
        MariaDbConfig.dropDatabase(CONTAINER_2_INTERNALNAME, DATABASE_2_INTERNALNAME, "root", "mariadb");
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        MariaDbConfig.dropDatabase(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, "root", "mariadb");
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_2_DTO)
                .thenReturn(DATABASE_3_DTO);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE);
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, DATABASE_2);
        generic_create(CONTAINER_3_ID, DATABASE_3_CREATE, DATABASE_3);
    }

    @Test
    public void create_outOfSequence_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND_ZOO, CONTAINER_2_SIMPLE, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2_SIMPLE);
        MariaDbConfig.dropDatabase(CONTAINER_2_INTERNALNAME, DATABASE_2_INTERNALNAME, "root", "mariadb");
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        MariaDbConfig.dropDatabase(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, "root", "mariadb");
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_3_DTO)
                .thenReturn(DATABASE_2_DTO);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE);
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_create(CONTAINER_3_ID, DATABASE_3_CREATE, DATABASE_3);
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, DATABASE_2);
    }

    @Test
    public void create_queryStore_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_queryStoreSameQueryHash_succeeds() throws SQLException, InterruptedException,
            QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
        generic_insert(QUERY_5_STATEMENT, 2L);
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_systemProcedure_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_system_insert("root", "mariadb");
    }

    @Test
    public void create_systemProcedure_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        assertThrows(SQLException.class, () -> {
            generic_system_insert("junit1", "junit1");
        });
    }

    @Test
    public void create_userProcedureRoot_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_user_insert("root", "mariadb");
    }

    @Test
    public void create_userProcedureUser_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3_SIMPLE, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_2_SIMPLE) /* increase id */;
        containerRepository.save(CONTAINER_3_SIMPLE);

        /* test */
        generic_user_insert("junit1", "junit1");
    }

    @Test
    public void delete_succeeds() throws InterruptedException, QueryMalformedException, UserNotFoundException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, DatabaseMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        databaseService.delete(CONTAINER_1_ID, DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void visibility_succeeds() throws DatabaseNotFoundException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1);

        /* test */
        final Database response = databaseService.visibility(CONTAINER_1_ID, DATABASE_1_ID, request);
        assertTrue(response.getIsPublic());
    }

    @Test
    public void transfer_succeeds() throws DatabaseNotFoundException, UserNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_2_USERNAME)
                .build();

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1);

        /* test */
        final Database response = databaseService.transfer(CONTAINER_1_ID, DATABASE_1_ID, request);
        assertEquals(USER_2_ID, response.getOwnedBy());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_insert(String query, Long assertQueryId) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.mockGrantUserPermissions(CONTAINER_3_INTERNALNAME, DATABASE_3, USER_1);

        /* test */
        final Long response = MariaDbConfig.mockSystemQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, query);
        assertNotNull(response);
        assertEquals(assertQueryId, response);
    }

    protected void generic_create(Long containerId, DatabaseCreateDto createDto, Database database)
            throws Exception {

        /* test */
        final Database response = databaseService.create(containerId, createDto, USER_1_PRINCIPAL);
        assertEquals(database.getName(), response.getName());
        assertEquals(containerId, database.getId());
    }

    protected void generic_system_insert(String username, String password) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.mockGrantUserPermissions(CONTAINER_3_INTERNALNAME, DATABASE_3, USER_1);

        /* test */
        final Long queryId = MariaDbConfig.mockSystemQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME,
                QUERY_4_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

    protected void generic_user_insert(String username, String password) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.mockGrantUserPermissions(CONTAINER_3_INTERNALNAME, DATABASE_3, USER_1);

        /* test */
        final Long queryId = MariaDbConfig.mockUserQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME,
                QUERY_4_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

}
