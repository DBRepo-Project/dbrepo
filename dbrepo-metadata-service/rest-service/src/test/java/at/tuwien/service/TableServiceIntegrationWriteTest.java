package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class TableServiceIntegrationWriteTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableMapper tableMapper;

    @Autowired
    private UserRepository userRepository;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_1_FOREIGN_KEY_1.setReferences(List.of(TABLE_1_FOREIGN_KEY_REFERENCE));
        TABLE_1.setConstraints(TABLE_1_CONSTRAINTS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_2.setConstraints(TABLE_2_CONSTRAINTS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_3.setConstraints(TABLE_3_CONSTRAINTS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        DATABASE_1.setAccesses(List.of());
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_3.setDatabase(DATABASE_1);
        TABLE_4.setDatabase(DATABASE_1);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2));
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        /* data stuff */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException, TableNotFoundException {
        final TableCreateDto request = TableCreateDto.builder()
                .name("Hello Table")
                .description(TABLE_3_DESCRIPTION)
                .columns(List.of())
                .constraints(TABLE_3_CONSTRAINTS_CREATE_DTO)
                .build();

        /* test */
        final Table response = tableService.createTable(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertNotNull(response.getId());
    }

    @Test
    public void create_withConstraints_succeeds() throws TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException, SQLException,
            TableNotFoundException {

        /* test */
        tableService.createTable(DATABASE_1_ID, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL); // table to reference
        assertTrue(MariaDbConfig.tableExists(DATABASE_1, TABLE_5_INTERNALNAME));
        final Table response = tableService.createTable(DATABASE_1_ID, TABLE_6_CREATE_DTO, USER_1_PRINCIPAL);
        assertTrue(MariaDbConfig.tableExists(DATABASE_1, TABLE_6_INTERNALNAME));
        assertNotNull(response.getId());
        assertEquals(TABLE_6_NAME, response.getName());
        assertEquals(TABLE_6_INTERNALNAME, response.getInternalName());
        assertEquals(TABLE_6_DESCRIPTION, response.getDescription());
    }

    @Test
    public void create_full_succeeds() throws Exception {

        /* test */
        final Table response = tableService.createTable(DATABASE_1_ID, TABLE_0_CREATE_DTO, USER_1_PRINCIPAL);
        assertNotNull(response.getId());
        assertEquals("full", response.getInternalName());
        assertEquals("full example", response.getDescription());
        assertEquals(32, response.getColumns().size());
        for (int i = 1; i < TABLE_0_CREATE_DTO.getColumns().size(); i++) {
            final ColumnCreateDto expected = TABLE_0_CREATE_DTO.getColumns().get(i);
            final TableColumn result = response.getColumns().get(i);
            assertEquals(expected.getName(), result.getName());
            assertEquals(expected.getType(), tableMapper.columnTypeToColumnTypeDto(result.getColumnType()));
            if (expected.getSize() == null) {
                assertNull(result.getSize());
            } else {
                assertEquals(expected.getSize(), result.getSize());
            }
            if (expected.getD() == null) {
                assertNull(result.getD());
            } else {
                assertEquals(expected.getD(), result.getD());
            }
            if (expected.getDfid() == null) {
                assertNull(result.getDateFormat());
            } else {
                assertNotNull(result.getDateFormat());
                assertEquals(expected.getDfid(), result.getDateFormat().getId());
            }
        }
        final Map<String, List<Object>> schema = MariaDbConfig.describeTableSchema(response, CONTAINER_1_PRIVILEGED_USERNAME, CONTAINER_1_PRIVILEGED_PASSWORD);
        for (Map.Entry<String, List<Object>> entry : schema.entrySet()) {
            final ColumnCreateDto columnRequest = TABLE_0_CREATE_DTO.getColumns().stream().filter(c -> c.getName().equals(entry.getKey())).findFirst().get();
            final TableColumn columnEntity = response.getColumns().stream().filter(c -> c.getName().equals(entry.getKey())).findFirst().get();
            final List<Object> columnSchema = schema.get(columnEntity.getInternalName());
            if (columnEntity.getInternalName().equals("id")) {
                continue;
            }
            log.trace("internalName={}, type={}, size={}", columnEntity.getInternalName(), columnEntity.getColumnType(), columnEntity.getSize());
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

    @Test
    public void create_withForeignKeyButWithoutReferencingTable_fails() {

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.createTable(DATABASE_1_ID, TABLE_6_CREATE_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    @Transactional
    public void delete_succeeds() throws TableNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        tableService.deleteTable(DATABASE_1_ID, TABLE_1_ID);
    }

    @Test
    @Transactional
    public void delete_full_succeeds() throws TableNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException {

        /* test */
        final Table response = tableService.createTable(DATABASE_1_ID, TABLE_0_CREATE_DTO, USER_1_PRINCIPAL);
        tableService.deleteTable(DATABASE_1_ID, response.getId());
    }

    @Test
    @Transactional
    public void delete_hasIdentifier_succeeds() throws TableNotFoundException, TableMalformedException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        tableService.deleteTable(DATABASE_1_ID, TABLE_4_ID);
    }

    private Long getLength(Object type) {
        final Pattern pattern = Pattern.compile("\\(([0-9]+)\\)");
        final Matcher matcher = pattern.matcher(String.valueOf(type));
        if (!matcher.find()) {
            log.error("Failed to extract length");
            return null;
        }
        final String raw = matcher.group();
        return Long.valueOf(raw.substring(1, raw.length() - 1));
    }

}
