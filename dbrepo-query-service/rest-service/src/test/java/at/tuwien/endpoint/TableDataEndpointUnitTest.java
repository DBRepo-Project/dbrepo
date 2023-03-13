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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public static Stream<Arguments> import_fails_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous", NotAllowedException.class, CONTAINER_1_ID, DATABASE_1_ID,
                        DATABASE_1,
                        TABLE_1_ID, TABLE_1, null, null, null),
                Arguments.arguments("public read", NotAllowedException.class, CONTAINER_1_ID, DATABASE_1_ID,
                        DATABASE_1, TABLE_1_ID,
                        TABLE_1, USER_2_USERNAME, DATABASE_1_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL),
                Arguments.arguments("public write-own", NotAllowedException.class, CONTAINER_1_ID, DATABASE_1_ID,
                        DATABASE_1, TABLE_1_ID,
                        TABLE_1, USER_2_USERNAME, DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL),
                Arguments.arguments("private anonymous", NotAllowedException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        DATABASE_2, TABLE_1_ID, TABLE_1, null, null, null),
                Arguments.arguments("private read", NotAllowedException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL),
                Arguments.arguments("private write-own", NotAllowedException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL)
        );
    }

    @ParameterizedTest
    @MethodSource("import_fails_parameters")
    public <T extends Throwable> void import_fails(String test, Class<T> expectedException, Long containerId,
                                                   Long databaseId, Database database, Long tableId, Table table,
                                                   String username, DatabaseAccess access, Principal principal) {

        /* test */
        assertThrows(expectedException, () -> {
            generic_import(containerId, databaseId, database, tableId, table, username, access, principal);
        });
    }

    public static Stream<Arguments> import_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("public write-all", CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1,
                        USER_2_USERNAME, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL),
                Arguments.arguments("public owner", CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1,
                        USER_1_USERNAME, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL),
                Arguments.arguments("private write-all", CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL),
                Arguments.arguments("private owner", CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL)
        );
    }

    @ParameterizedTest
    @MethodSource("import_succeeds_parameters")
    public void import_succeeds(String test, Long containerId, Long databaseId, Database database, Long tableId,
                                Table table, String username, DatabaseAccess access, Principal principal) throws UserNotFoundException, TableNotFoundException, NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(containerId, databaseId, database, tableId, table, username, access, principal);
    }

    public static Stream<Arguments> insert_fails_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous", NotAllowedException.class, CONTAINER_1_ID, DATABASE_1_ID,
                        TABLE_1_ID,
                        DATABASE_1, TABLE_1, USER_2_USERNAME, null, TABLE_1_CSV_DTO, null),
                Arguments.arguments("public read", NotAllowedException.class, CONTAINER_1_ID, DATABASE_1_ID,
                        TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_2_USERNAME, DATABASE_1_RESEARCHER_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL),
                Arguments.arguments("public write-own", NotAllowedException.class, CONTAINER_1_ID, DATABASE_1_ID,
                        TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_2_USERNAME, DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL),
                Arguments.arguments("private anonymous", NotAllowedException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME, null,
                        TABLE_1_CSV_DTO, null),
                Arguments.arguments("private read", NotAllowedException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL),
                Arguments.arguments("private write-own", NotAllowedException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL)
        );
    }

    @ParameterizedTest
    @MethodSource("insert_fails_parameters")
    public <T extends Throwable> void insert_fails(String test, Class<T> expectedException, Long containerId,
                                                   Long databaseId, Long tableId, Database database, Table table,
                                                   String username, DatabaseAccess access, TableCsvDto data,
                                                   Principal principal) {

        /* test */
        assertThrows(expectedException, () -> {
            generic_insert(containerId, databaseId, tableId, database, table, username, access, data, principal);
        });
    }

    public static Stream<Arguments> insert_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("public write-all", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL),
                Arguments.arguments("public owner", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL),
                Arguments.arguments("public owner, data null", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, null, USER_1_PRINCIPAL),
                Arguments.arguments("private write-all", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL),
                Arguments.arguments("private owner", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL),
                Arguments.arguments("private owner, data null", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2
                        , TABLE_1, USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, null, USER_1_PRINCIPAL)
        );
    }

    @ParameterizedTest
    @MethodSource("insert_succeeds_parameters")
    public void insert_succeeds(String test, Long containerId, Long databaseId, Long tableId, Database database,
                                Table table, String username, DatabaseAccess access, TableCsvDto data,
                                Principal principal) throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_insert(containerId, databaseId, tableId, database, table, username, access, data, principal);
    }

    public static Stream<Arguments> getAll_fails_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous page null", PaginationException.class, CONTAINER_1_ID,
                        DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, null, null, null, null, null, 1L, null, null),
                Arguments.arguments("public anonymous size null", PaginationException.class, CONTAINER_1_ID,
                        DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, null, null, null, null, 1L, null, null, null),
                Arguments.arguments("public anonymous page negative", PaginationException.class, CONTAINER_1_ID,
                        DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, null, null, null, null, -1L, 1L, null, null),
                Arguments.arguments("public anonymous size zero", PaginationException.class, CONTAINER_1_ID,
                        DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, null, null, null, null, 0L, 0L, null, null),
                Arguments.arguments("public anonymous size negative", PaginationException.class, CONTAINER_1_ID,
                        DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, null, null, null, null, 0L, -1L, null, null),
                Arguments.arguments("private anonymous", NotAllowedException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        TABLE_1_ID, DATABASE_2, TABLE_1, null, null, null, null,
                        null, null, null, null),
                Arguments.arguments("private read, page null", PaginationException.class, CONTAINER_2_ID,
                        DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null, null, 1L, null, null),
                Arguments.arguments("private read, size null", PaginationException.class, CONTAINER_2_ID,
                        DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null, 1L, null, null, null),
                Arguments.arguments("private read, page negative", PaginationException.class, CONTAINER_2_ID,
                        DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null, -1L, 1L, null, null),
                Arguments.arguments("private read, size zero", PaginationException.class, CONTAINER_2_ID,
                        DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null, 0L, 0L, null, null),
                Arguments.arguments("private read, size negative", PaginationException.class, CONTAINER_2_ID,
                        DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null, 0L, -1L, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("getAll_fails_parameters")
    public <T extends Throwable> void getAll_fails(String test, Class<T> expectedException, Long containerId,
                                                   Long databaseId, Long tableId, Database database, Table table,
                                                   String username, DatabaseAccess access, Principal principal,
                                                   Instant timestamp, Long page, Long size, SortType sortDirection,
                                                   String sortColumn) {

        /* test */
        assertThrows(expectedException, () -> {
            generic_getAll(containerId, databaseId, tableId, database, table, username, access, principal, timestamp,
                    page, size, sortDirection, sortColumn);
        });
    }

    public static Stream<Arguments> getAll_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("public anonymous", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, null, null, null,
                        null, null, null, null, null),
                Arguments.arguments("public read", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1,
                        USER_2_USERNAME,
                        DATABASE_1_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("public write-own", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("public write-all", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("public owner", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private read", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1,
                        USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private write-own", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private write-all", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null),
                Arguments.arguments("private owner", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null)
        );
    }

    @ParameterizedTest
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
                Arguments.arguments("public anonymous", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, null, null, null, null),
                Arguments.arguments("public read", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1,
                        USER_2_USERNAME,
                        DATABASE_1_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("public write-own", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("public write-all", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_1_RESEARCHER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("public owner", CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_1_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null),
                Arguments.arguments("private read", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1,
                        USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_READ_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("private write-own", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("private write-all", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2,
                        TABLE_1, USER_2_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null),
                Arguments.arguments("private owner", CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1,
                        USER_1_USERNAME,
                        DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null)
        );
    }

    @ParameterizedTest
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
        final ResponseEntity<QueryResultDto> response = dataEndpoint.data(containerId, databaseId, tableId,
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
