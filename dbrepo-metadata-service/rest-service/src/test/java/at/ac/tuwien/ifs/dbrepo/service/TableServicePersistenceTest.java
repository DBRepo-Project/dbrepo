package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.primaryKey.PrimaryKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.repository.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.repository.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class TableServicePersistenceTest extends BaseTest {

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @MockBean
    private UserService userService;

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableService tableService;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.save(CONTAINER_1);
        databaseRepository.saveAll(List.of(DATABASE_1));
    }

    @Test
    @Transactional
    public void create_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            UserNotFoundException, TableNotFoundException, DatabaseNotFoundException, TableExistsException,
            SearchServiceException, SearchServiceConnectionException, OntologyNotFoundException,
            SemanticEntityNotFoundException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("New Table")
                .description("A wonderful table")
                .isPublic(true)
                .isSchemaPublic(true)
                .columns(List.of(CreateTableColumnDto.builder()
                                .name("id")
                                .nullAllowed(false)
                                .type(ColumnTypeDto.BIGINT)
                                .build(),
                        CreateTableColumnDto.builder()
                                .name("date")
                                .nullAllowed(true)
                                .type(ColumnTypeDto.DATE)
                                .build()))
                .constraints(CreateTableConstraintsDto.builder()
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
                .createTable(DATABASE_1_ID, request);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Table response = tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        assertNotNull(response.getId());
        assertEquals(request.getColumns().size(), response.getColumns().size());
        final TableColumn id = response.getColumns().get(0);
        assertEquals("id", id.getName());
        assertEquals("id", id.getInternalName());
        assertFalse(id.getIsNullAllowed());
        assertEquals(TableColumnType.BIGINT, id.getColumnType());
        final TableColumn date = response.getColumns().get(1);
        assertEquals("date", date.getName());
        assertEquals("date", date.getInternalName());
        assertEquals(TableColumnType.DATE, date.getColumnType());
        assertTrue(date.getIsNullAllowed());
        assertNotNull(response.getConstraints());
        final List<Unique> uniques = response.getConstraints().getUniques();
        assertEquals(request.getConstraints().getUniques().size(), uniques.size());
        assertNotNull(uniques.get(0).getName());
        assertEquals(request.getName(), uniques.get(0).getTable().getName());
        final List<PrimaryKey> primaryKeys = response.getConstraints().getPrimaryKey();
        assertEquals(request.getConstraints().getPrimaryKey().size(), primaryKeys.size());
        assertEquals(request.getConstraints().getPrimaryKey().toArray()[0], primaryKeys.get(0).getColumn().getInternalName());
        final Set<String> checks = response.getConstraints().getChecks();
        assertEquals(request.getConstraints().getChecks().size(), checks.size());
        final List<ForeignKey> foreignKeys = response.getConstraints().getForeignKeys();
        assertEquals(request.getConstraints().getForeignKeys().size(), foreignKeys.size());
    }

    @Test
    public void findColumnById_succeeds() throws MalformedException {

        /* test */
        final TableColumn response = tableService.findColumnById(TABLE_1, COLUMN_1_1_ID);
        assertEquals(TABLE_1_COLUMNS.get(0), response);
    }

    @Test
    public void findColumnById_fails() {

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.findColumnById(TABLE_1, UUID.randomUUID());
        });
    }

    @Test
    public void updateTable_succeeds() throws TableNotFoundException, SearchServiceException, DataServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_3_BRIEF_DTO);

        /* test */
        final Table response = tableService.updateTable(TABLE_8, TABLE_8_UPDATE_DTO);
        assertTrue(response.getIsPublic());
        assertTrue(response.getIsSchemaPublic());
        assertNull(response.getDescription());
    }

    @Test
    public void updateTable_dataServiceConnection_fails() throws DataServiceException, DatabaseNotFoundException,
            DataServiceConnectionException {

        /* mock */
        doThrow(DataServiceConnectionException.class)
                .when(dataServiceGateway)
                .updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            tableService.updateTable(TABLE_8, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void updateTable_dataService_fails() throws DataServiceException, DatabaseNotFoundException,
            DataServiceConnectionException {

        /* mock */
        doThrow(DataServiceException.class)
                .when(dataServiceGateway)
                .updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);

        /* test */
        assertThrows(DataServiceException.class, () -> {
            tableService.updateTable(TABLE_8, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void updateTable_searchServiceConnection_fails() throws DataServiceException, DatabaseNotFoundException,
            DataServiceConnectionException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            tableService.updateTable(TABLE_8, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void updateTable_searchService_fails() throws DataServiceException, DatabaseNotFoundException,
            DataServiceConnectionException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        doThrow(SearchServiceException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            tableService.updateTable(TABLE_8, TABLE_8_UPDATE_DTO);
        });
    }

}
