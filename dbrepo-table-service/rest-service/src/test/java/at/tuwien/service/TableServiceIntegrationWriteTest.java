
package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.repository.sdb.ConceptIdxRepository;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.UnitIdxRepository;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
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
    private Channel channel;

    @MockBean
    private UnitIdxRepository unitIdxRepository;

    @MockBean
    private ConceptIdxRepository conceptIdxRepository;

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
        tableRepository.save(TABLE_1_SIMPLE);
        tableRepository.save(TABLE_2_SIMPLE);
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
    public void create_full_succeeds() throws Exception {
        final TableCreateDto request = TableCreateDto.builder()
                .name("full")
                .description("full example")
                .constraints(ConstraintsCreateDto.builder()
                        .uniques(List.of())
                        .foreignKeys(List.of())
                        .build())
                .columns(List.of(ColumnCreateDto.builder()
                                .name("col1a")
                                .type(ColumnTypeDto.CHAR)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col1b")
                                .type(ColumnTypeDto.CHAR)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .size(50)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col2a")
                                .type(ColumnTypeDto.VARCHAR)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col2b")
                                .type(ColumnTypeDto.VARCHAR)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .indexLength(1024)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col3")
                                .type(ColumnTypeDto.BINARY)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col4")
                                .type(ColumnTypeDto.VARBINARY)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .size(200)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col5")
                                .type(ColumnTypeDto.TINYBLOB)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col6")
                                .type(ColumnTypeDto.TINYTEXT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col7")
                                .type(ColumnTypeDto.TEXT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col8")
                                .type(ColumnTypeDto.BLOB)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col9")
                                .type(ColumnTypeDto.MEDIUMTEXT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col10")
                                .type(ColumnTypeDto.MEDIUMBLOB)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col11")
                                .type(ColumnTypeDto.LONGTEXT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col12")
                                .type(ColumnTypeDto.LONGBLOB)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col13")
                                .type(ColumnTypeDto.ENUM)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .enums(List.of("val1", "val2"))
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col14")
                                .type(ColumnTypeDto.SET)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .sets(List.of("val1", "val2"))
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col15")
                                .type(ColumnTypeDto.BIT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col16")
                                .type(ColumnTypeDto.TINYINT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col17")
                                .type(ColumnTypeDto.BOOL)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col18")
                                .type(ColumnTypeDto.SMALLINT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col19")
                                .type(ColumnTypeDto.MEDIUMINT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col20")
                                .type(ColumnTypeDto.INT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col21")
                                .type(ColumnTypeDto.BIGINT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col22")
                                .type(ColumnTypeDto.FLOAT)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col23")
                                .type(ColumnTypeDto.DOUBLE)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col24")
                                .type(ColumnTypeDto.DECIMAL)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col25")
                                .type(ColumnTypeDto.DATE)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col26")
                                .type(ColumnTypeDto.DATETIME)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col27")
                                .type(ColumnTypeDto.TIMESTAMP)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col28")
                                .type(ColumnTypeDto.TIME)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("col29")
                                .type(ColumnTypeDto.YEAR)
                                .nullAllowed(true)
                                .primaryKey(false)
                                .build()))
                .build();

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        final Table response = tableService.createTable(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals("full", response.getInternalName());
        assertEquals("full example", response.getDescription());
        assertEquals(32, response.getColumns().size());
        final Map<String, List<Object>> schema = MariaDbConfig.describeTableSchema(response, CONTAINER_1_PRIVILEGED_USERNAME, CONTAINER_1_PRIVILEGED_PASSWORD);
        for (Map.Entry<String, List<Object>> entry : schema.entrySet()) {
            final ColumnCreateDto columnRequest = request.getColumns().stream().filter(c -> c.getName().equals(entry.getKey())).findFirst().get();
            final TableColumn columnEntity = response.getColumns().stream().filter(c -> c.getName().equals(entry.getKey())).findFirst().get();
            final List<Object> columnSchema = schema.get(columnEntity.getInternalName());
            if (columnEntity.getInternalName().equals("id")) {
                continue;
            }
            log.trace("internalName={}, type={}", columnEntity.getInternalName(), columnEntity.getColumnType());
            /* correct in the metadata database */
            assertEquals(columnRequest.getNullAllowed(), columnEntity.getIsNullAllowed());
            assertEquals(columnRequest.getPrimaryKey(), columnEntity.getIsPrimaryKey());
            /* correct in the user database */
            assertEquals(columnRequest.getType(), MariaDbConfig.typetoColumnTypeDto(String.valueOf(columnSchema.get(0)))) /* type */;
            if (columnRequest.getSize() != null) {
                assertEquals(columnRequest.getSize(), getLength(columnSchema.get(0))) /* length */;
            }
            final boolean isNullAllowed = String.valueOf(columnSchema.get(1)).equals("YES") /* nullable */;
            assertTrue(isNullAllowed);
        }
    }

    private String getType(Object type) {
        final Pattern pattern = Pattern.compile("^([a-z]+)");
        final Matcher matcher = pattern.matcher(String.valueOf(type));
        if (!matcher.find()) {
            log.error("Failed to extract type");
            return null;
        }
        return matcher.group();
    }

    private Integer getLength(Object type) {
        final Pattern pattern = Pattern.compile("\\(([0-9]+)\\)");
        final Matcher matcher = pattern.matcher(String.valueOf(type));
        if (!matcher.find()) {
            log.error("Failed to extract length");
            return null;
        }
        final String raw = matcher.group();
        return Integer.valueOf(raw.substring(1, raw.length() - 1));
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
