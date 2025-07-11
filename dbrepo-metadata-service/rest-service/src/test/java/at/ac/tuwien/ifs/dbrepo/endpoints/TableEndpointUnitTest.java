package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.EntityDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.TableColumnEntityDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableEndpointUnitTest extends BaseTest {

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private TableService tableService;

    @MockBean
    private UserService userService;

    @MockBean
    private EntityService entityService;

    @Autowired
    private TableEndpoint tableEndpoint;

    public static Stream<Arguments> needNothing_parameters() {
        return EndpointValidator.NEED_NOTHING.stream()
                .map(Arguments::arguments);
    }

    public static Stream<Arguments> needSize_parameters() {
        return EndpointValidator.NEED_SIZE.stream()
                .map(Arguments::arguments);
    }

    public static Stream<Arguments> canHaveSize_parameters() {
        return EndpointValidator.CAN_HAVE_SIZE.stream()
                .map(Arguments::arguments);
    }

    public static Stream<Arguments> canHaveSizeAndD_parameters() {
        return EndpointValidator.CAN_HAVE_SIZE_AND_D.stream()
                .map(Arguments::arguments);
    }

    @BeforeAll
    public static void beforeAll() {
        /* init Apache Jena */
        JenaSystem.init();
    }

    @Test
    @WithAnonymousUser
    public void list_publicDataPrivateSchemaAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_list(DATABASE_3_ID, DATABASE_3, null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void list_publicDataPublicSchemaAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        generic_list(DATABASE_4_ID, DATABASE_4, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_publicHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(DATABASE_3_ID, null, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_publicHasRole_succeeds() throws NotAllowedException,
            UserNotFoundException, DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        final ResponseEntity<List<TableBriefDto>> response = generic_list(DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableBriefDto> body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void list_publicDataPrivateSchemaNoRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_list(DATABASE_3_ID, DATABASE_3, USER_4_PRINCIPAL, USER_4, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_publicAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_create(DATABASE_3_ID, null, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_publicNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicDecimalColumnSizeTooSmall_fails() {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.DECIMAL)
                        .size(-1L) // <<<
                        .d(0L)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicDecimalColumnDTooSmall_fails() {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.DECIMAL)
                        .size(0L)
                        .d(-1L) // <<<
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @ParameterizedTest
    @MethodSource("canHaveSize_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicOptionalSizeNone_succeeds(ColumnTypeDto columnType) throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException, TableNotFoundException,
            DataServiceConnectionException, MalformedException, DataServiceException, DatabaseNotFoundException,
            AccessNotFoundException, OntologyNotFoundException, TableExistsException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(null) // <<<
                        .d(null) // <<<
                        .build()))
                .constraints(null)
                .build();

        /* mock */
        when(tableService.createTable(DATABASE_3, request, USER_1_PRINCIPAL))
                .thenReturn(TABLE_1) /* some table */;

        /* test */
        if (EndpointValidator.NEED_SIZE.contains(columnType)) {
            assertThrows(MalformedException.class, () -> {
                generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
            });
        } else {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        }
    }

    @ParameterizedTest
    @MethodSource("canHaveSize_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicOptionalSize_succeeds(ColumnTypeDto columnType) throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException, TableNotFoundException,
            DataServiceConnectionException, MalformedException, DataServiceException, DatabaseNotFoundException,
            AccessNotFoundException, OntologyNotFoundException, TableExistsException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(40L)
                        .d(10L)
                        .build()))
                .constraints(null)
                .build();

        /* mock */
        when(tableService.createTable(DATABASE_3, request, USER_1_PRINCIPAL))
                .thenReturn(TABLE_1) /* some table */;

        /* test */
        generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @ParameterizedTest
    @MethodSource("needNothing_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicNeedNothing_succeeds(ColumnTypeDto columnType) throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException, TableNotFoundException,
            DataServiceConnectionException, MalformedException, DataServiceException, DatabaseNotFoundException,
            AccessNotFoundException, OntologyNotFoundException, TableExistsException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .nullAllowed(false)
                        .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .uniques(List.of(List.of("ID")))
                        .build())
                .build();

        /* mock */
        when(tableService.createTable(DATABASE_3, request, USER_1_PRINCIPAL))
                .thenReturn(TABLE_1) /* some table */;

        /* test */
        generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @ParameterizedTest
    @MethodSource("needSize_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicNeedSize_succeeds(ColumnTypeDto columnType) throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException, TableNotFoundException,
            DataServiceConnectionException, MalformedException, DataServiceException, DatabaseNotFoundException,
            AccessNotFoundException, OntologyNotFoundException, TableExistsException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(40L)
                        .d(10L)
                        .build()))
                .constraints(null)
                .build();

        /* mock */
        when(tableService.createTable(DATABASE_3, request, USER_1_PRINCIPAL))
                .thenReturn(TABLE_1) /* some table */;

        /* test */
        generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @ParameterizedTest
    @MethodSource("needSize_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicNeedSizeNone_fails(ColumnTypeDto columnType) {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(null) // <<<
                        .d(10L)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @ParameterizedTest
    @MethodSource("canHaveSizeAndD_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicCanHaveSizeAndDSizeNone_fails(ColumnTypeDto columnType) {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(null) // <<<
                        .d(0L)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @ParameterizedTest
    @MethodSource("canHaveSizeAndD_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicCanHaveSizeAndDDNone_fails(ColumnTypeDto columnType) {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(0L)
                        .d(null) // <<<
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @ParameterizedTest
    @MethodSource("canHaveSizeAndD_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicCanHaveSizeAndDBothNone_succeeds(ColumnTypeDto columnType) throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException,
            DataServiceConnectionException, TableNotFoundException, MalformedException, DataServiceException,
            DatabaseNotFoundException, AccessNotFoundException, OntologyNotFoundException, TableExistsException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(null) // <<<
                        .d(null) // <<<
                        .build()))
                .constraints(null)
                .build();

        /* mock */
        when(tableService.createTable(DATABASE_3, request, USER_1_PRINCIPAL))
                .thenReturn(TABLE_1) /* some table */;

        /* test */
        generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicHasMultipleSerial_fails() {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                                .name("ID")
                                .type(ColumnTypeDto.SERIAL)
                                .nullAllowed(false)
                                .build(),
                        CreateTableColumnDto.builder()
                                .name("Counter")
                                .type(ColumnTypeDto.SERIAL)
                                .nullAllowed(false)
                                .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .uniques(List.of(List.of("ID"),
                                List.of("Counter")))
                        .build())
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicSerialNullAllowed_fails() {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.SERIAL)
                        .nullAllowed(true) // <<<
                        .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .uniques(List.of(List.of("ID")))
                        .build())
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @ParameterizedTest
    @MethodSource("canHaveSizeAndD_parameters")
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicCanHaveSizeAndDBothNotNone_succeeds(ColumnTypeDto columnType) throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException,
            DataServiceConnectionException, TableNotFoundException, MalformedException, DataServiceException,
            DatabaseNotFoundException, AccessNotFoundException, OntologyNotFoundException, TableExistsException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(columnType)
                        .size(0L) // <<<
                        .d(0L) // <<<
                        .build()))
                .constraints(null)
                .build();

        /* mock */
        when(tableService.createTable(DATABASE_3, request, USER_1_PRINCIPAL))
                .thenReturn(TABLE_1) /* some table */;

        /* test */
        generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_PRINCIPAL, USER_1, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void findById_publicDataPublicSchemaAnonymous_succeeds() throws UserNotFoundException,
            TableNotFoundException, NotAllowedException, DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        generic_findById(DATABASE_4_ID, DATABASE_4, TABLE_9_ID, TABLE_9, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_publicHasRoleTableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_findById(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, null, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_publicHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_findById(DATABASE_3_ID, null, TABLE_8_ID, TABLE_8, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_publicHasRole_succeeds() throws UserNotFoundException,
            TableNotFoundException, NotAllowedException, DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        final ResponseEntity<TableDto> response = generic_findById(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final TableDto body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findById_publicNoRole_succeeds() throws UserNotFoundException,
            TableNotFoundException, NotAllowedException, DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        generic_findById(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_PRINCIPAL, USER_1, null);
    }

    @Test
    @WithAnonymousUser
    public void analyseTable_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTable_generic(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {"table-semantic-analyse"})
    public void analyseTable_notOwner_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            analyseTable_generic(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTable_generic(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"table-semantic-analyse"})
    public void findAll_hasRole_succeeds() throws MalformedException, TableNotFoundException,
            DatabaseNotFoundException, NotAllowedException {

        /* test */
        analyseTable_generic(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void analyseTableColumn_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), TABLE_1_COLUMNS.get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void analyseTableColumn_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), TABLE_1_COLUMNS.get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"table-semantic-analyse"})
    public void analyseTableColumn_hasRole_succeeds() throws MalformedException, TableNotFoundException,
            DatabaseNotFoundException {

        /* test */
        analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), TABLE_1_COLUMNS.get(0));
    }

    @Test
    @WithAnonymousUser
    public void update_publicAnonymous_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .conceptUri(CONCEPT_2_URI)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_updateColumn(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, TABLE_8_COLUMNS.get(0).getId(),
                    TABLE_8_COLUMNS.get(0), null, null, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleNoAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .conceptUri(CONCEPT_2_URI)
                .build();

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            generic_updateColumn(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, TABLE_8_COLUMNS.get(0).getId(),
                    TABLE_8_COLUMNS.get(0), USER_1_PRINCIPAL, USER_1, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleHasOnlyReadAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .conceptUri(CONCEPT_2_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_updateColumn(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, TABLE_8_COLUMNS.get(0).getId(),
                    TABLE_8_COLUMNS.get(0), USER_1_PRINCIPAL, USER_1, request, DATABASE_3_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleHasOwnWriteAccess_succeeds() throws MalformedException, DataServiceException,
            NotAllowedException, DataServiceConnectionException, UserNotFoundException, TableNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        generic_updateColumn(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, TABLE_8_COLUMNS.get(0).getId(),
                TABLE_8_COLUMNS.get(0), USER_1_PRINCIPAL, USER_1, request, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleForeignHasOwnWriteAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_updateColumn(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, TABLE_8_COLUMNS.get(0).getId(),
                    TABLE_8_COLUMNS.get(0), USER_2_PRINCIPAL, USER_2, request, DATABASE_3_USER_2_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicTableNotFound_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_updateColumn(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, null, TABLE_8_COLUMNS.get(0).getId(),
                    TABLE_8_COLUMNS.get(0), USER_1_PRINCIPAL, USER_1, request, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleForeignHasAllWriteAccess_succeeds() throws MalformedException, DataServiceException,
            NotAllowedException, DataServiceConnectionException, UserNotFoundException, TableNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        generic_updateColumn(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, TABLE_8_COLUMNS.get(0).getId(),
                TABLE_8_COLUMNS.get(0), USER_2_PRINCIPAL, USER_2, request, DATABASE_3_USER_2_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void update_privateAnonymous_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_updateColumn(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, TABLE_1_COLUMNS.get(0).getId(),
                    TABLE_1_COLUMNS.get(0), null, null, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleNoAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            generic_updateColumn(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, TABLE_1_COLUMNS.get(0).getId(),
                    TABLE_1_COLUMNS.get(0), USER_1_PRINCIPAL, USER_1, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleHasOnlyReadAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_updateColumn(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, TABLE_1_COLUMNS.get(0).getId(),
                    TABLE_1_COLUMNS.get(0), USER_1_PRINCIPAL, USER_1, request, DATABASE_1.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleHasOwnWriteAccess_succeeds() throws MalformedException, DataServiceException,
            NotAllowedException, DataServiceConnectionException, UserNotFoundException, TableNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        generic_updateColumn(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, TABLE_1_COLUMNS.get(0).getId(),
                TABLE_1_COLUMNS.get(0), USER_1_PRINCIPAL, USER_1, request, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleForeignHasOwnWriteAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_updateColumn(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, TABLE_1_COLUMNS.get(0).getId(),
                    TABLE_1_COLUMNS.get(0), USER_2_PRINCIPAL, USER_2, request, DATABASE_1_USER_2_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateTableNotFound_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .build();

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_updateColumn(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, null, TABLE_1_COLUMNS.get(0).getId(),
                    TABLE_1_COLUMNS.get(0), USER_2_PRINCIPAL, USER_2, request, DATABASE_1_USER_2_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleForeignHasAllWriteAccess_succeeds() throws MalformedException, DataServiceException,
            NotAllowedException, DataServiceConnectionException, UserNotFoundException, TableNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(null)
                .conceptUri(null)
                .build();

        /* test */
        generic_updateColumn(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, TABLE_1_COLUMNS.get(0).getId(),
                TABLE_1_COLUMNS.get(0), USER_2_PRINCIPAL, USER_2, request, DATABASE_1_USER_2_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_privateHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(DATABASE_1_ID, null, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_privateHasRole_succeeds() throws NotAllowedException,
            UserNotFoundException, DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        final ResponseEntity<List<TableBriefDto>> response = generic_list(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableBriefDto> body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithAnonymousUser
    public void create_privateAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_create(DATABASE_1_ID, null, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_privateNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_privateDatabasePrivateDataPublicSchemaAnonymous_fails() throws UserNotFoundException,
            TableNotFoundException, NotAllowedException, DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        generic_findById(DATABASE_1_ID, DATABASE_1, TABLE_2_ID, TABLE_2, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findById_publicSchemaNotOwnerNoAccess_succeeds() throws UserNotFoundException,
            TableNotFoundException, NotAllowedException, DatabaseNotFoundException, AccessNotFoundException {

        /* test */
        final ResponseEntity<TableDto> response = generic_findById(DATABASE_1_ID, DATABASE_1, TABLE_2_ID, TABLE_2, USER_4_PRINCIPAL, USER_4, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final TableDto body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_privateHasRoleTableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_findById(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, null, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_privateHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_findById(DATABASE_1_ID, null, TABLE_1_ID, TABLE_1, USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_privateHasRole_succeeds() throws UserNotFoundException,
            TableNotFoundException, NotAllowedException, DatabaseNotFoundException, AccessNotFoundException {
        /* test */
        final ResponseEntity<TableDto> response = generic_findById(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1,
                USER_1_PRINCIPAL, USER_1, DATABASE_1.getAccesses().get(0));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final TableDto body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void delete_privateNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_delete(USER_4_PRINCIPAL, TABLE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table"})
    public void delete_succeeds() throws NotAllowedException, DataServiceException, DataServiceConnectionException,
            TableNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {

        /* test */
        generic_delete(USER_1_PRINCIPAL, TABLE_1);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-table"})
    public void delete_foreign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_delete(USER_3_PRINCIPAL, TABLE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_5_USERNAME, authorities = {"delete-foreign-table"})
    public void delete_foreign_succeeds() throws NotAllowedException, DataServiceConnectionException,
            TableNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DataServiceException, DashboardServiceException,
            DashboardServiceConnectionException {

        /* test */
        generic_delete(USER_5_PRINCIPAL, TABLE_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-table"})
    public void delete_hasIdentifiers_fails() {
        final Table response = Table.builder()
                .identifiers(List.of(IDENTIFIER_1))
                .owner(USER_1)
                .ownedBy(USER_1_ID)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_delete(USER_1_PRINCIPAL, response);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-table"})
    public void update_succeeds() throws TableNotFoundException, SearchServiceException, NotAllowedException,
            DataServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {
        final TableUpdateDto request = TableUpdateDto.builder()
                .isPublic(true)
                .isSchemaPublic(true)
                .description("Debug")
                .build();

        /* test */
        final ResponseEntity<TableBriefDto> response = generic_update(request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"update-table"})
    public void update_notOwner_fails() {
        final TableUpdateDto request = TableUpdateDto.builder()
                .isPublic(true)
                .isSchemaPublic(true)
                .description("Debug")
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"update-table-statistic"})
    public void updateStatistic_notOwner_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_updateStatistic(USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-table-statistic"})
    public void updateStatistic_succeeds() throws TableNotFoundException, SearchServiceException, MalformedException,
            NotAllowedException, DataServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException {

        /* test */
        final ResponseEntity<Void> response = generic_updateStatistic(USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system", "update-table-statistic"})
    public void updateStatistic_internalUser_succeeds() throws TableNotFoundException, SearchServiceException,
            MalformedException, NotAllowedException, DataServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException {

        /* test */
        final ResponseEntity<Void> response = generic_updateStatistic(USER_LOCAL_ADMIN_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void analyseTable_generic(UUID databaseId, Database database, UUID tableId, Table table, Principal principal)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException, NotAllowedException {

        /* mock */
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        when(tableService.findById(database, tableId))
                .thenReturn(table);
        when(entityService.suggestByTable(table))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<EntityDto>> response = tableEndpoint.analyseTable(databaseId, tableId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<EntityDto> body = response.getBody();
        assertNotNull(body);
    }

    public void analyseTableColumn_generic(UUID databaseId, UUID tableId, UUID columnId, TableColumn tableColumn)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(entityService.suggestByColumn(tableColumn))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<TableColumnEntityDto>> response = tableEndpoint.analyseTableColumn(databaseId,
                tableId, columnId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableColumnEntityDto> body = response.getBody();
        assertNotNull(body);
    }

    protected ResponseEntity<List<TableBriefDto>> generic_list(UUID databaseId, Database database, Principal principal,
                                                               User user, DatabaseAccess access)
            throws NotAllowedException, DatabaseNotFoundException, AccessNotFoundException, UserNotFoundException {

        /* mock */
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
            log.trace("mock {} table(s)", database.getTables().size());
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
            log.trace("mock 0 tables");
        }
        if (access != null) {
            when(accessService.find(database, user))
                    .thenReturn(access);
        } else {
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(database, user);
        }

        /* test */
        return tableEndpoint.list(databaseId, principal);
    }

    protected ResponseEntity<TableBriefDto> generic_create(UUID databaseId, Database database, CreateTableDto data,
                                                           Principal principal, User user, DatabaseAccess access)
            throws MalformedException, NotAllowedException, DataServiceException, DataServiceConnectionException,
            UserNotFoundException, DatabaseNotFoundException, AccessNotFoundException, TableNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, OntologyNotFoundException,
            SemanticEntityNotFoundException, DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        if (principal != null) {
            when(userService.findByUsername(user.getUsername()))
                    .thenReturn(user);
        }
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
        }
        if (access != null) {
            when(accessService.find(database, user))
                    .thenReturn(access);
        } else {
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(database, user);
        }

        /* test */
        return tableEndpoint.create(databaseId, data, principal);
    }

    protected ResponseEntity<TableDto> generic_findById(UUID databaseId, Database database, UUID tableId,
                                                        Table table, Principal principal, User user,
                                                        DatabaseAccess access) throws TableNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, UserNotFoundException, NotAllowedException {

        /* mock */
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
        }
        if (table != null) {
            when(tableService.findById(any(Database.class), eq(tableId)))
                    .thenReturn(table);
        } else {
            doThrow(TableNotFoundException.class)
                    .when(tableService)
                    .findById(any(Database.class), eq(tableId));
        }
        if (user != null) {
            when(userService.findByUsername(user.getUsername()))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findByUsername(anyString());
        }
        if (access != null) {
            when(accessService.find(any(Database.class), any(User.class)))
                    .thenReturn(access);
        } else {
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(any(Database.class), any(User.class));
        }

        /* test */
        return tableEndpoint.findById(databaseId, tableId, principal);
    }

    protected ResponseEntity<?> generic_delete(Principal principal, Table table) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, TableNotFoundException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(tableService.findById(any(Database.class), any(UUID.class)))
                .thenReturn(table);

        /* test */
        return tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID, principal);
    }

    protected ResponseEntity<ColumnDto> generic_updateColumn(UUID databaseId, Database database, UUID tableId,
                                                             Table table, UUID columnId, TableColumn column,
                                                             Principal principal, User user,
                                                             ColumnSemanticsUpdateDto data, DatabaseAccess access)
            throws DataServiceException, DataServiceConnectionException, MalformedException, NotAllowedException,
            UserNotFoundException, TableNotFoundException, DatabaseNotFoundException, AccessNotFoundException,
            SearchServiceException, SearchServiceConnectionException, OntologyNotFoundException,
            SemanticEntityNotFoundException {

        /* mock */
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        if (table != null) {
            when(tableService.findById(database, tableId))
                    .thenReturn(table);
            when(tableService.update(column, data))
                    .thenReturn(column);
        } else {
            doThrow(DataServiceException.class)
                    .when(tableService)
                    .update(column, data);
            doThrow(TableNotFoundException.class)
                    .when(tableService)
                    .findById(database, tableId);
        }
        if (principal != null) {
            log.trace("mock user {}", user);
            when(userService.findByUsername(user.getUsername()))
                    .thenReturn(user);
        }
        if (access != null) {
            log.trace("mock access {}", access);
            when(accessService.find(any(Database.class), any(User.class)))
                    .thenReturn(access);
        } else {
            log.trace("mock no access");
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(database, user);
        }

        /* test */
        return tableEndpoint.updateColumn(databaseId, tableId, columnId, data, principal);
    }

    protected ResponseEntity<TableBriefDto> generic_update(TableUpdateDto data, Principal caller)
            throws TableNotFoundException, SearchServiceException, NotAllowedException, DataServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(tableService.updateTable(any(Table.class), eq(data)))
                .thenReturn(TABLE_1);

        /* test */
        return tableEndpoint.update(DATABASE_1_ID, TABLE_1_ID, data, caller);
    }

    protected ResponseEntity<Void> generic_updateStatistic(Principal caller)
            throws TableNotFoundException, SearchServiceException, NotAllowedException, DataServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException,
            MalformedException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        doNothing()
                .when(tableService)
                .updateStatistics(TABLE_1);

        /* test */
        return tableEndpoint.updateStatistic(DATABASE_1_ID, TABLE_1_ID, caller);
    }
}
