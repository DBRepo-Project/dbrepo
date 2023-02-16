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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.time.Instant;

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
    private MessageQueueListener messageQueueListener;

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
    public void import_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, null,
                    null, null);
        });
    }

    @Test
    public void import_publicRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_publicWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_publicWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                DATABASE_1_WRITE_ALL_ACCESS, USER_2_PRINCIPAL);
    }

    @Test
    public void import_publicOwner_succeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_1_USERNAME,
                DATABASE_1_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                    null, TABLE_1_CSV_DTO, null);
        });
    }

    @Test
    public void insert_publicRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                    DATABASE_1_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                DATABASE_1_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
    }

    @Test
    public void insert_publicOwner_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_USERNAME,
                DATABASE_1_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_publicOwnerDataNull_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_USERNAME,
                DATABASE_1_WRITE_ALL_ACCESS, null, USER_1_PRINCIPAL);
    }

    @Test
    public void getAll_publicAnonymous_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null,
                null, null, null, null, null, null, null);
    }

    @Test
    public void getAll_publicRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                DATABASE_1_READ_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                DATABASE_1_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_2_USERNAME,
                DATABASE_1_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicOwner_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_USERNAME,
                DATABASE_1_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicAnonymousPageNull_fails() {
        final Long page = null;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousSizeNull_fails() {
        final Long page = 1L;
        final Long size = null;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousPageNegative_fails() {
        final Long page = -1L;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousSizeZero_fails() {
        final Long page = 0L;
        final Long size = 0L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousSizeNegative_fails() {
        final Long page = 0L;
        final Long size = -1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null,
                    null, null, null, page, size, null, null);
        });
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    public void import_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, null, null, null);
        });
    }

    @Test
    public void import_privateRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_privateWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_privateWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_2_USERNAME,
                DATABASE_2_WRITE_ALL_ACCESS, USER_2_PRINCIPAL);
    }

    @Test
    public void import_privateOwner_succeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_1_ID, TABLE_1, USER_1_USERNAME,
                DATABASE_2_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    null, TABLE_1_CSV_DTO, null);
        });
    }

    @Test
    public void insert_privateRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_READ_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                DATABASE_2_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_2_PRINCIPAL);
    }

    @Test
    public void insert_privateOwner_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_1_USERNAME,
                DATABASE_2_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_privateOwnerDataNull_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_1_USERNAME,
                DATABASE_2_WRITE_ALL_ACCESS, null, USER_1_PRINCIPAL);
    }

    @Test
    public void getAll_privateAnonymous_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, null,
                    null, null, null, null, null, null, null);
        });
    }

    @Test
    public void getAll_privateRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                DATABASE_2_READ_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                DATABASE_2_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                DATABASE_2_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateOwner_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_1_USERNAME,
                DATABASE_2_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateReadPageNull_fails() {
        final Long page = null;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_READ_ACCESS, USER_2_PRINCIPAL, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateReadSizeNull_fails() {
        final Long page = 1L;
        final Long size = null;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_READ_ACCESS, USER_2_PRINCIPAL, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateReadPageNegative_fails() {
        final Long page = -1L;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_READ_ACCESS, USER_2_PRINCIPAL, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateReadSizeZero_fails() {
        final Long page = 0L;
        final Long size = 0L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_READ_ACCESS, USER_2_PRINCIPAL, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateReadSizeNegative_fails() {
        final Long page = 0L;
        final Long size = -1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, TABLE_1, USER_2_USERNAME,
                    DATABASE_2_READ_ACCESS, USER_2_PRINCIPAL, null, page, size, null, null);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void generic_import(Long containerId, Long databaseId, Database database, Long tableId, Table table,
                               String username, DatabaseAccess access, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException, UserNotFoundException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException,
            ContainerNotFoundException {
        final ImportDto request = ImportDto.builder()
                .location("test:csv/csv_01.csv")
                .build();

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, username))
                .thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.importCsv(containerId, databaseId, tableId, request,
                principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_insert(Long containerId, Long databaseId, Long tableId, Database database, Table table,
                               String username, DatabaseAccess access, TableCsvDto data, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException, UserNotFoundException,
            TableMalformedException, DatabaseConnectionException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, username))
                .thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.insert(containerId, databaseId, tableId, data, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_getAll(Long containerId, Long databaseId, Long tableId, Database database, Table table,
                               String username, DatabaseAccess access, Principal principal, Instant timestamp,
                               Long page, Long size, SortType sortDirection, String sortColumn)
            throws UserNotFoundException, TableMalformedException, NotAllowedException, PaginationException,
            TableNotFoundException, QueryStoreException, SortException, DatabaseConnectionException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, username))
                .thenReturn(access);
        when(queryService.findAll(containerId, databaseId, tableId, timestamp, page, size, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = dataEndpoint.getAll(containerId, databaseId, tableId, principal, timestamp, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

}
