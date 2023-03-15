package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.repository.elastic.DatabaseIdxRepository;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.impl.MariaDbServiceImpl;
import com.rabbitmq.client.Channel;
import at.tuwien.config.DockerConfig;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DatabaseServiceComponentTest extends BaseUnitTest {

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
    public void create_elasticSearch_succeeds() throws Exception {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_ELASTIC, CONTAINER_ELASTIC_ENV);
        DockerConfig.startContainer(CONTAINER_ELASTIC);
        DockerConfig.createContainer(BIND_MUSICOLOGY, CONTAINER_3, CONTAINER_3_ENV);
        DockerConfig.startContainer(CONTAINER_3);
        MariaDbConfig.dropDatabase(CONTAINER_3_INTERNALNAME, DATABASE_3_INTERNALNAME, "root", "mariadb");
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_3_DTO);

        /* test */
        generic_create(CONTAINER_3_ID, DATABASE_3_CREATE, DATABASE_3);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

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

}
