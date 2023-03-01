package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.TableRepository;
import com.rabbitmq.client.Channel;
import config.DockerConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceIntegrationReadTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private TableRepository tableRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private H2Utils h2Utils;

    final static String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        DockerConfig.createAllNetworks();
        /* user container */
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
        /* metadata db */
        h2Utils.runScript("schema.sql");
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        TABLE_1.setColumns(List.of());
        TABLE_2.setColumns(List.of());
        TABLE_3.setColumns(List.of());
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void findHistory_anonymous_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* mock */
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, null);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithAnonymousUser
    public void findHistory_anonymous2_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* mock */
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, null);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findHistory_researcher_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* mock */
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findHistory_developer_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* mock */
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, USER_2_PRINCIPAL);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findHistory_dataSteward_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* mock */
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, USER_3_PRINCIPAL);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

}
