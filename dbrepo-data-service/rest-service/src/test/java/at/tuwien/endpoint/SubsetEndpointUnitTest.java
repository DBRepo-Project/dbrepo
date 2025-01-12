package at.tuwien.endpoint;

import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.endpoints.SubsetEndpoint;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.CredentialService;
import at.tuwien.service.SchemaService;
import at.tuwien.service.StorageService;
import at.tuwien.service.SubsetService;
import at.tuwien.test.AbstractUnitTest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
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

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
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

    @Autowired
    private SparkSession sparkSession;

    @MockBean
    private SubsetService subsetService;

    @MockBean
    private SchemaService schemaService;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockBean
    private HttpServletRequest httpServletRequest;

    @MockBean
    private StorageService storageService;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private MockHttpServletRequest mockHttpServletRequest;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithAnonymousUser
    public void list_publicDataPrivateSchemaAnonymous_fails() throws QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, SQLException, MetadataServiceException {

        /* mock */
        when(subsetService.findAll(DATABASE_3_PRIVILEGED_DTO, null))
                .thenReturn(List.of(QUERY_1_DTO, QUERY_2_DTO, QUERY_3_DTO, QUERY_4_DTO, QUERY_5_DTO, QUERY_6_DTO));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_list(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void list_publicDataPrivateSchema_succeeds() throws DatabaseUnavailableException, NotAllowedException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, SQLException, MetadataServiceException {

        /* mock */
        when(subsetService.findAll(DATABASE_3_PRIVILEGED_DTO, null))
                .thenReturn(List.of(QUERY_1_DTO, QUERY_2_DTO, QUERY_3_DTO, QUERY_4_DTO, QUERY_5_DTO, QUERY_6_DTO));

        /* test */
        final List<QueryDto> response = generic_list(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO, USER_3_PRINCIPAL);
        assertEquals(6, response.size());
    }

    @Test
    @WithAnonymousUser
    public void list_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void list_publicDataAndPrivateSchemaUnavailable_fails() throws SQLException, QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .findAll(DATABASE_3_PRIVILEGED_DTO, null);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            generic_list(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_privateDataPrivateSchemaAnonymous_fails() throws DatabaseNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, QueryNotFoundException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_findById(DATABASE_1_ID, QUERY_1_ID, "application/json", null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_privateDataPrivateSchema_succeeds() throws DatabaseNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, QueryNotFoundException, MetadataServiceException, DatabaseUnavailableException, TableNotFoundException, StorageUnavailableException, NotAllowedException, ViewMalformedException, QueryMalformedException, FormatNotAvailableException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);

        /* test */
        generic_findById(DATABASE_1_ID, QUERY_1_ID, "application/json", null, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findById_publicDataPrivateSchema_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            DatabaseUnavailableException, StorageUnavailableException, QueryMalformedException, QueryNotFoundException,
            FormatNotAvailableException, TableNotFoundException, MetadataServiceException, SQLException,
            ViewMalformedException, NotAllowedException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);

        /* test */
        generic_findById(DATABASE_3_ID, QUERY_5_ID, "application/json", null, USER_3_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void findById_format_fails() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            UserNotFoundException, QueryNotFoundException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_4_ID))
                .thenReturn(DATABASE_4_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_4_PRIVILEGED_DTO, QUERY_7_ID))
                .thenReturn(QUERY_7_DTO);

        /* test */
        assertThrows(FormatNotAvailableException.class, () -> {
            generic_findById(DATABASE_4_ID, QUERY_7_ID, "application/pdf", null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_privateDataPrivateSchemaAcceptCsv_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, StorageUnavailableException, QueryMalformedException,
            QueryNotFoundException, FormatNotAvailableException, SQLException, MetadataServiceException,
            TableNotFoundException, ViewMalformedException, NotAllowedException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_5_DTO);
        when(storageService.transformDataset(any(Dataset.class)))
                .thenReturn(EXPORT_RESOURCE_DTO);
        when(subsetService.getData(any(PrivilegedDatabaseDto.class), any(QueryDto.class), eq(null), eq(null)))
                .thenReturn(mock);

        /* test */
        generic_findById(DATABASE_1_ID, QUERY_1_ID, "text/csv", null, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void findById_publicDataPrivateSchemaAnonymous_fails() throws DatabaseNotFoundException,
            RemoteUnavailableException, UserNotFoundException, StorageUnavailableException, QueryMalformedException,
            QueryNotFoundException, SQLException, MetadataServiceException, TableNotFoundException,
            ViewMalformedException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.getData(any(PrivilegedDatabaseDto.class), any(QueryDto.class), eq(null), eq(null)))
                .thenReturn(mock);
        when(storageService.transformDataset(any(Dataset.class)))
                .thenReturn(EXPORT_RESOURCE_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_findById(DATABASE_3_ID, QUERY_5_ID, "text/csv", Instant.now(), null);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_publicDataPublicSchemaAnonymous_fails() throws DatabaseNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, QueryMalformedException, StorageUnavailableException,
            QueryNotFoundException, MetadataServiceException, TableNotFoundException, ViewMalformedException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_4_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.getData(any(PrivilegedDatabaseDto.class), any(QueryDto.class), eq(null), eq(null)))
                .thenReturn(mock);
        when(storageService.transformDataset(any(Dataset.class)))
                .thenReturn(EXPORT_RESOURCE_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_findById(DATABASE_3_ID, QUERY_5_ID, "text/csv", Instant.now(), null);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_notFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(credentialService)
                .getDatabase(DATABASE_3_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_findById(DATABASE_3_ID, QUERY_5_ID, "application/json", null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findById_publicDataAndPrivateSchemaUnavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, SQLException, UserNotFoundException, QueryNotFoundException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            generic_findById(DATABASE_3_ID, QUERY_5_ID, "application/json", null, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_publicDataPrivateSchemaUnavailableExport_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, SQLException, QueryMalformedException, UserNotFoundException,
            QueryNotFoundException, TableNotFoundException, ViewMalformedException, StorageUnavailableException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(storageService.transformDataset(any(Dataset.class)))
                .thenReturn(EXPORT_RESOURCE_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .getData(eq(DATABASE_3_PRIVILEGED_DTO), eq(QUERY_5_DTO), eq(null), eq(null));

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            generic_findById(DATABASE_3_ID, QUERY_5_ID, "text/csv", null, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_noAccess_succeeds() throws UserNotFoundException, QueryStoreInsertException,
            TableMalformedException, NotAllowedException, QueryNotSupportedException, PaginationException,
            StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException, SQLException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, TableNotFoundException, ViewMalformedException, ViewNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.getData(eq(DATABASE_3_PRIVILEGED_DTO), any(QueryDto.class), eq(0L), eq(10L)))
                .thenReturn(mock);
        when(subsetService.findById(eq(DATABASE_3_PRIVILEGED_DTO), anyLong()))
                .thenReturn(QUERY_5_DTO);
        when(schemaService.inspectView(eq(DATABASE_3_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_5_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, httpServletRequest, null, 0L, 10L);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_forbiddenKeyword_fails() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT COUNT(id) FROM tbl")
                .build();

        /* mock */
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        assertThrows(QueryNotSupportedException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, httpServletRequest, null, 0L, 10L);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_noPageSize_succeeds() throws UserNotFoundException, QueryStoreInsertException,
            TableMalformedException, NotAllowedException, QueryNotSupportedException,
            PaginationException, StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            SQLException, MetadataServiceException, TableNotFoundException, ViewMalformedException,
            ViewNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(schemaService.inspectView(eq(DATABASE_3_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_5_DTO);
        when(subsetService.findById(eq(DATABASE_3_PRIVILEGED_DTO), anyLong()))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.getData(eq(DATABASE_3_PRIVILEGED_DTO), any(QueryDto.class), eq(0L), eq(10L)))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, httpServletRequest, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_unavailable_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            SQLException, MetadataServiceException, QueryMalformedException, TableNotFoundException,
            ViewMalformedException, UserNotFoundException, QueryNotFoundException, QueryStoreInsertException,
            NotAllowedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .getData(eq(DATABASE_3_PRIVILEGED_DTO), any(QueryDto.class), eq(null), eq(null));
        when(subsetService.findById(eq(DATABASE_3_PRIVILEGED_DTO), anyLong()))
                .thenReturn(QUERY_5_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_READ_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .create(eq(DATABASE_3_PRIVILEGED_DTO), eq(QUERY_5_STATEMENT), any(Instant.class), eq(USER_1_ID));
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, httpServletRequest, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_databaseNotFound_fails() throws RemoteUnavailableException,
            DatabaseNotFoundException, MetadataServiceException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(credentialService)
                .getDatabase(DATABASE_3_ID);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL, httpServletRequest, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, UserNotFoundException, QueryStoreInsertException, TableMalformedException,
            NotAllowedException, SQLException, QueryNotFoundException, DatabaseUnavailableException,
            StorageUnavailableException, QueryMalformedException, QueryNotSupportedException, PaginationException,
            StorageNotFoundException, TableNotFoundException, ViewMalformedException, ViewNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.getData(eq(DATABASE_3_PRIVILEGED_DTO), any(QueryDto.class), eq(0L), eq(10L)))
                .thenReturn(mock);
        when(subsetService.findById(eq(DATABASE_3_PRIVILEGED_DTO), anyLong()))
                .thenReturn(QUERY_5_DTO);
        when(schemaService.inspectView(eq(DATABASE_3_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_5_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_3_ID, request, USER_4_PRINCIPAL, httpServletRequest, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void create_publicDataPublicSchemaAnonymous_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, UserNotFoundException, QueryStoreInsertException, TableMalformedException,
            NotAllowedException, SQLException, QueryNotFoundException, DatabaseUnavailableException,
            StorageUnavailableException, QueryMalformedException, QueryNotSupportedException, PaginationException,
            StorageNotFoundException, TableNotFoundException, ViewMalformedException, ViewNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(credentialService.getDatabase(DATABASE_4_ID))
                .thenReturn(DATABASE_4_PRIVILEGED_DTO);
        when(subsetService.findById(eq(DATABASE_4_PRIVILEGED_DTO), anyLong()))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.getData(eq(DATABASE_4_PRIVILEGED_DTO), any(QueryDto.class), eq(0L), eq(10L)))
                .thenReturn(mock);
        when(schemaService.inspectView(eq(DATABASE_4_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_5_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_4_ID, request, null, httpServletRequest, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_privateDataPrivateSchema_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, UserNotFoundException, QueryStoreInsertException, TableMalformedException,
            NotAllowedException, SQLException, QueryNotFoundException, DatabaseUnavailableException,
            StorageUnavailableException, QueryMalformedException, QueryNotSupportedException, PaginationException,
            StorageNotFoundException, TableNotFoundException, ViewMalformedException, ViewNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(eq(DATABASE_1_PRIVILEGED_DTO), anyLong()))
                .thenReturn(QUERY_1_DTO);
        when(subsetService.getData(eq(DATABASE_1_PRIVILEGED_DTO), any(QueryDto.class), eq(0L), eq(10L)))
                .thenReturn(mock);
        when(schemaService.inspectView(eq(DATABASE_1_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_1_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_1_ID, request, USER_1_PRINCIPAL, httpServletRequest, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void create_privateDataPublicSchemaAnonymous_fails() throws DatabaseNotFoundException, SQLException,
            MetadataServiceException, UserNotFoundException, QueryNotFoundException, QueryMalformedException,
            TableNotFoundException, ViewMalformedException, ViewNotFoundException, RemoteUnavailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_5_STATEMENT)
                .build();

        /* mock */
        when(credentialService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_PRIVILEGED_DTO);
        when(subsetService.findById(eq(DATABASE_2_PRIVILEGED_DTO), anyLong()))
                .thenReturn(QUERY_2_DTO);
        when(subsetService.getData(eq(DATABASE_2_PRIVILEGED_DTO), any(QueryDto.class), eq(0L), eq(10L)))
                .thenReturn(mock);
        when(schemaService.inspectView(eq(DATABASE_2_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_4_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.create(DATABASE_2_ID, request, null, httpServletRequest, null, null, null);
        });
    }

    @Test
    public void getData_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            NotAllowedException, SQLException, QueryNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseUnavailableException, PaginationException, MetadataServiceException, TableNotFoundException,
            ViewMalformedException, ViewNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.reExecuteCount(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO))
                .thenReturn(QUERY_5_RESULT_NUMBER);
        when(subsetService.getData(eq(DATABASE_3_PRIVILEGED_DTO), any(QueryDto.class), eq(0L), eq(10L)))
                .thenReturn(mock);
        when(schemaService.inspectView(eq(DATABASE_3_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_5_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<List<Map<String, Object>>> response = subsetEndpoint.getData(DATABASE_3_ID, QUERY_5_ID, null, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void getData_head_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, NotAllowedException, SQLException, QueryNotFoundException, TableMalformedException,
            QueryMalformedException, DatabaseUnavailableException, PaginationException, MetadataServiceException,
            TableNotFoundException, ViewMalformedException, ViewNotFoundException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_3_ID))
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

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_private_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, NotAllowedException, TableMalformedException,
            QueryMalformedException, QueryNotFoundException, PaginationException, SQLException,
            MetadataServiceException, TableNotFoundException, ViewMalformedException, ViewNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);
        when(subsetService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO))
                .thenReturn(QUERY_1_RESULT_NUMBER);
        when(subsetService.getData(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, 0L, 10L))
                .thenReturn(mock);
        when(schemaService.inspectView(eq(DATABASE_1_PRIVILEGED_DTO), anyString()))
                .thenReturn(VIEW_1_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<List<Map<String, Object>>> response = subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithAnonymousUser
    public void getData_privateAnonymous_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
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
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(NotAllowedException.class)
                .when(credentialService)
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
            MetadataServiceException, TableNotFoundException, ViewMalformedException, ViewNotFoundException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
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

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            UserNotFoundException, QueryNotFoundException, MetadataServiceException, QueryMalformedException,
            TableNotFoundException, ViewMalformedException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");
        doThrow(SQLException.class)
                .when(subsetService)
                .getData(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, 0L, 10L);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"persist-query"})
    public void persist_succeeds() throws NotAllowedException, RemoteUnavailableException, DatabaseNotFoundException,
            QueryStorePersistException, SQLException, UserNotFoundException, QueryNotFoundException,
            DatabaseUnavailableException, MetadataServiceException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        when(credentialService.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        when(credentialService.getDatabase(DATABASE_3_ID))
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
                .when(credentialService)
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
        when(credentialService.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        doThrow(DatabaseNotFoundException.class)
                .when(credentialService)
                .getDatabase(DATABASE_3_ID);

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
        when(credentialService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(credentialService.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .persist(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID, true);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            subsetEndpoint.persist(DATABASE_3_ID, QUERY_5_ID, request, USER_3_PRINCIPAL);
        });
    }

    protected List<QueryDto> generic_list(Long databaseId, PrivilegedDatabaseDto database, Principal principal)
            throws NotAllowedException, DatabaseUnavailableException, QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        if (database != null) {
            when(credentialService.getDatabase(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(credentialService)
                    .getDatabase(databaseId);
        }

        /* test */
        final ResponseEntity<List<QueryDto>> response = subsetEndpoint.list(databaseId, null, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    protected void generic_findById(Long databaseId, Long subsetId, String accept, Instant timestamp,
                                    Principal principal) throws UserNotFoundException, DatabaseUnavailableException,
            StorageUnavailableException, NotAllowedException, QueryMalformedException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, FormatNotAvailableException,
            MetadataServiceException, TableNotFoundException, ViewMalformedException {

        /* test */
        final ResponseEntity<?> response = subsetEndpoint.findById(databaseId, subsetId, accept, timestamp, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
