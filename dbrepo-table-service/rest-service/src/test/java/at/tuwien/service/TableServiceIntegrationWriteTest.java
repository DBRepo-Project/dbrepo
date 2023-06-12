
package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import at.tuwien.repository.mdb.*;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.Principal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
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
    private at.tuwien.repository.mdb.TableRepository tableRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        imageRepository.save(IMAGE_1);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        tableRepository.save(TABLE_1);
        tableRepository.save(TABLE_2);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException {

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        tableService.createTable(DATABASE_1_ID, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
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
            tableService.createTable(DATABASE_1_ID, TABLE_3_INVALID_CREATE_DTO, principal);
        } catch (TableMalformedException e) {
            /* ignore */
        }
        tableService.createTable(DATABASE_1_ID, TABLE_3_CREATE_DTO, principal);
    }

    @Test
    public void create_withConstraints_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException {

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        tableService.createTable(DATABASE_1_ID, TABLE_4_CREATE_DTO, USER_1_PRINCIPAL); // table to reference
        tableService.createTable(DATABASE_1_ID, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL);
    }

    @Test
    public void create_withForeignKeyButWithoutReferencingTable_fails() {

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableService.createTable(DATABASE_1_ID, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL);
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
        tableService.deleteTable(DATABASE_1_ID, TABLE_1_ID);
        assertTrue(userRepository.findById(TABLE_1_CREATED_BY).isPresent());
        assertTrue(databaseRepository.findById(TABLE_1_DATABASE_ID).isPresent());
    }

    @Test
    public void delete_notFound_fails() {

        /* mock */
        doNothing()
                .when(tableidxRepository)
                .delete(any(TableDto.class));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.deleteTable(DATABASE_1_ID, 9999L);
        });
    }

}
