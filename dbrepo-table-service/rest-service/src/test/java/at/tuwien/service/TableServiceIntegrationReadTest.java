package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.*;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceIntegrationReadTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private TableIdxRepository tableidxRepository;

    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private H2Utils h2Utils;

    private static final String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        DockerConfig.createAllNetworks();
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        h2Utils.runScript("schema.sql");
        imageRepository.save(IMAGE_1);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        tableRepository.save(TABLE_1_SIMPLE);
        tableRepository.save(TABLE_2_SIMPLE);
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException {

        /* test */
        final List<Table> response = tableService.findAll(CONTAINER_1_ID, DATABASE_1_ID);
        assertEquals(2, response.size());
    }

    @Test
    public void findAll_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findAll(CONTAINER_2_ID, DATABASE_2_ID);
        });
    }

    @Test
    public void findById_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException {

        /* test */
        final Table response = tableService.findById(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);
        assertEquals(TABLE_1_ID, response.getId());
        assertEquals(TABLE_1_NAME, response.getName());
        assertEquals(TABLE_1_INTERNALNAME, response.getInternalName());
    }

    @Test
    public void findById_tableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.findById(CONTAINER_1_ID, DATABASE_1_ID, 99999L);
        });
    }

    @Test
    public void findById_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findById(CONTAINER_2_ID, 99999L, TABLE_3_ID);
        });
    }

    @Test
    public void findById_containerNotFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            tableService.findById(99999L, DATABASE_3_ID, TABLE_3_ID);
        });
    }

}
