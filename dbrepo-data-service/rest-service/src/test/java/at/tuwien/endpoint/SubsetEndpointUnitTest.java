package at.tuwien.endpoint;

import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SubsetEndpointUnitTest extends AbstractUnitTest {

    @Autowired
    private SubsetEndpoint subsetEndpoint;

    @MockBean
    private SubsetService subsetService;

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
    public void list_succeeds() throws DatabaseUnavailableException, NotAllowedException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, SQLException, MetadataServiceException {

        /* mock */
        when(subsetService.findAll(DATABASE_3_PRIVILEGED_DTO, null))
                .thenReturn(List.of(QUERY_1_DTO, QUERY_2_DTO, QUERY_3_DTO, QUERY_4_DTO, QUERY_5_DTO, QUERY_6_DTO));

        /* test */
        final List<QueryDto> response = generic_list(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO);
        assertEquals(6, response.size());
    }

    @Test
    @WithAnonymousUser
    public void list_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void list_unavailable_fails() throws SQLException, QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .findAll(DATABASE_3_PRIVILEGED_DTO, null);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            generic_list(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO);
        });
    }

//    @Test
//    @WithAnonymousUser
//    public void findById_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
//            DatabaseUnavailableException, StorageUnavailableException, QueryMalformedException, QueryNotFoundException,
//            FormatNotAvailableException, StorageNotFoundException, SQLException, MetadataServiceException,
//            ViewNotFoundException, MalformedException, TableNotFoundException {
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
//                .thenReturn(QUERY_5_DTO);
//
//        /* test */
//        generic_findById(QUERY_5_ID, MediaType.APPLICATION_JSON, null);
//    }

//    @Test
//    @WithAnonymousUser
//    public void findById_format_fails() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
//            UserNotFoundException, QueryNotFoundException, MetadataServiceException {
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
//                .thenReturn(QUERY_5_DTO);
//
//        /* test */
//        assertThrows(FormatNotAvailableException.class, () -> {
//            generic_findById(QUERY_5_ID, MediaType.APPLICATION_PDF, null);
//        });
//    }

//    @Test
//    @WithAnonymousUser
//    public void findById_acceptCsv_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
//            UserNotFoundException, DatabaseUnavailableException, StorageUnavailableException, QueryMalformedException,
//            QueryNotFoundException, FormatNotAvailableException, StorageNotFoundException, SQLException,
//            MetadataServiceException, ViewNotFoundException, MalformedException, TableNotFoundException {
//        final ExportResourceDto mock = ExportResourceDto.builder()
//                .filename("deadbeef")
//                .resource(new InputStreamResource(InputStream.nullInputStream()))
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
//                .thenReturn(QUERY_5_DTO);
//        when(subsetService.export(any(PrivilegedDatabaseDto.class), any(QueryDto.class), any(Instant.class)))
//                .thenReturn(mock);
//
//        /* test */
//        generic_findById(QUERY_5_ID, MediaType.parseMediaType("text/csv"), null);
//    }

//    @Test
//    @WithAnonymousUser
//    public void findById_timestamp_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
//            UserNotFoundException, DatabaseUnavailableException, StorageUnavailableException, QueryMalformedException,
//            QueryNotFoundException, FormatNotAvailableException, StorageNotFoundException, SQLException,
//            MetadataServiceException, ViewNotFoundException, MalformedException, TableNotFoundException {
//        final ExportResourceDto mock = ExportResourceDto.builder()
//                .filename("deadbeef")
//                .resource(new InputStreamResource(InputStream.nullInputStream()))
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
//                .thenReturn(QUERY_5_DTO);
//        when(subsetService.export(any(PrivilegedDatabaseDto.class), any(QueryDto.class), any(Instant.class)))
//                .thenReturn(mock);
//
//        /* test */
//        generic_findById(QUERY_5_ID, MediaType.parseMediaType("text/csv"), Instant.now());
//    }

//    @Test
//    @WithAnonymousUser
//    public void findById_notFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException {
//
//        /* mock */
//        doThrow(DatabaseNotFoundException.class)
//                .when(metadataServiceGateway)
//                .getDatabaseById(DATABASE_3_ID);
//
//        /* test */
//        assertThrows(DatabaseNotFoundException.class, () -> {
//            generic_findById(QUERY_5_ID, MediaType.APPLICATION_JSON, null);
//        });
//    }

//    @Test
//    @WithAnonymousUser
//    public void findById_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
//            MetadataServiceException, SQLException, UserNotFoundException, QueryNotFoundException {
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        doThrow(SQLException.class)
//                .when(subsetService)
//                .findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID);
//
//        /* test */
//        assertThrows(DatabaseUnavailableException.class, () -> {
//            generic_findById(QUERY_5_ID, MediaType.APPLICATION_JSON, null);
//        });
//    }

//    @Test
//    @WithAnonymousUser
//    public void findById_unavailableExport_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
//            MetadataServiceException, SQLException, StorageUnavailableException, QueryMalformedException,
//            StorageNotFoundException, UserNotFoundException, QueryNotFoundException, ViewNotFoundException, MalformedException {
//        final ExportResourceDto mock = ExportResourceDto.builder()
//                .filename("deadbeef")
//                .resource(new InputStreamResource(InputStream.nullInputStream()))
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
//                .thenReturn(QUERY_5_DTO);
//        when(subsetService.export(any(PrivilegedDatabaseDto.class), any(QueryDto.class), any(Instant.class)))
//                .thenReturn(mock);
//        doThrow(SQLException.class)
//                .when(subsetService)
//                .export(eq(DATABASE_3_PRIVILEGED_DTO), eq(QUERY_5_DTO), any(Instant.class));
//
//        /* test */
//        assertThrows(DatabaseUnavailableException.class, () -> {
//            generic_findById(QUERY_5_ID, MediaType.parseMediaType("text/csv"), null);
//        });
//    }
//
//    @Test
//    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
//    public void create_noAccess_succeeds() throws UserNotFoundException, QueryStoreInsertException,
//            TableMalformedException, NotAllowedException, QueryNotSupportedException, PaginationException,
//            StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException,
//            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
//            SQLException, MetadataServiceException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_5_STATEMENT)
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.execute(eq(DATABASE_3_PRIVILEGED_DTO), anyString(), any(Instant.class), eq(USER_1_ID), eq(0L), eq(10L), eq(null), eq(null)))
//                .thenReturn(QUERY_5_RESULT_DTO);
//
//        /* test */
//        subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, 0L, 10L, null);
//    }

//    @Test
//    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
//    public void create_forbiddenKeyword_fails() {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT COUNT(id) FROM tbl")
//                .build();
//
//        /* test */
//        assertThrows(QueryNotSupportedException.class, () -> {
//            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, 0L, 10L, null);
//        });
//    }

//    @Test
//    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
//    public void create_noPageSize_succeeds() throws UserNotFoundException, QueryStoreInsertException,
//            TableMalformedException, NotAllowedException, QueryNotSupportedException,
//            PaginationException, StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException,
//            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
//            SQLException, MetadataServiceException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_5_STATEMENT)
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.execute(eq(DATABASE_3_PRIVILEGED_DTO), anyString(), any(Instant.class), eq(USER_1_ID), eq(0L), eq(10L), eq(null), eq(null)))
//                .thenReturn(QUERY_5_RESULT_DTO);
//
//        /* test */
//        subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, null, null, null);
//    }
//
//    @Test
//    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
//    public void create_unavailable_succeeds() throws UserNotFoundException, QueryStoreInsertException,
//            TableMalformedException, NotAllowedException, QueryNotFoundException, DatabaseNotFoundException,
//            RemoteUnavailableException, SQLException, MetadataServiceException, QueryMalformedException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_5_STATEMENT)
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        doThrow(SQLException.class)
//                .when(subsetService)
//                .execute(eq(DATABASE_3_PRIVILEGED_DTO), anyString(), any(Instant.class), eq(USER_1_ID), eq(0L), eq(10L), eq(null), eq(null));
//
//        /* test */
//        assertThrows(DatabaseUnavailableException.class, () -> {
//            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, null, null, null);
//        });
//    }
//
//    @Test
//    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
//    public void create_databaseNotFound_fails() throws RemoteUnavailableException,
//            DatabaseNotFoundException, MetadataServiceException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_5_STATEMENT)
//                .build();
//
//        /* mock */
//        doThrow(DatabaseNotFoundException.class)
//                .when(metadataServiceGateway)
//                .getDatabaseById(DATABASE_3_ID);
//
//        /* test */
//        assertThrows(DatabaseNotFoundException.class, () -> {
//            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, null, null, null);
//        });
//    }
//
//    @Test
//    @WithMockUser(username = USER_4_USERNAME)
//    public void create_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
//            MetadataServiceException, UserNotFoundException, QueryStoreInsertException, TableMalformedException,
//            NotAllowedException, SQLException, QueryNotFoundException, DatabaseUnavailableException,
//            StorageUnavailableException, QueryMalformedException, QueryNotSupportedException, PaginationException,
//            StorageNotFoundException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_5_STATEMENT)
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.execute(eq(DATABASE_3_PRIVILEGED_DTO), anyString(), any(Instant.class), eq(USER_4_ID), eq(0L), eq(10L), eq(null), eq(null)))
//                .thenReturn(QUERY_5_RESULT_DTO);
//
//        /* test */
//        subsetEndpoint.create(DATABASE_3_ID, request, USER_4_PRINCIPAL, null, null, null);
//    }
//
//    @Test
//    @WithAnonymousUser
//    public void create_anonymous_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
//            MetadataServiceException, UserNotFoundException, QueryStoreInsertException, TableMalformedException,
//            NotAllowedException, SQLException, QueryNotFoundException, DatabaseUnavailableException,
//            StorageUnavailableException, QueryMalformedException, QueryNotSupportedException, PaginationException,
//            StorageNotFoundException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_5_STATEMENT)
//                .build();
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.execute(eq(DATABASE_3_PRIVILEGED_DTO), anyString(), any(Instant.class), eq(null), eq(0L), eq(10L), eq(null), eq(null)))
//                .thenReturn(QUERY_5_RESULT_DTO);
//
//        /* test */
//        subsetEndpoint.create(DATABASE_3_ID, request, null, null, null, null);
//    }
//
//    @Test
//    public void getData_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
//            NotAllowedException, SQLException, QueryNotFoundException, TableMalformedException, QueryMalformedException,
//            DatabaseUnavailableException, PaginationException, MetadataServiceException {
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
//                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
//        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
//                .thenReturn(QUERY_5_DTO);
//        when(subsetService.reExecuteCount(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO))
//                .thenReturn(QUERY_5_RESULT_NUMBER);
//        when(subsetService.reExecute(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO, 0L, 10L, null, null))
//                .thenReturn(QUERY_5_RESULT_DTO);
//        when(httpServletRequest.getMethod())
//                .thenReturn("GET");
//
//        /* test */
//        final ResponseEntity<List<Map<String, Object>>> response = subsetEndpoint.getData(DATABASE_3_ID, QUERY_5_ID, null, httpServletRequest, null, null);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }

    @Test
    public void getData_head_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            NotAllowedException, SQLException, QueryNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseUnavailableException, PaginationException, MetadataServiceException, TableNotFoundException, ViewMalformedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.reExecuteCount(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO))
                .thenReturn(QUERY_5_RESULT_NUMBER);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<List<Map<String, Object>>> response = subsetEndpoint.getData(DATABASE_3_ID, QUERY_5_ID, null, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_5_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
    }

//    @Test
//    @WithMockUser(username = USER_1_USERNAME)
//    public void getData_private_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
//            UserNotFoundException, DatabaseUnavailableException, NotAllowedException, TableMalformedException,
//            QueryMalformedException, QueryNotFoundException, PaginationException, SQLException, MetadataServiceException {
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
//                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
//        when(httpServletRequest.getMethod())
//                .thenReturn("GET");
//        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
//                .thenReturn(QUERY_1_DTO);
//        when(subsetService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO))
//                .thenReturn(QUERY_1_RESULT_NUMBER);
//        when(subsetService.reExecute(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, 0L, 10L, null, null))
//                .thenReturn(QUERY_1_RESULT_DTO);
//
//        /* test */
//        final ResponseEntity<List<Map<String, Object>>> response = subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }

    @Test
    @WithAnonymousUser
    public void getData_privateAnonymous_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

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
            NotAllowedException, MetadataServiceException {

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
    public void getData_privateHead_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, NotAllowedException, TableMalformedException,
            QueryMalformedException, QueryNotFoundException, PaginationException, SQLException,
            MetadataServiceException, TableNotFoundException, ViewMalformedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);
        when(subsetService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO))
                .thenReturn(QUERY_1_RESULT_NUMBER);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<List<Map<String, Object>>> response = subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_1_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
    }

//    @Test
//    @WithMockUser(username = USER_1_USERNAME)
//    public void getData_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
//            UserNotFoundException, QueryNotFoundException, MetadataServiceException, QueryMalformedException {
//
//        /* mock */
//        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
//                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
//        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
//                .thenReturn(QUERY_1_DTO);
//        when(httpServletRequest.getMethod())
//                .thenReturn("GET");
//        doThrow(SQLException.class)
//                .when(subsetService)
//                .reExecute(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, 0L, 10L, null, null);
//
//        /* test */
//        assertThrows(DatabaseUnavailableException.class, () -> {
//            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
//        });
//    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"persist-query"})
    public void persist_succeeds() throws NotAllowedException, RemoteUnavailableException, DatabaseNotFoundException,
            QueryStorePersistException, SQLException, UserNotFoundException, QueryNotFoundException,
            DatabaseUnavailableException, MetadataServiceException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doNothing()
                .when(subsetService)
                .persist(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID, true);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
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
    public void persist_noAccess_fails() throws NotAllowedException, RemoteUnavailableException, MetadataServiceException {
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
            DatabaseNotFoundException, MetadataServiceException {
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

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"persist-query"})
    public void persist_unavailable_fails() throws NotAllowedException, RemoteUnavailableException,
            MetadataServiceException, QueryStorePersistException, SQLException, DatabaseNotFoundException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .persist(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID, true);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            subsetEndpoint.persist(DATABASE_3_ID, QUERY_5_ID, request, USER_3_PRINCIPAL);
        });
    }

    protected List<QueryDto> generic_list(Long databaseId, PrivilegedDatabaseDto database) throws NotAllowedException,
            DatabaseUnavailableException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        if (database != null) {
            when(metadataServiceGateway.getDatabaseById(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(metadataServiceGateway)
                    .getDatabaseById(databaseId);
        }

        /* test */
        final ResponseEntity<List<QueryDto>> response = subsetEndpoint.list(databaseId, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

//    protected void generic_findById(Long subsetId, MediaType accept, Instant timestamp) throws UserNotFoundException,
//            DatabaseUnavailableException, StorageUnavailableException, QueryMalformedException, QueryNotFoundException,
//            DatabaseNotFoundException, RemoteUnavailableException, FormatNotAvailableException, MalformedException,
//            StorageNotFoundException, MetadataServiceException, ViewNotFoundException, TableNotFoundException {
//
//        /* mock */
//        when(mockHttpServletRequest.getHeader("Accept"))
//                .thenReturn(accept.toString());
//
//        /* test */
//        final ResponseEntity<?> response = subsetEndpoint.findById(DATABASE_3_ID, subsetId, mockHttpServletRequest, timestamp);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertNotNull(response.getBody());
//    }

}
