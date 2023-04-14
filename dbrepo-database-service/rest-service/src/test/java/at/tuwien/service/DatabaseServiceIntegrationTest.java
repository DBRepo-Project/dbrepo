package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.repository.elastic.DatabaseIdxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.impl.MariaDbServiceImpl;
import at.tuwien.test.BaseTest;
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
    private UserRepository userRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private MariaDbServiceImpl databaseService;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private MariaDbConfig mariaDbConfig;

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
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void create_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        MariaDbConfig.dropDatabase(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, "root", "mariadb");
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_create(CONTAINER_3_ID, DATABASE_3_CREATE, DATABASE_3);
    }

    @Test
    public void create_inSequence_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND_ZOO, CONTAINER_2, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2);
        MariaDbConfig.dropDatabase(CONTAINER_2_INTERNALNAME, DATABASE_2_INTERNALNAME, "root", "mariadb");
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        MariaDbConfig.dropDatabase(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, "root", "mariadb");
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_2_DTO)
                .thenReturn(DATABASE_3_DTO);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, DATABASE_2);
        generic_create(CONTAINER_3_ID, DATABASE_3_CREATE, DATABASE_3);
    }

    @Test
    public void create_outOfSequence_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND_ZOO, CONTAINER_2, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2);
        MariaDbConfig.dropDatabase(CONTAINER_2_INTERNALNAME, DATABASE_2_INTERNALNAME, "root", "mariadb");
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        MariaDbConfig.dropDatabase(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, "root", "mariadb");
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_3_DTO)
                .thenReturn(DATABASE_2_DTO);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_create(CONTAINER_3_ID, DATABASE_3_CREATE, DATABASE_3);
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, DATABASE_2);
    }

    @Test
    public void create_queryStore_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_queryStoreSameQueryHash_succeeds() throws SQLException, InterruptedException,
            QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
        generic_insert(QUERY_5_STATEMENT, 2L);
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_systemProcedure_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_system_insert("root", "mariadb");
    }

    @Test
    public void create_systemProcedure_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        assertThrows(SQLException.class, () -> {
            generic_system_insert("junit1", "junit1");
        });
    }

    @Test
    public void create_userProcedureRoot_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_user_insert("root", "mariadb");
    }

    @Test
    public void create_userProcedureUser_succeeds() throws SQLException, InterruptedException, QueryMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));

        /* test */
        generic_user_insert("junit1", "junit1");
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
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* test */
        final Database response = databaseService.create(containerId, createDto, principal);
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
