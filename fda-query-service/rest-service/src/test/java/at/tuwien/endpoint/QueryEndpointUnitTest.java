package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private RabbitMqListenerImpl rabbitMqListener;

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
    public void execute_publicAnonymized_fails() {
        final Principal principal = null;

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_execute(CONTAINER_1_ID, DATABASE_1_ID, null, principal, DATABASE_1, null);
        });
    }

    @Test
    public void execute_publicRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_READ_ACCESS);
    }

    @Test
    public void execute_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_WRITE_OWN_ACCESS);
    }

    @Test
    public void execute_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void execute_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_1_ID, DATABASE_1_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1, DATABASE_1_OWNER_ACCESS);
    }

    @Test
    public void reExecute_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, null, null, DATABASE_1, null);
    }

    @Test
    public void reExecute_publicRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_READ_ACCESS);
    }

    @Test
    public void reExecute_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_WRITE_OWN_ACCESS);
    }

    @Test
    public void reExecute_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void reExecute_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1, DATABASE_1_OWNER_ACCESS);
    }

    @Test
    public void export_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, null, null, DATABASE_1, null, null, HttpStatus.OK);
    }

    @Test
    public void export_publicAnonymized_fails() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, null, null, DATABASE_1, null, "application/json", HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    public void export_publicRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1, DATABASE_1_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1, DATABASE_1_OWNER_ACCESS, null, HttpStatus.OK);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */


    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_execute(Long containerId, Long databaseId, String username, Principal principal,
                                   Database database, DatabaseAccess access)
            throws UserNotFoundException, QueryStoreException, TableMalformedException, DatabaseConnectionException,
            QueryMalformedException, ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, SortException, NotAllowedException, PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

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
        when(queryService.execute(containerId, databaseId, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

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

    protected void generic_reExecute(Long containerId, Long databaseId, Long queryId, String username,
                                     Principal principal, Database database, DatabaseAccess access)
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
                .thenReturn(QUERY_1);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(queryService.reExecute(containerId, databaseId, QUERY_1, page, size, sortDirection, sortColumn, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(containerId, databaseId, queryId,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
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