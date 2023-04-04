
package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.TableRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceIntegrationWriteTest extends BaseUnitTest {

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
    private ImageRepository imageRepository;

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

    @BeforeEach
    public void beforeEach() throws InterruptedException {
        afterEach();
        /* create networks */
        DockerConfig.createAllNetworks();
        /* create user container */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        /* metadata database */
        h2Utils.runScript("schema.sql");
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_1) /* will have 2 tables */;
        tableRepository.save(TABLE_1);
        tableRepository.save(TABLE_2);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_3_CREATE_DTO, principal);
    }

    @Test
    public void create_failedBefore_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        try {
            tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_3_INVALID_CREATE_DTO, principal);
        } catch (TableMalformedException e) {
            /* ignore */
        }
        tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_3_CREATE_DTO, principal);
    }

    @Test
    public void create_withConstraints_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_4_CREATE_DTO, principal); // table to reference
        tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_5_CREATE_DTO, principal);
    }

    @Test
    public void create_withForeignKeyButWithoutReferencingTable_fails() {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_5_CREATE_DTO, principal);
        });
    }

    @Test
    public void delete_succeeds() throws TableMalformedException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, TableNotFoundException, DataProcessingException {

        /* mock */
        doNothing()
                .when(tableidxRepository)
                .delete(any(TableDto.class));

        /* test */
        tableService.deleteTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);
    }

}
