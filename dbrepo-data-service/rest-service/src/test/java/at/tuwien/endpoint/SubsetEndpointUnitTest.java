package at.tuwien.endpoint;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.endpoints.SubsetEndpoint;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.SubsetService;
import at.tuwien.test.AbstractUnitTest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SubsetEndpointUnitTest extends AbstractUnitTest {

    @Autowired
    private SubsetEndpoint subsetEndpoint;

    @MockBean
    private SubsetService queryService;

    @MockBean
    private HttpServletRequest httpServletRequest;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockBean
    private MockHttpServletRequest mockHttpServletRequest;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithAnonymousUser
    public void findAllById_succeeds() throws DatabaseUnavailableException, NotAllowedException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, SQLException {

        /* test */
        final List<QueryDto> response = generic_findAllById(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO, null);
        assertEquals(6, response.size());
    }

    @Test
    @WithAnonymousUser
    public void findAllById_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_findAllById(null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void findAllById_privateNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_findAllById(DATABASE_1_ID, DATABASE_1_PRIVILEGED_DTO, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            DatabaseUnavailableException, StorageUnavailableException, NotAllowedException, QueryMalformedException,
            QueryNotFoundException, SidecarExportException, FormatNotAvailableException, StorageNotFoundException,
            SQLException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);

        /* test */
        generic_findById(QUERY_5_ID, QUERY_5_DTO, MediaType.APPLICATION_JSON, null, null);
    }

    @Test
    @WithAnonymousUser
    public void findById_acceptCsv_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, StorageUnavailableException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, SidecarExportException, FormatNotAvailableException,
            StorageNotFoundException, SQLException {
        final ExportResourceDto mock = ExportResourceDto.builder()
                .filename("deadbeef")
                .resource(new InputStreamResource(InputStream.nullInputStream()))
                .build();

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(queryService.export(any(PrivilegedDatabaseDto.class), any(QueryDto.class), any(Instant.class), anyString()))
                .thenReturn(mock);

        /* test */
        generic_findById(QUERY_5_ID, QUERY_5_DTO, MediaType.parseMediaType("text/csv"), null, null);
    }

    @Test
    @WithAnonymousUser
    public void findById_timestamp_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, StorageUnavailableException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, SidecarExportException, FormatNotAvailableException,
            StorageNotFoundException, SQLException {
        final ExportResourceDto mock = ExportResourceDto.builder()
                .filename("deadbeef")
                .resource(new InputStreamResource(InputStream.nullInputStream()))
                .build();

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(queryService.export(any(PrivilegedDatabaseDto.class), any(QueryDto.class), any(Instant.class), anyString()))
                .thenReturn(mock);

        /* test */
        generic_findById(QUERY_5_ID, QUERY_5_DTO, MediaType.parseMediaType("text/csv"), Instant.now(), null);
    }

    @Test
    @WithAnonymousUser
    public void findById_fails() throws DatabaseNotFoundException, RemoteUnavailableException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_3_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_findById(QUERY_5_ID, QUERY_5_DTO, MediaType.APPLICATION_JSON, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_succeeds() throws UserNotFoundException, QueryStoreInsertException, TableMalformedException,
            NotAllowedException, SidecarExportException, QueryNotSupportedException, PaginationException,
            StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            FormatNotAvailableException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_READ_ACCESS_DTO);
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(queryService.execute(eq(DATABASE_3_PRIVILEGED_DTO), anyString(), any(Instant.class), eq(USER_1_ID), eq(0L), eq(10L), eq(null), eq(null)))
                .thenReturn(QUERY_5_RESULT_DTO);

        /* test */
        subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, 0L, 10L, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_forbiddenKeyword_fails() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT * FROM tbl")
                .build();

        /* test */
        assertThrows(QueryNotSupportedException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, 0L, 10L, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_noPageSize_succeeds() throws UserNotFoundException, QueryStoreInsertException,
            TableMalformedException, NotAllowedException, SidecarExportException, QueryNotSupportedException,
            PaginationException, StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            FormatNotAvailableException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_READ_ACCESS_DTO);
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(queryService.execute(eq(DATABASE_3_PRIVILEGED_DTO), anyString(), any(Instant.class), eq(USER_1_ID), eq(0L), eq(10L), eq(null), eq(null)))
                .thenReturn(QUERY_5_RESULT_DTO);

        /* test */
        subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_databaseNotFound_fails() throws NotAllowedException, RemoteUnavailableException,
            DatabaseNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_READ_ACCESS_DTO);
        doThrow(DatabaseNotFoundException.class)
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_3_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, request, USER_4_PRINCIPAL, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {"execute-query"})
    public void create_noAccess_fails() throws NotAllowedException, RemoteUnavailableException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        doThrow(NotAllowedException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_3_ID, USER_4_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, request, USER_4_PRINCIPAL, null, null, null);
        });
    }

    @Test
    public void getData_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            NotAllowedException, SQLException, QueryNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseUnavailableException, PaginationException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(queryService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(queryService.reExecuteCount(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO))
                .thenReturn(QUERY_5_RESULT_NUMBER);
        when(queryService.reExecute(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO, 0L, 10L, null, null))
                .thenReturn(QUERY_5_RESULT_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<QueryResultDto> response = subsetEndpoint.getData(DATABASE_3_ID, QUERY_5_ID, null, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_5_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
        assertNotNull(response.getHeaders().get("Access-Control-Expose-Headers"));
        assertEquals(1, response.getHeaders().get("Access-Control-Expose-Headers").size());
        assertEquals("X-Count", response.getHeaders().get("Access-Control-Expose-Headers").get(0));
        assertNotNull(response.getBody());
    }

    @Test
    public void getData_onlyHead_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            NotAllowedException, SQLException, QueryNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseUnavailableException, PaginationException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(queryService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(queryService.reExecuteCount(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO))
                .thenReturn(QUERY_5_RESULT_NUMBER);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<QueryResultDto> response = subsetEndpoint.getData(DATABASE_3_ID, QUERY_5_ID, null, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_5_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_private_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, NotAllowedException, TableMalformedException,
            QueryMalformedException, QueryNotFoundException, PaginationException, SQLException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");
        when(queryService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);
        when(queryService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO))
                .thenReturn(QUERY_1_RESULT_NUMBER);
        when(queryService.reExecute(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, 0L, 10L, null, null))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_1_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
        assertNotNull(response.getBody());
    }

    @Test
    @WithAnonymousUser
    public void getData_privateAnonymous_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, null, httpServletRequest, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_privateNoAccess_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(NotAllowedException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_1_ID, USER_1_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_privateOnlyHead_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, NotAllowedException, TableMalformedException,
            QueryMalformedException, QueryNotFoundException, PaginationException, SQLException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(queryService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);
        when(queryService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO))
                .thenReturn(QUERY_1_RESULT_NUMBER);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<QueryResultDto> response = subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_1_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"persist-query"})
    public void persist_succeeds() throws NotAllowedException, RemoteUnavailableException, DatabaseNotFoundException,
            QueryStorePersistException, SQLException, UserNotFoundException, QueryNotFoundException,
            DatabaseUnavailableException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doNothing()
                .when(queryService)
                .persist(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID, true);
        when(queryService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);

        /* test */
        subsetEndpoint.persist(DATABASE_3_ID, QUERY_5_ID, request, USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void persist_noRole_fails() {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            subsetEndpoint.persist(DATABASE_3_ID, QUERY_5_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"persist-query"})
    public void persist_noAccess_fails() throws NotAllowedException, RemoteUnavailableException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        doThrow(NotAllowedException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_3_ID, USER_3_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.persist(DATABASE_3_ID, QUERY_5_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"persist-query"})
    public void persist_databaseNotFound_fails() throws NotAllowedException, RemoteUnavailableException,
            DatabaseNotFoundException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        doThrow(DatabaseNotFoundException.class)
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_3_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            subsetEndpoint.persist(DATABASE_3_ID, QUERY_5_ID, request, USER_3_PRINCIPAL);
        });
    }

    protected List<QueryDto> generic_findAllById(Long databaseId, PrivilegedDatabaseDto database, Principal principal)
            throws DatabaseUnavailableException, NotAllowedException, QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, SQLException {

        /* mock */
        if (database != null) {
            when(metadataServiceGateway.getDatabaseById(databaseId))
                    .thenReturn(database);
            when(queryService.findAll(database, null))
                    .thenReturn(List.of(QUERY_1_DTO, QUERY_2_DTO, QUERY_3_DTO, QUERY_4_DTO, QUERY_5_DTO, QUERY_6_DTO));
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(metadataServiceGateway)
                    .getDatabaseById(databaseId);
        }

        /* test */
        final ResponseEntity<List<QueryDto>> response = subsetEndpoint.findAllById(databaseId, null, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    protected void generic_findById(Long subsetId, QueryDto subset, MediaType accept, Instant timestamp,
                                    Principal principal) throws UserNotFoundException, DatabaseUnavailableException,
            StorageUnavailableException, NotAllowedException, QueryMalformedException, QueryNotFoundException,
            DatabaseNotFoundException, SidecarExportException, RemoteUnavailableException, FormatNotAvailableException,
            StorageNotFoundException, SQLException {

        /* mock */
        when(queryService.findById(DATABASE_3_PRIVILEGED_DTO, subsetId))
                .thenReturn(subset);
        when(mockHttpServletRequest.getHeader("Accept"))
                .thenReturn(accept.toString());

        /* test */
        final ResponseEntity<?> response = subsetEndpoint.findById(DATABASE_3_ID, subsetId, mockHttpServletRequest, timestamp, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
