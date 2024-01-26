package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.SortType;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.service.impl.QueryServiceImpl;
import lombok.extern.log4j.Log4j2;
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
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class TableDataEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private QueryServiceImpl queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private TableService tableService;

    @Autowired
    private TableDataEndpoint dataEndpoint;

    @Test
    @WithAnonymousUser
    public void import_publicAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_import(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_publicNoRoleRead_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_import(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_2_ID,
                    DATABASE_1_USER_1_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_publicNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_import(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_2_ID,
                    DATABASE_1_USER_1_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void import_privateAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_import(DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_privateNoRoleRead_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_import(DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_ID,
                    DATABASE_2_USER_1_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_privateNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_import(DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_ID,
                    DATABASE_2_USER_1_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void import_publicAnonymous_succeeds() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_import(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void import_publicWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AccessDeniedException, DataDbSidecarException {

        /* test */
        generic_import(DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_ID,
                DATABASE_3_USER_1_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void import_privateWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AccessDeniedException, DataDbSidecarException {

        /* test */
        generic_import(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_1_ID,
                DATABASE_1_USER_1_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void insert_publicAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_insert(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_ID, null,
                    TABLE_1_CSV_DTO, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_publicNoRoleRead_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_insert(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_ID,
                    DATABASE_1_USER_1_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_publicNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_insert(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_ID,
                    DATABASE_1_USER_1_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void insert_privateAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_insert(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_ID, null,
                    TABLE_1_CSV_DTO, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_privateNoRoleRead_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_insert(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_ID,
                    DATABASE_2_USER_1_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_privateNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_insert(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_ID,
                    DATABASE_2_USER_1_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insert_publicWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, AccessDeniedException {

        /* test */
        generic_insert(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_ID,
                DATABASE_3_USER_1_WRITE_ALL_ACCESS, TABLE_8_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insert_privateWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, AccessDeniedException {

        /* test */
        generic_insert(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_ID, DATABASE_1_USER_1_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insert_privateDataNull_fails() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, AccessDeniedException {

        /* test */
        generic_insert(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_ID, DATABASE_1_USER_1_WRITE_ALL_ACCESS, null, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousPageNull_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, null,
                    3L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousSizeNull_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, 3L,
                    null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousPageNegative_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, -3L,
                    3L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousSizeNegative_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, 3L,
                    -3L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousPageZero_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, 0L,
                    0L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_getAll(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null, null, null, null, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void getAll_privateNoRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_getAll(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_4_ID, DATABASE_1_USER_1_READ_ACCESS, USER_4_PRINCIPAL, null, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void getCount_privateNoRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_getCount(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_4_ID, DATABASE_1_USER_1_READ_ACCESS, USER_4_PRINCIPAL, null);
        });
    }

    public static Stream<Arguments> getAll_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous", DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, null, null, null,
                        null, null, null, null, null),
                Arguments.arguments("public read", DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8,
                        USER_1_ID,
                        DATABASE_3_USER_1_READ_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("public write-own", DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_ID,
                        DATABASE_3_USER_1_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("public write-all", DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_ID,
                        DATABASE_3_USER_1_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private read", DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1,
                        USER_1_ID,
                        DATABASE_1_USER_1_READ_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private write-own", DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_1_ID,
                        DATABASE_1_USER_1_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private write-all", DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_1_ID,
                        DATABASE_1_USER_1_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null)
        );
    }

    @ParameterizedTest
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    @MethodSource("getAll_succeeds_parameters")
    public void getAll_succeeds(String test, Long databaseId, Long tableId, Database database, Table table, UUID userId,
                                DatabaseAccess access, Principal principal, Instant timestamp, Long page, Long size,
                                SortType sortDirection, String sortColumn) throws UserNotFoundException,
            TableNotFoundException, SortException, TableMalformedException, NotAllowedException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException, AccessDeniedException {

        /* test */
        generic_getAll(databaseId, tableId, database, table, userId, access, principal, timestamp,
                page, size, sortDirection, sortColumn);
    }

    public static Stream<Arguments> getCount_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous", DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, null, null, null, null),
                Arguments.arguments("public read", DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8,
                        USER_1_USERNAME,
                        DATABASE_3_USER_1_READ_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("public write-own", DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_USERNAME,
                        DATABASE_3_USER_1_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("public write-all", DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_USERNAME,
                        DATABASE_3_USER_1_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("private read", DATABASE_2_ID, TABLE_8_ID, DATABASE_2, TABLE_8,
                        USER_1_USERNAME,
                        DATABASE_2_USER_1_READ_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("private write-own", DATABASE_2_ID, TABLE_8_ID, DATABASE_2,
                        TABLE_8, USER_2_USERNAME,
                        DATABASE_2_USER_1_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("private write-all", DATABASE_2_ID, TABLE_8_ID, DATABASE_2,
                        TABLE_8, USER_2_USERNAME,
                        DATABASE_2_USER_1_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null)
        );
    }

    @ParameterizedTest
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    @MethodSource("getAll_succeeds_parameters")
    public void getCount_succeeds(String test, Long databaseId, Long tableId, Database database, Table table,
                                  UUID userId, DatabaseAccess access, Principal principal, Instant timestamp)
            throws UserNotFoundException, TableNotFoundException, QueryStoreException, TableMalformedException,
            NotAllowedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AccessDeniedException {

        /* test */
        generic_getCount(databaseId, tableId, database, table, userId, access, principal, timestamp);
    }


    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void generic_import(Long databaseId, Database database, Long tableId, Table table, UUID userId,
                               DatabaseAccess access, Principal principal) throws DatabaseNotFoundException,
            TableNotFoundException, AccessDeniedException, TableMalformedException, NotAllowedException, DataDbSidecarException {
        final ImportDto request = ImportDto.builder().location("test:csv/csv_01.csv").build();

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(tableService.find(databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, userId))
                .thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.importCsv(databaseId, tableId, request, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_insert(Long databaseId, Long tableId, Database database, Table table, UUID userId,
                               DatabaseAccess access, TableCsvDto data, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, AccessDeniedException, TableMalformedException,
            NotAllowedException {

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(tableService.find(databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, userId))
                .thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.insert(databaseId, tableId, data, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_getAll(Long databaseId, Long tableId, Database database, Table table, UUID userId,
                               DatabaseAccess access, Principal principal, Instant timestamp, Long page, Long size,
                               SortType sortDirection, String sortColumn) throws TableMalformedException,
            NotAllowedException, PaginationException, TableNotFoundException, SortException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, AccessDeniedException {

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(tableService.find(databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, userId))
                .thenReturn(access);
        when(queryService.tableFindAll(databaseId, tableId, timestamp, page, size, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = dataEndpoint.getAll(databaseId, tableId,
                principal, timestamp, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    public void generic_getCount(Long databaseId, Long tableId, Database database, Table table, UUID userId,
                                 DatabaseAccess access, Principal principal, Instant timestamp)
            throws TableMalformedException, NotAllowedException, TableNotFoundException, QueryStoreException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, AccessDeniedException {

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(tableService.find(databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, userId))
                .thenReturn(access);
        when(queryService.tableCount(databaseId, tableId, timestamp, principal))
                .thenReturn(QUERY_1_RESULT_NUMBER);

        /* test */
        final ResponseEntity<Long> response = dataEndpoint.getCount(databaseId, tableId,
                principal, timestamp);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody());
    }

}
