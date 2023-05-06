package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.MessageQueueListener;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.service.impl.QueryServiceImpl;
import com.rabbitmq.client.Channel;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableDataEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    /* keep */
    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    /* keep */
    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private QueryServiceImpl queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private IndexConfig indexInitializer;

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
        assertThrows(AccessDeniedException.class, () -> {
            generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_publicNoRoleRead_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_publicNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void import_privateAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_privateNoRoleRead_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void import_privateNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void import_publicAnonymous_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void import_publicWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_USERNAME,
                DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void import_privateWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_1_USERNAME,
                DATABASE_1_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void insert_publicAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME, null,
                    TABLE_1_CSV_DTO, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_publicNoRoleRead_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_RESEARCHER_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_publicNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void insert_privateAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME, null,
                    TABLE_1_CSV_DTO, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_privateNoRoleRead_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_RESEARCHER_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void insert_privateNoRoleWriteOwn_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insert_publicWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, TABLE_8_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insert_privateWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_USERNAME,
                DATABASE_1_RESEARCHER_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insert_privateDataNull_fails() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_USERNAME,
                DATABASE_1_RESEARCHER_WRITE_ALL_ACCESS, null, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousPageNull_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, null,
                    3L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousSizeNull_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, 3L,
                    null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousPageNegative_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, -3L,
                    3L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousSizeNegative_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, 3L,
                    -3L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_publicAnonymousPageZero_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null, null, 0L,
                    0L, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getAll_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null, null, null, null, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {})
    public void getAll_privateNoRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_3_USERNAME, DATABASE_1_RESEARCHER_READ_ACCESS, USER_3_PRINCIPAL, null, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {})
    public void getCount_privateNoRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_getCount(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_3_USERNAME, DATABASE_1_RESEARCHER_READ_ACCESS, USER_3_PRINCIPAL, null);
        });
    }

    public static Stream<Arguments> getAll_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, null, null, null,
                        null, null, null, null, null),
                Arguments.arguments("public read", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8,
                        USER_1_USERNAME,
                        DATABASE_3_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("public write-own", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_USERNAME,
                        DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("public write-all", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_USERNAME,
                        DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private read", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_1_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private write-own", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_1_USERNAME,
                        DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private write-all", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_1_USERNAME,
                        DATABASE_1_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null)
        );
    }

    @ParameterizedTest
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    @MethodSource("getAll_succeeds_parameters")
    public void getAll_succeeds(String test, Long containerId, Long databaseId, Long tableId, Database database,
                                Table table, String username, DatabaseAccess access, Principal principal,
                                Instant timestamp, Long page, Long size, SortType sortDirection, String sortColumn) throws UserNotFoundException, TableNotFoundException, QueryStoreException, SortException, TableMalformedException, NotAllowedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException {

        /* test */
        generic_getAll(containerId, databaseId, tableId, database, table, username, access, principal, timestamp,
                page, size, sortDirection, sortColumn);
    }

    public static Stream<Arguments> getCount_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, null, null, null, null),
                Arguments.arguments("public read", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8,
                        USER_1_USERNAME,
                        DATABASE_3_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("public write-own", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_USERNAME,
                        DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("public write-all", CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3,
                        TABLE_8, USER_1_USERNAME,
                        DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("private read", CONTAINER_1_ID, DATABASE_2_ID, TABLE_8_ID, DATABASE_2, TABLE_8,
                        USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("private write-own", CONTAINER_1_ID, DATABASE_2_ID, TABLE_8_ID, DATABASE_2,
                        TABLE_8, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("private write-all", CONTAINER_1_ID, DATABASE_2_ID, TABLE_8_ID, DATABASE_2,
                        TABLE_8, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null)
        );
    }

    @ParameterizedTest
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    @MethodSource("getAll_succeeds_parameters")
    public void getCount_succeeds(String test, Long containerId, Long databaseId, Long tableId, Database database,
                                  Table table, String username, DatabaseAccess access, Principal principal,
                                  Instant timestamp) throws UserNotFoundException, TableNotFoundException, QueryStoreException, SortException, TableMalformedException, NotAllowedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException {

        /* test */
        generic_getCount(containerId, databaseId, tableId, database, table, username, access, principal, timestamp);
    }


    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void generic_import(Long containerId, Long databaseId, Database database, Long tableId, Table table,
                               String username, DatabaseAccess access, Principal principal) throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException, UserNotFoundException, TableMalformedException, DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException, ContainerNotFoundException {
        final ImportDto request = ImportDto.builder().location("test:csv/csv_01.csv").build();

        /* mock */
        when(databaseService.find(containerId, databaseId)).thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId)).thenReturn(table);
        when(accessService.find(databaseId, username)).thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.importCsv(containerId, databaseId, tableId, request, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_insert(Long containerId, Long databaseId, Long tableId, Database database, Table table,
                               String username, DatabaseAccess access, TableCsvDto data, Principal principal) throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException, UserNotFoundException, TableMalformedException, DatabaseConnectionException, ImageNotSupportedException, ContainerNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId)).thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId)).thenReturn(table);
        when(accessService.find(databaseId, username)).thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.insert(containerId, databaseId, tableId, data, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_getAll(Long containerId, Long databaseId, Long tableId, Database database, Table table,
                               String username, DatabaseAccess access, Principal principal, Instant timestamp,
                               Long page, Long size, SortType sortDirection, String sortColumn) throws UserNotFoundException, TableMalformedException, NotAllowedException, PaginationException, TableNotFoundException, QueryStoreException, SortException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId)).thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId)).thenReturn(table);
        when(accessService.find(databaseId, username)).thenReturn(access);
        when(queryService.tableFindAll(containerId, databaseId, tableId, timestamp, page, size, principal)).thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = dataEndpoint.getAll(containerId, databaseId, tableId,
                principal, timestamp, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    public void generic_getCount(Long containerId, Long databaseId, Long tableId, Database database, Table table,
                                 String username, DatabaseAccess access, Principal principal, Instant timestamp) throws UserNotFoundException, TableMalformedException, NotAllowedException, PaginationException, TableNotFoundException, QueryStoreException, SortException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId)).thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId)).thenReturn(table);
        when(accessService.find(databaseId, username)).thenReturn(access);
        when(queryService.tableCount(containerId, databaseId, tableId, timestamp, principal)).thenReturn(QUERY_1_RESULT_NUMBER);

        /* test */
        final ResponseEntity<Long> response = dataEndpoint.getCount(containerId, databaseId, tableId,
                principal, timestamp);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody());
    }

}
