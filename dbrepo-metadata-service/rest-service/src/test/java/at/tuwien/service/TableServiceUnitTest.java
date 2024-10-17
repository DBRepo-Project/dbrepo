package at.tuwien.service;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyCreateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @MockBean
    private UserService userService;

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @Autowired
    private TableService tableService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void findById_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));

        /* test */
        final Table response = tableService.findById(DATABASE_3_ID, TABLE_8_ID);
        assertEquals(TABLE_8_ID, response.getId());
        assertEquals(TABLE_8_NAME, response.getName());
        assertEquals(TABLE_8_INTERNAL_NAME, response.getInternalName());
    }

    @Test
    public void findById_notFound_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.findById(DATABASE_3_ID, TABLE_1_ID);
        });
    }

    @Test
    public void findByName_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));

        /* test */
        final Table response = tableService.findByName(DATABASE_3_ID, TABLE_8_INTERNAL_NAME);
        assertEquals(TABLE_8_ID, response.getId());
        assertEquals(TABLE_8_NAME, response.getName());
        assertEquals(TABLE_8_INTERNAL_NAME, response.getInternalName());
    }

    @Test
    public void findByName_notFound_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_3_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findByName(DATABASE_3_ID, TABLE_1_INTERNALNAME);
        });
    }

    @Test
    public void createTable_succeeds() throws DataServiceException, DataServiceConnectionException,
            UserNotFoundException, TableNotFoundException, DatabaseNotFoundException, TableExistsException,
            SearchServiceException, SearchServiceConnectionException, MalformedException, OntologyNotFoundException,
            SemanticEntityNotFoundException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        doNothing()
                .when(dataServiceGateway)
                .createTable(eq(DATABASE_1_ID), any(TableCreateDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Table response = tableService.createTable(DATABASE_1, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
        assertEquals(TABLE_3_INTERNALNAME, response.getInternalName());
    }

    @Test
    public void createTable_nonStandardColumnNames_succeeds() throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, TableNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException,
            OntologyNotFoundException, SemanticEntityNotFoundException {
        final TableCreateDto request = TableCreateDto.builder()
                .name("New Table")
                .description("A wonderful table")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("I Am Späshül")
                        .nullAllowed(true)
                        .type(ColumnTypeDto.TEXT)
                        .build()))
                .constraints(ConstraintsCreateDto.builder()
                        .checks(Set.of())
                        .uniques(List.of(List.of("I Am Späshül")))
                        .foreignKeys(List.of())
                        .primaryKey(Set.of())
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        doNothing()
                .when(dataServiceGateway)
                .createTable(eq(DATABASE_1_ID), any(TableCreateDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Table response = tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        assertEquals("New Table", response.getName());
        assertEquals("new_table", response.getInternalName());
        assertEquals(1, response.getColumns().size());
        /* columns */
        final TableColumn column0 = response.getColumns().get(0);
        assertEquals("I Am Späshül", column0.getName());
        assertEquals("i_am_spa_shu_l", column0.getInternalName());
        assertEquals(TableColumnType.TEXT, column0.getColumnType());
        assertTrue(column0.getIsNullAllowed());
        /* constraints */
        final Constraints constraints = response.getConstraints();
        assertEquals(0, constraints.getPrimaryKey().size());
        assertEquals(1, constraints.getUniques().get(0).getColumns().size());
        assertNotNull(constraints.getUniques().get(0).getName());
        assertEquals(column0.getName(), constraints.getUniques().get(0).getColumns().get(0).getName());
        assertEquals(column0.getInternalName(), constraints.getUniques().get(0).getColumns().get(0).getInternalName());
        assertEquals(0, constraints.getChecks().size());
        assertEquals(0, constraints.getForeignKeys().size());
    }

    @Test
    public void createTable_dateFormatNotFound_fails() throws DataServiceException, DataServiceConnectionException,
            UserNotFoundException, DatabaseNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException {
        final TableCreateDto request = TableCreateDto.builder()
                .name("New Table")
                .description("A wonderful table")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("date")
                        .nullAllowed(true)
                        .type(ColumnTypeDto.DATE)
                        .build()))
                .constraints(ConstraintsCreateDto.builder()
                        .checks(Set.of())
                        .uniques(List.of(List.of("date")))
                        .foreignKeys(List.of())
                        .primaryKey(Set.of("id"))
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        doNothing()
                .when(dataServiceGateway)
                .createTable(eq(DATABASE_1_ID), any(TableCreateDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void create_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            UserNotFoundException, TableNotFoundException, DatabaseNotFoundException, TableExistsException,
            SearchServiceException, SearchServiceConnectionException, OntologyNotFoundException,
            SemanticEntityNotFoundException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doNothing()
                .when(dataServiceGateway)
                .createTable(DATABASE_1_ID, TABLE_3_CREATE_DTO);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Table response = tableService.createTable(DATABASE_1, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
        assertNotNull(response.getId());
    }

    @Test
    public void create_dataServiceError_fails() throws DataServiceException, DataServiceConnectionException,
            UserNotFoundException, DatabaseNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(DataServiceException.class)
                .when(dataServiceGateway)
                .createTable(DATABASE_1_ID, TABLE_5_CREATE_DTO);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        assertThrows(DataServiceException.class, () -> {
            tableService.createTable(DATABASE_1, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void createTable_primaryKeyMalformed_fails() throws UserNotFoundException {
        final TableCreateDto request = TableCreateDto.builder()
                .name(TABLE_5_NAME)
                .description(TABLE_5_DESCRIPTION)
                .columns(TABLE_5_COLUMNS_CREATE)
                .constraints(ConstraintsCreateDto.builder()
                        .foreignKeys(new LinkedList<>())
                        .checks(new LinkedHashSet<>())
                        .primaryKey(Set.of("i_do_not_exist"))
                        .uniques(new LinkedList<>())
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void createTable_uniquesMalformed_fails() throws UserNotFoundException {
        final TableCreateDto request = TableCreateDto.builder()
                .name(TABLE_5_NAME)
                .description(TABLE_5_DESCRIPTION)
                .columns(TABLE_5_COLUMNS_CREATE)
                .constraints(ConstraintsCreateDto.builder()
                        .foreignKeys(new LinkedList<>())
                        .checks(new LinkedHashSet<>())
                        .primaryKey(new LinkedHashSet<>())
                        .uniques(List.of(List.of("i_do_not_exist")))
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void createTable_foreignKeyMalformed_fails() throws UserNotFoundException {
        final TableCreateDto request = TableCreateDto.builder()
                .name(TABLE_5_NAME)
                .description(TABLE_5_DESCRIPTION)
                .columns(TABLE_5_COLUMNS_CREATE)
                .constraints(ConstraintsCreateDto.builder()
                        .foreignKeys(List.of(ForeignKeyCreateDto.builder()
                                .columns(List.of("some_column"))
                                .referencedColumns(List.of("some_foreign_column"))
                                .referencedTable("some_referenced_table")
                                .referencedTable("i_do_not_exist")
                                .build()))
                        .checks(new LinkedHashSet<>())
                        .primaryKey(new LinkedHashSet<>())
                        .uniques(new LinkedList<>())
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @Transactional
    public void delete_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .deleteTable(DATABASE_1_ID, TABLE_1_ID);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        tableService.deleteTable(TABLE_1);
    }

    @Test
    @Transactional
    public void delete_hasIdentifier_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, TableNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .deleteTable(DATABASE_1_ID, TABLE_4_ID);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        tableService.deleteTable(TABLE_4);
    }

    @Test
    public void findById_tableNotFound_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_2)) /* any other db */;

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.findByName(DATABASE_1_ID, "i_do_not_exist");
        });
    }

    @Test
    public void findById_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findByName(99999L, TABLE_3_INTERNALNAME);
        });
    }

}
