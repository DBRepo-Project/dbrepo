package at.tuwien.service;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.primaryKey.PrimaryKey;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.repository.ContainerRepository;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.repository.LicenseRepository;
import at.tuwien.repository.UserRepository;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class TableServicePersistenceTest extends AbstractUnitTest {

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
        genesis();
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.save(CONTAINER_1);
        databaseRepository.saveAll(List.of(DATABASE_1));
    }

    @Test
    @Transactional
    public void create_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            UserNotFoundException, TableNotFoundException, DatabaseNotFoundException, TableExistsException, SearchServiceException, SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        final TableCreateDto request = TableCreateDto.builder()
                .name("New Table")
                .description("A wonderful table")
                .columns(List.of(ColumnCreateDto.builder()
                                .name("id")
                                .nullAllowed(false)
                                .type(ColumnTypeDto.BIGINT)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("date")
                                .nullAllowed(true)
                                .type(ColumnTypeDto.DATE)
                                .dfid(3L)
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
                .createTable(DATABASE_1_ID, request);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

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
        assertNotNull(date.getDateFormat());
        assertEquals(3L, date.getDateFormat().getId());
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

}
