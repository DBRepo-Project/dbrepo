package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.*;
import at.tuwien.entities.database.Database;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.repository.mdb.ContainerRepository;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.service.impl.MariaDbServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.Optional;

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
    private UserRepository userRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

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

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(containerRepository.findById(CONTAINER_3_ID))
                .thenReturn(Optional.of(CONTAINER_3));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_3);

        /* test */
        final Database response = databaseService.create(containerId, createDto, USER_1_PRINCIPAL);
        assertEquals(database.getName(), response.getName());
        assertEquals(containerId, database.getId());
    }

}
