package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.*;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
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
    private Channel channel;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @Autowired
    private H2Utils h2Utils;

    private final static String BIND = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

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
        /* metadata database */
        h2Utils.runScript("schema.sql");
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
    }

    @Test
    public void create_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_CREATE, DATABASE_1);
    }

    @Test
    public void create_inSequence_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        DockerConfig.createContainer(BIND, CONTAINER_2, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO)
                .thenReturn(DATABASE_2_DTO);

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_CREATE, DATABASE_1);
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, DATABASE_2);
    }

    @Test
    public void create_outOfSequence_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        DockerConfig.createContainer(BIND, CONTAINER_2, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO)
                .thenReturn(DATABASE_2_DTO);

        /* test */
        generic_create(CONTAINER_2_ID, DATABASE_2_CREATE, DATABASE_2);
        generic_create(CONTAINER_1_ID, DATABASE_1_CREATE, DATABASE_1);
    }

    @Test
    public void create_queryStore_succeeds() throws SQLException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);

        /* test */
        generic_create(QUERY_1_STATEMENT, 1L);
    }

    @Test
    public void create_queryStoreSameQueryHash_succeeds() throws SQLException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);

        /* test */
        generic_create(QUERY_1_STATEMENT, 1L);
        generic_create(QUERY_2_STATEMENT, 2L);
        generic_create(QUERY_1_STATEMENT, 1L);
    }

    @Test
    public void create_systemProcedure_succeeds() throws SQLException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);

        /* test */
        generic_system_create("root", "mariadb");
    }

    @Test
    public void create_systemProcedure_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);

        /* test */
        assertThrows(SQLException.class, () -> {
            generic_system_create("junit", "junit");
        });
    }

    @Test
    public void create_userProcedureRoot_succeeds() throws SQLException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);

        /* test */
        generic_user_create("root", "mariadb");
    }

    @Test
    public void create_userProcedureUser_succeeds() throws SQLException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);

        /* test */
        generic_user_create("junit", "junit");
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(String query, Long assertQueryId) throws InterruptedException,
            SQLException {

        /* mock */
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        containerRepository.save(CONTAINER_3);

        /* test */
        final Long response = MariaDbConfig.mockSystemQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, query);
        assertNotNull(response);
        assertEquals(assertQueryId, response);
    }

    protected void generic_create(Long containerId, DatabaseCreateDto createDto, Database database)
            throws Exception {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        containerRepository.save(CONTAINER_3);

        /* test */
        final Database response = databaseService.create(containerId, createDto, principal);
        assertEquals(database.getName(), response.getName());
        assertEquals(containerId, database.getId());
    }

    protected void generic_system_create(String username, String password) throws SQLException {

        /* mock */
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        containerRepository.save(CONTAINER_3);

        /* test */
        final Long queryId = MariaDbConfig.mockSystemQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME,
                QUERY_1_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

    protected void generic_user_create(String username, String password) throws InterruptedException, SQLException {

        /* mock */
        containerRepository.save(CONTAINER_3);
        createContainer(BIND, CONTAINER_3, CONTAINER_3_ENV);
        startContainer(CONTAINER_3);

        /* test */
        final Long queryId = MariaDbConfig.mockUserQueryInsert(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME,
                QUERY_1_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

}
