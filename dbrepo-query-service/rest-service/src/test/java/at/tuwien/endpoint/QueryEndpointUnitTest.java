package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    /* keep */
    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    /* keep */
    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private QueryService queryService;

    @MockBean
    private StoreService storeService;

    @Autowired
    private QueryEndpoint queryEndpoint;

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicForbiddenKeyword_fails() {
        final String statement = "SELECT w.* FROM `weather_aus` w";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, null, USER_2_PRINCIPAL, DATABASE_3, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicEmptyStatement_fails() {
        final String statement = null;

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_2_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicBlankStatement_fails() {
        final String statement = "";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_2_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicForbiddenKeyword2_fails() {
        final String statement = "SELECT * FROM `weather_aus` w";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, null, USER_2_PRINCIPAL, DATABASE_3, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void execute_publicAnonymized_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, null, null, DATABASE_3, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_2_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_2_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_2_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_2_WRITE_ALL_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void reExecute_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException, SortException,
            DatabaseConnectionException, TableMalformedException, NotAllowedException, QueryMalformedException,
            QueryNotFoundException, ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, QUERY_4, QUERY_4_RESULT_ID, QUERY_4_RESULT_DTO,
                null, null, DATABASE_3, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_publicRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, QUERY_4, QUERY_4_RESULT_ID, QUERY_4_RESULT_DTO,
                USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_public_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, QUERY_4, QUERY_4_RESULT_ID, QUERY_4_RESULT_DTO,
                USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void export_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException, NotAllowedException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_3_ID, null, null, DATABASE_3, null, null, HttpStatus.OK);
    }

    @Test
    @WithAnonymousUser
    public void export_publicAnonymizedInvalidFormat_fails() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException, TableMalformedException, NotAllowedException, QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, IOException, FileStorageException, ContainerNotFoundException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_3_ID, null, null, DATABASE_3, null, "application/json", HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_publicRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException, NotAllowedException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_3_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_1_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException, NotAllowedException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_1_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException, NotAllowedException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_USER_1_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void execute_privateAnonymized_fails() {
        final Principal principal = null;

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_STATEMENT, null, principal, DATABASE_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateOwner_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_STATEMENT, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_WRITE_ALL_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void reExecute_privateAnonymized_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                    null, null, DATABASE_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_privateRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_privateWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_privateWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_WRITE_ALL_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void export_privateAnonymized_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, null, null, DATABASE_2, null, null, HttpStatus.OK);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateInvalidFormat_fails() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException, TableMalformedException, NotAllowedException, QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, IOException, FileStorageException, ContainerNotFoundException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_2_READ_ACCESS, "application/json", HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_USER_1_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_execute(Long containerId, Long databaseId, String statement, String username,
                                   Principal principal, Database database, DatabaseAccess access)
            throws UserNotFoundException, QueryStoreException, TableMalformedException, DatabaseConnectionException,
            QueryMalformedException, ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, SortException, NotAllowedException, PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(statement)
                .build();
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(containerId, databaseId))
                .thenReturn(Optional.of(database));
        log.trace("mock database for container with id {} and database id {}", containerId, databaseId);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
            log.trace("mock no access for database with id {} and username {}", databaseId, username);
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
            log.trace("mock access {} for database with id {} and username {}", access.getType(), databaseId, username);
        }
        when(queryService.execute(containerId, databaseId, request, principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);
        log.trace("mock query service for container with id {} and database with id {}", containerId, databaseId);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(containerId, databaseId, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    protected void generic_reExecute(Long containerId, Long databaseId, Long queryId, Query query, Long resultId,
                                     QueryResultDto result, String username, Principal principal, Database database,
                                     DatabaseAccess access)
            throws UserNotFoundException, QueryStoreException, DatabaseConnectionException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, QueryMalformedException,
            ColumnParseException, SortException, NotAllowedException, PaginationException {
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(containerId, databaseId))
                .thenReturn(Optional.of(database));
        when(storeService.findOne(containerId, databaseId, queryId, principal))
                .thenReturn(query);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(queryService.reExecute(containerId, databaseId, query, page, size, sortDirection, sortColumn, principal))
                .thenReturn(result);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(containerId, databaseId, queryId,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(resultId, response.getBody().getId());
    }

    protected void export_generic(Long containerId, Long databaseId, Long queryId, String username, Principal principal,
                                  Database database, DatabaseAccess access, String accept, HttpStatus status) throws IOException,
            UserNotFoundException, QueryStoreException, DatabaseConnectionException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, QueryMalformedException,
            FileStorageException, ContainerNotFoundException, NotAllowedException {
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(containerId, databaseId))
                .thenReturn(Optional.of(database));
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(storeService.findOne(containerId, databaseId, queryId, principal))
                .thenReturn(QUERY_1);
        when(queryService.findOne(containerId, databaseId, queryId, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(containerId, databaseId, queryId, accept, principal);
        assertEquals(status, response.getStatusCode());
        if (status.equals(HttpStatus.OK)) {
            assertNotNull(response.getBody());
        }
    }

}