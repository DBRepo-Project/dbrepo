package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.CreateForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnEnum;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnSet;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceUnitTest extends BaseTest {

    @MockitoBean
    private DatabaseRepository databaseRepository;

    @MockitoBean
    private SearchServiceGateway searchServiceGateway;

    @MockitoBean
    private DatabaseCacheRepository databaseCacheRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DataServiceGateway dataServiceGateway;

    @Autowired
    private TableService tableService;

    public static Stream<Arguments> updateStatistics_fails_parameters() {
        return Stream.of(
                Arguments.of("name_invalid", TableStatisticDto.builder()
                                .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                        .name("")
                                        .min(BigDecimal.ZERO)
                                        .max(BigDecimal.ZERO)
                                        .mean(BigDecimal.ZERO)
                                        .median(BigDecimal.ZERO)
                                        .stdDev(BigDecimal.ZERO)
                                        .build())))
                                .build(),
                        MalformedException.class)
        );
    }

    public static Stream<Arguments> updateStatistics_succeeds_parameters() {
        return Stream.of(
                Arguments.of("min_large", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.valueOf(Long.MAX_VALUE))
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("min_small", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.valueOf(Long.MIN_VALUE))
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("max_large", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.valueOf(Long.MAX_VALUE))
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("max_small", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.valueOf(Long.MIN_VALUE))
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("mean_large", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.valueOf(Long.MAX_VALUE))
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("mean_small", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.valueOf(Long.MIN_VALUE))
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("median_large", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.valueOf(Long.MAX_VALUE))
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("median_small", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.valueOf(Long.MIN_VALUE))
                                .stdDev(BigDecimal.ZERO)
                                .build())))
                        .build()),
                Arguments.of("stddev_large", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.valueOf(Long.MAX_VALUE))
                                .build())))
                        .build()),
                Arguments.of("stddev_small", TableStatisticDto.builder()
                        .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                                .name(COLUMN_8_1_INTERNAL_NAME)
                                .min(BigDecimal.ZERO)
                                .max(BigDecimal.ZERO)
                                .mean(BigDecimal.ZERO)
                                .median(BigDecimal.ZERO)
                                .stdDev(BigDecimal.valueOf(Long.MIN_VALUE))
                                .build())))
                        .build())
        );
    }

    @Test
    public void findById_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));

        /* test */
        final Table response = tableService.findById(DATABASE_3, TABLE_8_ID);
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
            tableService.findById(DATABASE_3, TABLE_1_ID);
        });
    }

    @Test
    public void findByName_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_3_ID))
                .thenReturn(Optional.of(DATABASE_3));

        /* test */
        final Table response = tableService.findByName(DATABASE_3, TABLE_8_INTERNAL_NAME);
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
        assertThrows(TableNotFoundException.class, () -> {
            tableService.findByName(DATABASE_3, TABLE_1_INTERNAL_NAME);
        });
    }

    @Test
    public void updateStatistics_succeeds() throws TableNotFoundException, DataServiceException,
            DataServiceConnectionException, SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, MalformedException {

        /* mock */
        when(dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_STATISTIC_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        tableService.updateStatistics(TABLE_8);
    }

    @Test
    public void updateStatistics_searchServiceNotFound_fails() throws TableNotFoundException, DataServiceException,
            DataServiceConnectionException, SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_STATISTIC_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.updateStatistics(TABLE_8);
        });
    }

    @ParameterizedTest
    @MethodSource("updateStatistics_fails_parameters")
    public void updateStatistics_largeNumbers_fails(String name, TableStatisticDto statistic,
                                                    Class<? extends Exception> ex) throws TableNotFoundException,
            DataServiceException, DataServiceConnectionException, SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(statistic);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        assertThrows(ex, () -> {
            tableService.updateStatistics(TABLE_8);
        });
    }

    @ParameterizedTest
    @MethodSource("updateStatistics_succeeds_parameters")
    public void updateStatistics_largeNumbers_fails(String name, TableStatisticDto statistic) throws TableNotFoundException,
            DataServiceException, DataServiceConnectionException, SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, MalformedException {

        /* mock */
        when(dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(statistic);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        tableService.updateStatistics(TABLE_8);
    }

    @Test
    public void updateStatistics_searchServiceConnection_fails() throws TableNotFoundException, DataServiceException,
            DataServiceConnectionException, SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_STATISTIC_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            tableService.updateStatistics(TABLE_8);
        });
    }

    @Test
    public void updateStatistics_columnNotFound_fails() throws TableNotFoundException, DataServiceException,
            DataServiceConnectionException {
        final TableStatisticDto mock = TableStatisticDto.builder()
                .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                        .name("unknown_column")
                        .min(BigDecimal.valueOf(11.2))
                        .max(BigDecimal.valueOf(23.1))
                        .mean(BigDecimal.valueOf(13.5333))
                        .median(BigDecimal.valueOf(11.4))
                        .stdDev(BigDecimal.valueOf(4.2952))
                        .build())))
                .build();

        /* mock */
        when(dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(mock);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.updateStatistics(TABLE_8);
        });
    }

    @Test
    public void update_known_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            DatabaseNotFoundException, OntologyNotFoundException, SearchServiceConnectionException,
            SemanticEntityNotFoundException, DataServiceConnectionException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .conceptUri(CONCEPT_1_URI)
                .build();

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        tableService.update(TABLE_1_COLUMNS.get(0), request);
    }

    @Test
    public void update_unknown_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            DatabaseNotFoundException, OntologyNotFoundException, SearchServiceConnectionException,
            SemanticEntityNotFoundException, DataServiceConnectionException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .conceptUri(CONCEPT_1_URI)
                .build();

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        tableService.update(TABLE_1_COLUMNS.get(0), request);
    }

    @Test
    public void createTable_succeeds() throws DataServiceException, DataServiceConnectionException,
            UserNotFoundException, TableNotFoundException, DatabaseNotFoundException, TableExistsException,
            SearchServiceException, SearchServiceConnectionException, MalformedException, OntologyNotFoundException,
            SemanticEntityNotFoundException, NotAllowedException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doNothing()
                .when(dataServiceGateway)
                .createTable(eq(DATABASE_1_ID), any(CreateTableDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Table response = tableService.createTable(DATABASE_1, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
        assertEquals(TABLE_3_INTERNAL_NAME, response.getInternalName());
    }

    @Test
    public void createTable_nonStandardColumnNames_succeeds() throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, TableNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException,
            OntologyNotFoundException, SemanticEntityNotFoundException, NotAllowedException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("New Table")
                .description("A wonderful table")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("I Am Späshül")
                        .nullAllowed(true)
                        .type(ColumnTypeDto.TEXT)
                        .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .checks(Set.of())
                        .uniques(List.of(List.of("I Am Späshül")))
                        .foreignKeys(List.of())
                        .primaryKey(Set.of())
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doNothing()
                .when(dataServiceGateway)
                .createTable(eq(DATABASE_1_ID), any(CreateTableDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

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
    public void createTable_enumsSets_succeeds() throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, TableNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException,
            OntologyNotFoundException, SemanticEntityNotFoundException, NotAllowedException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("New Table")
                .description("A wonderful table")
                .columns(new LinkedList<>(List.of(CreateTableColumnDto.builder()
                                .name("sex")
                                .type(ColumnTypeDto.ENUM) // <<<
                                .enums(new LinkedList<>(List.of("male", "female", "other")))
                                .build(),
                        CreateTableColumnDto.builder()
                                .name("status")
                                .type(ColumnTypeDto.SET) // <<<
                                .sets(new LinkedList<>(List.of("single", "married", "divorced", "widowed")))
                                .build())))
                .constraints(CreateTableConstraintsDto.builder()
                        .checks(Set.of())
                        .uniques(List.of())
                        .foreignKeys(List.of())
                        .primaryKey(Set.of())
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doNothing()
                .when(dataServiceGateway)
                .createTable(eq(DATABASE_1_ID), any(CreateTableDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Table response = tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        final TableColumn column0 = response.getColumns().get(0);
        assertEquals("sex", column0.getInternalName());
        assertEquals(TableColumnType.ENUM, column0.getColumnType());
        assertEquals(List.of("male", "female", "other"), column0.getEnums().stream().map(ColumnEnum::getValue).toList());
        final TableColumn column1 = response.getColumns().get(1);
        assertEquals("status", column1.getInternalName());
        assertEquals(List.of("single", "married", "divorced", "widowed"), column1.getSets().stream().map(ColumnSet::getValue).toList());
    }

    @Test
    public void createTable_dateFormatNotFound_fails() throws DataServiceException, DataServiceConnectionException,
            UserNotFoundException, DatabaseNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException, NotAllowedException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("New Table")
                .description("A wonderful table")
                .columns(List.of(CreateTableColumnDto.builder()
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
                .thenReturn(USER_1_DTO);
        doNothing()
                .when(dataServiceGateway)
                .createTable(eq(DATABASE_1_ID), any(CreateTableDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void updateTable_succeeds() throws DataServiceException, DataServiceConnectionException,
            TableNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .updateTable(any(UUID.class), any(UUID.class), any(TableUpdateDto.class));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_3);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_3_ID);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_3_BRIEF_DTO);

        /* test */
        final Table response = tableService.updateTable(TABLE_8, TABLE_8_UPDATE_DTO);
        assertNotNull(response.getId());
    }

    @Test
    public void create_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            UserNotFoundException, TableNotFoundException, DatabaseNotFoundException, TableExistsException,
            SearchServiceException, SearchServiceConnectionException, OntologyNotFoundException,
            SemanticEntityNotFoundException, NotAllowedException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doNothing()
                .when(dataServiceGateway)
                .createTable(DATABASE_1_ID, TABLE_3_CREATE_DTO);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Table response = tableService.createTable(DATABASE_1, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
        assertNotNull(response.getId());
    }

    @Test
    public void create_dataServiceError_fails() throws DataServiceException, DataServiceConnectionException,
            UserNotFoundException, DatabaseNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException, NotAllowedException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(DataServiceException.class)
                .when(dataServiceGateway)
                .createTable(DATABASE_1_ID, TABLE_5_CREATE_DTO);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        assertThrows(DataServiceException.class, () -> {
            tableService.createTable(DATABASE_1, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void createTable_primaryKeyMalformed_fails() throws UserNotFoundException, NotAllowedException {
        final CreateTableDto request = CreateTableDto.builder()
                .name(TABLE_5_NAME)
                .description(TABLE_5_DESCRIPTION)
                .columns(TABLE_5_COLUMNS_CREATE)
                .constraints(CreateTableConstraintsDto.builder()
                        .foreignKeys(new LinkedList<>())
                        .checks(new LinkedHashSet<>())
                        .primaryKey(Set.of("i_do_not_exist"))
                        .uniques(new LinkedList<>())
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void createTable_uniquesMalformed_fails() throws UserNotFoundException, NotAllowedException {
        final CreateTableDto request = CreateTableDto.builder()
                .name(TABLE_5_NAME)
                .description(TABLE_5_DESCRIPTION)
                .columns(TABLE_5_COLUMNS_CREATE)
                .constraints(CreateTableConstraintsDto.builder()
                        .foreignKeys(new LinkedList<>())
                        .checks(new LinkedHashSet<>())
                        .primaryKey(new LinkedHashSet<>())
                        .uniques(List.of(List.of("i_do_not_exist")))
                        .build())
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.createTable(DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void createTable_foreignKeyMalformed_fails() throws UserNotFoundException, NotAllowedException {
        final CreateTableDto request = CreateTableDto.builder()
                .name(TABLE_5_NAME)
                .description(TABLE_5_DESCRIPTION)
                .columns(TABLE_5_COLUMNS_CREATE)
                .constraints(CreateTableConstraintsDto.builder()
                        .foreignKeys(List.of(CreateForeignKeyDto.builder()
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
                .thenReturn(USER_1_DTO);

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
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doNothing()
                .when(dataServiceGateway)
                .deleteTable(DATABASE_1_ID, TABLE_1_ID);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        tableService.deleteTable(TABLE_1);
    }

    @Test
    @Transactional
    public void delete_hasIdentifier_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, TableNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doNothing()
                .when(dataServiceGateway)
                .deleteTable(DATABASE_1_ID, TABLE_4_ID);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

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
            tableService.findByName(DATABASE_1, "i_do_not_exist");
        });
    }

    @Test
    public void delete_dataServiceNotFound_succeeds() throws TableNotFoundException, DataServiceException,
            DataServiceConnectionException, SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(TableNotFoundException.class)
                .when(dataServiceGateway)
                .deleteTable(DATABASE_1_ID, TABLE_4_ID);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        tableService.deleteTable(TABLE_4);
    }

}
