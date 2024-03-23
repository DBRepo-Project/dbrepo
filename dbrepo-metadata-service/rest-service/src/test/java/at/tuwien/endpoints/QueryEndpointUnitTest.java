package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class QueryEndpointUnitTest extends BaseUnitTest {

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
            generic_execute(DATABASE_3_ID, statement, USER_2_PRINCIPAL, DATABASE_3);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicEmptyStatement_fails() {

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(DATABASE_3_ID, null, USER_2_PRINCIPAL, DATABASE_3);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicBlankStatement_fails() {

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(DATABASE_3_ID, "", USER_2_PRINCIPAL, DATABASE_3);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicForbiddenKeyword2_fails() {
        final String statement = "SELECT * FROM `weather_aus` w";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(DATABASE_3_ID, statement, USER_2_PRINCIPAL, DATABASE_3);
        });
    }

    @Test
    @WithAnonymousUser
    public void execute_publicAnonymized_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_execute(DATABASE_3_ID, QUERY_4_STATEMENT, null, DATABASE_3);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {"execute-query"})
    public void execute_publicNoAccess_succeeds() throws UserNotFoundException, AccessDeniedException,
            QueryStoreException, SortException, TableMalformedException, NotAllowedException, QueryMalformedException,
            ColumnParseException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException {

        /* test */
        generic_execute(DATABASE_3_ID, QUERY_4_STATEMENT, null, DATABASE_3);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicRead_succeeds() throws UserNotFoundException, AccessDeniedException, QueryStoreException,
            SortException, TableMalformedException, NotAllowedException, QueryMalformedException, ColumnParseException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* test */
        generic_execute(DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_PRINCIPAL, DATABASE_3);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicWriteOwn_succeeds() throws UserNotFoundException, AccessDeniedException,
            QueryStoreException, SortException, TableMalformedException, NotAllowedException, QueryMalformedException,
            ColumnParseException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException {

        /* test */
        generic_execute(DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_PRINCIPAL, DATABASE_3);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicWriteAll_succeeds() throws UserNotFoundException, AccessDeniedException,
            QueryStoreException, SortException, TableMalformedException, NotAllowedException, QueryMalformedException,
            ColumnParseException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* test */
        generic_execute(DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_PRINCIPAL, DATABASE_3);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_publicOwner_succeeds() throws UserNotFoundException, AccessDeniedException, QueryStoreException,
            SortException, TableMalformedException, NotAllowedException, QueryMalformedException, ColumnParseException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* test */
        generic_execute(DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_PRINCIPAL, DATABASE_3);
    }

    @Test
    @WithAnonymousUser
    public void reExecute_publicAnonymized_succeeds() throws AccessDeniedException, QueryStoreException, SortException,
            TableMalformedException, NotAllowedException, QueryMalformedException, QueryNotFoundException,
            ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* test */
        generic_reExecute(DATABASE_3_ID, QUERY_4_ID, QUERY_4, QUERY_4_RESULT_ID, QUERY_4_RESULT_DTO,
                null, DATABASE_3, true);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_publicRead_succeeds() throws AccessDeniedException, QueryStoreException, SortException,
            TableMalformedException, NotAllowedException, QueryMalformedException, QueryNotFoundException,
            ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* test */
        generic_reExecute(DATABASE_3_ID, QUERY_4_ID, QUERY_4, QUERY_4_RESULT_ID, QUERY_4_RESULT_DTO,
                USER_2_PRINCIPAL, DATABASE_3, true);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_public_succeeds() throws AccessDeniedException, QueryStoreException, SortException,
            TableMalformedException, NotAllowedException, QueryMalformedException, QueryNotFoundException,
            ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* test */
        generic_reExecute(DATABASE_3_ID, QUERY_4_ID, QUERY_4, QUERY_4_RESULT_ID, QUERY_4_RESULT_DTO,
                USER_2_PRINCIPAL, DATABASE_3, true);
    }

    @Test
    @WithAnonymousUser
    public void export_publicAnonymized_succeeds() throws QueryStoreException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            IOException, FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_3_ID, QUERY_3_ID, null, DATABASE_3, null, HttpStatus.OK);
    }

    @Test
    @WithAnonymousUser
    public void export_publicAnonymizedInvalidFormat_fails() throws QueryStoreException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            IOException, FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_3_ID, QUERY_3_ID, null, DATABASE_3, "application/json", HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_publicRead_succeeds() throws QueryStoreException, NotAllowedException, QueryMalformedException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, IOException,
            FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_3_ID, QUERY_3_ID, USER_2_PRINCIPAL, DATABASE_3, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_publicWriteOwn_succeeds() throws QueryStoreException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            IOException, FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_3_ID, QUERY_4_ID, USER_2_PRINCIPAL, DATABASE_3, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_publicWriteAll_succeeds() throws QueryStoreException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            IOException, FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_3_ID, QUERY_4_ID, USER_2_PRINCIPAL, DATABASE_3, null, HttpStatus.OK);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void execute_privateAnonymized_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_execute(DATABASE_2_ID, QUERY_1_STATEMENT, null, DATABASE_2);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateRead_succeeds() throws UserNotFoundException, AccessDeniedException, QueryStoreException,
            SortException, TableMalformedException, NotAllowedException, QueryMalformedException, ColumnParseException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* mock */
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_2_READ_ACCESS));

        /* test */
        generic_execute(DATABASE_2_ID, QUERY_1_STATEMENT, USER_2_PRINCIPAL, DATABASE_2);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateWriteOwn_succeeds() throws UserNotFoundException, AccessDeniedException,
            QueryStoreException, SortException, TableMalformedException, NotAllowedException, QueryMalformedException,
            ColumnParseException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException {

        /* mock */
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_2_WRITE_OWN_ACCESS));

        /* test */
        generic_execute(DATABASE_2_ID, QUERY_1_STATEMENT, USER_2_PRINCIPAL, DATABASE_2);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateWriteAll_succeeds() throws UserNotFoundException, AccessDeniedException,
            QueryStoreException, SortException, TableMalformedException, NotAllowedException, QueryMalformedException,
            ColumnParseException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException {

        /* mock */
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS));

        /* test */
        generic_execute(DATABASE_2_ID, QUERY_1_STATEMENT, USER_2_PRINCIPAL, DATABASE_2);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void execute_privateOwner_succeeds() throws UserNotFoundException, AccessDeniedException,
            QueryStoreException, SortException, TableMalformedException, NotAllowedException, QueryMalformedException,
            ColumnParseException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException {

        /* mock */
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_1_WRITE_ALL_ACCESS));

        /* test */
        generic_execute(DATABASE_2_ID, QUERY_1_STATEMENT, USER_1_PRINCIPAL, DATABASE_2);
    }

    @Test
    @WithAnonymousUser
    public void reExecute_privateAnonymized_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_reExecute(DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                    null, DATABASE_2, true);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_privateRead_succeeds() throws AccessDeniedException, QueryStoreException, SortException,
            TableMalformedException, NotAllowedException, QueryMalformedException, QueryNotFoundException,
            ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* mock */
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_2_READ_ACCESS));

        /* test */
        generic_reExecute(DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                USER_2_PRINCIPAL, DATABASE_2, true);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_privateWriteOwn_succeeds() throws AccessDeniedException, QueryStoreException, SortException,
            TableMalformedException, NotAllowedException, QueryMalformedException, QueryNotFoundException,
            ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException {

        /* mock */
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_2_WRITE_OWN_ACCESS));

        /* test */
        generic_reExecute(DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                USER_2_PRINCIPAL, DATABASE_2, true);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"execute-query"})
    public void reExecute_privateWriteAll_succeeds() throws QueryStoreException, TableMalformedException,
            QueryMalformedException, ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException,
            SortException, NotAllowedException, PaginationException, QueryNotFoundException,
            at.tuwien.exception.AccessDeniedException {

        /* mock */
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS));

        /* test */
        generic_reExecute(DATABASE_2_ID, QUERY_1_ID, QUERY_1, QUERY_1_RESULT_ID, QUERY_1_RESULT_DTO,
                USER_2_PRINCIPAL, DATABASE_2, true);
    }

    @Test
    @WithAnonymousUser
    public void export_privateAnonymized_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(DATABASE_2_ID, QUERY_1_ID, null, DATABASE_2, null, HttpStatus.OK);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateInvalidFormat_fails() throws QueryStoreException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            IOException, FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_2_ID, QUERY_1_ID, USER_2_PRINCIPAL, DATABASE_2, "application/json", HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateRead_succeeds() throws QueryStoreException, NotAllowedException, QueryMalformedException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, IOException,
            FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_2_ID, QUERY_1_ID, USER_2_PRINCIPAL, DATABASE_2, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateWriteOwn_succeeds() throws QueryStoreException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            IOException, FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_2_ID, QUERY_1_ID, USER_2_PRINCIPAL, DATABASE_2, null, HttpStatus.OK);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-query-data"})
    public void export_privateWriteAll_succeeds() throws QueryStoreException, NotAllowedException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            IOException, FileStorageException, DataProcessingException {

        /* test */
        export_generic(DATABASE_2_ID, QUERY_1_ID, USER_2_PRINCIPAL, DATABASE_2, null, HttpStatus.OK);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_execute(Long databaseId, String statement, Principal principal, Database database)
            throws UserNotFoundException, QueryStoreException, TableMalformedException, QueryMalformedException,
            ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException, SortException,
            NotAllowedException, PaginationException, at.tuwien.exception.AccessDeniedException,
            QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(statement)
                .build();
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        log.trace("mock database for container database id {}", databaseId);
        when(queryService.execute(databaseId, request, principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);
        log.trace("mock query service for container database with id {}", databaseId);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(databaseId, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    protected void generic_reExecute(Long databaseId, Long queryId, Query query, Long resultId,
                                     QueryResultDto result, Principal principal, Database database, boolean isGet)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, QueryMalformedException, ColumnParseException, SortException, NotAllowedException,
            PaginationException, at.tuwien.exception.AccessDeniedException {
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        when(storeService.findOne(databaseId, queryId, principal))
                .thenReturn(query);
        when(queryService.reExecute(databaseId, query, page, size, sortDirection, sortColumn, principal))
                .thenReturn(result);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod())
                .thenReturn(isGet ? "GET" : "HEAD");

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(databaseId, queryId,
                principal, request, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(resultId, response.getBody().getId());
    }

    protected void export_generic(Long databaseId, Long queryId, Principal principal, Database database, String accept,
                                  HttpStatus status) throws IOException, QueryStoreException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, FileStorageException,
            NotAllowedException, DataProcessingException {
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        doReturn(QUERY_1)
                .when(storeService)
                .findOne(databaseId, queryId, principal);
        doReturn(resource)
                .when(queryService)
                .findOne(databaseId, queryId, principal);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(databaseId, queryId, accept, principal);
        assertEquals(status, response.getStatusCode());
        if (status.equals(HttpStatus.OK)) {
            assertNotNull(response.getBody());
        }
    }

}