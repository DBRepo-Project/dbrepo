package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryPersistDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SubsetEndpointUnitTest extends BaseTest {

    @Autowired
    private SubsetEndpoint subsetEndpoint;

    @Autowired
    private SparkSession sparkSession;

    @MockitoBean
    private SubsetService subsetService;

    @MockitoBean
    private HttpServletRequest httpServletRequest;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private DatabaseService databaseService;

    @MockitoBean
    private CacheService cacheService;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @Test
    @WithAnonymousUser
    public void list_publicDataPrivateSchemaAnonymous_succeeds() throws QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, SQLException, MetadataServiceException,
            DatabaseUnavailableException, NotAllowedException, UserNotFoundException {

        /* mock */
        when(subsetService.findAll(DATABASE_3_PRIVILEGED_DTO, null))
                .thenReturn(List.of(QUERY_3_DTO, QUERY_4_DTO, QUERY_5_DTO));

        /* test */
        final List<QueryDto> response = generic_list(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO, null);
        assertEquals(3, response.size());
        assertEquals(QUERY_3_DTO, response.get(0));
        assertEquals(QUERY_4_DTO, response.get(1));
        assertEquals(QUERY_5_DTO, response.get(2));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void list_publicDataPrivateSchema_succeeds() throws DatabaseUnavailableException, NotAllowedException,
            QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            MetadataServiceException, UserNotFoundException {

        /* mock */
        when(cacheService.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_1_READ_ACCESS_DTO);
        when(subsetService.findAll(DATABASE_3_PRIVILEGED_DTO, null))
                .thenReturn(List.of(QUERY_1_DTO, QUERY_2_DTO, QUERY_3_DTO, QUERY_4_DTO, QUERY_5_DTO, QUERY_6_DTO));

        /* test */
        final List<QueryDto> response = generic_list(DATABASE_3_ID, DATABASE_3_PRIVILEGED_DTO, USER_3_PRINCIPAL);
        assertEquals(6, response.size());
        assertEquals(QUERY_1_DTO, response.get(0));
        assertEquals(QUERY_2_DTO, response.get(1));
        assertEquals(QUERY_3_DTO, response.get(2));
        assertEquals(QUERY_4_DTO, response.get(3));
        assertEquals(QUERY_5_DTO, response.get(4));
        assertEquals(QUERY_6_DTO, response.get(5));
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
    public void list_publicDataAndPrivateSchemaUnavailable_fails() throws SQLException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException, UserNotFoundException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID))
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
    public void findById_privateDataPrivateSchemaAnonymous_fails() throws DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_findById(DATABASE_1_ID, QUERY_1_ID, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_privateDataPrivateSchema_succeeds() throws DatabaseNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, DatabaseUnavailableException, NotAllowedException,
            QueryNotFoundException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);

        /* test */
        generic_findById(DATABASE_1_ID, QUERY_1_ID, null, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_privateDataPrivateSchemaAcceptEmpty_succeeds() throws DatabaseNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, DatabaseUnavailableException, NotAllowedException,
            QueryNotFoundException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);

        /* test */
        generic_findById(DATABASE_1_ID, QUERY_1_ID, null, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findById_publicDataPrivateSchema_succeeds() throws DatabaseNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, DatabaseUnavailableException, NotAllowedException,
            QueryNotFoundException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);

        /* test */
        generic_findById(DATABASE_3_ID, QUERY_5_ID, null, USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_privateDataPrivateSchemaAcceptCsv_succeeds() throws DatabaseNotFoundException,
            RemoteUnavailableException, UserNotFoundException, DatabaseUnavailableException,
            QueryNotFoundException, SQLException, MetadataServiceException, NotAllowedException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_5_DTO);

        /* test */
        generic_findById(DATABASE_1_ID, QUERY_1_ID, null, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void findById_publicDataPrivateSchemaAnonymous_succeeds() throws DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException, UserNotFoundException, DatabaseUnavailableException,
            NotAllowedException, QueryNotFoundException, SQLException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);

        /* test */
        generic_findById(DATABASE_3_ID, QUERY_5_ID, Instant.now(), null);
    }

    @Test
    @WithAnonymousUser
    public void findById_publicDataPublicSchemaAnonymous_succeeds() throws DatabaseNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, QueryMalformedException, StorageUnavailableException,
            QueryNotFoundException, MetadataServiceException, TableNotFoundException, DatabaseUnavailableException,
            NotAllowedException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(storageService.transformDataset(any(Dataset.class)))
                .thenReturn(EXPORT_RESOURCE_DTO);

        /* test */
        generic_findById(DATABASE_3_ID, QUERY_5_ID, Instant.now(), null);
    }

    @Test
    @WithAnonymousUser
    public void findById_notFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(cacheService)
                .getDatabase(DATABASE_3_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_findById(DATABASE_3_ID, QUERY_5_ID, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findById_publicDataAndPrivateSchemaUnavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, SQLException, UserNotFoundException, QueryNotFoundException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            generic_findById(DATABASE_3_ID, QUERY_5_ID, null, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_noAccess_succeeds() throws UserNotFoundException, QueryStoreInsertException,
            TableMalformedException, NotAllowedException, QueryNotSupportedException, PaginationException,
            StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException, SQLException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, TableNotFoundException, ViewMalformedException, ViewNotFoundException,
            ImageNotFoundException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID, true))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(subsetService.findById(any(DatabaseDto.class), any(UUID.class)))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.create(any(DatabaseDto.class), any(SubsetDto.class), any(Instant.class), any(UUID.class)))
                .thenReturn(QUERY_5_ID);
        when(databaseService.inspectView(any(DatabaseDto.class), anyString()))
                .thenReturn(QUERY_5_VIEW_DTO);
        when(metadataServiceGateway.getIdentifiers(DATABASE_3_ID, QUERY_5_ID))
                .thenReturn(List.of());
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_3_ID, QUERY_5_SUBSET_DTO, USER_1_PRINCIPAL, httpServletRequest, null, 0L, 10L);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_noPageSize_succeeds() throws UserNotFoundException, QueryStoreInsertException,
            TableMalformedException, NotAllowedException, QueryNotSupportedException,
            PaginationException, StorageNotFoundException, DatabaseUnavailableException, StorageUnavailableException,
            QueryMalformedException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            SQLException, MetadataServiceException, TableNotFoundException, ViewMalformedException,
            ViewNotFoundException, ImageNotFoundException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID, true))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.create(any(DatabaseDto.class), any(SubsetDto.class), any(Instant.class), eq(USER_1_ID)))
                .thenReturn(QUERY_5_ID);
        when(subsetService.findById(any(DatabaseDto.class), eq(QUERY_5_ID)))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(databaseService.createView(any(DatabaseDto.class), anyString(), anyString()))
                .thenReturn(QUERY_5_VIEW_DTO);
        when(databaseService.inspectView(any(DatabaseDto.class), anyString()))
                .thenReturn(QUERY_5_VIEW_DTO);
        when(metadataServiceGateway.getIdentifiers(DATABASE_3_ID, QUERY_5_ID))
                .thenReturn(List.of());
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_3_ID, QUERY_5_SUBSET_DTO, USER_1_PRINCIPAL, httpServletRequest, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void create_databaseNotFound_fails() throws RemoteUnavailableException,
            DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(cacheService)
                .getDatabase(DATABASE_3_ID, true);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            subsetEndpoint.create(DATABASE_3_ID, QUERY_5_SUBSET_DTO, USER_1_PRINCIPAL, httpServletRequest, null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_publicDataPublicSchemaAnonymous_succeeds() throws DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException, UserNotFoundException, QueryStoreInsertException,
            TableMalformedException, NotAllowedException, SQLException, QueryNotFoundException, PaginationException,
            DatabaseUnavailableException, StorageUnavailableException, QueryMalformedException,
            QueryNotSupportedException, StorageNotFoundException, TableNotFoundException, ViewMalformedException,
            ViewNotFoundException, ImageNotFoundException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_4_ID, true))
                .thenReturn(DATABASE_4_PRIVILEGED_DTO);
        when(cacheService.getDatabase(DATABASE_4_ID))
                .thenReturn(DATABASE_4_PRIVILEGED_DTO);
        when(subsetService.findById(eq(DATABASE_4_PRIVILEGED_DTO), any(UUID.class)))
                .thenReturn(QUERY_9_DTO);
        when(subsetService.create(eq(DATABASE_4_PRIVILEGED_DTO), any(SubsetDto.class), any(Instant.class), eq(null)))
                .thenReturn(QUERY_9_ID);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(databaseService.inspectView(any(DatabaseDto.class), anyString()))
                .thenReturn(QUERY_9_VIEW_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_4_ID, QUERY_9_SUBSET_DTO, null, httpServletRequest, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_privateDataPrivateSchema_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, UserNotFoundException, QueryStoreInsertException, TableMalformedException,
            NotAllowedException, SQLException, QueryNotFoundException, DatabaseUnavailableException,
            StorageUnavailableException, QueryMalformedException, QueryNotSupportedException, PaginationException,
            StorageNotFoundException, TableNotFoundException, ViewMalformedException, ViewNotFoundException,
            ImageNotFoundException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(any(DatabaseDto.class), any(UUID.class)))
                .thenReturn(QUERY_1_DTO);
        when(subsetService.create(any(DatabaseDto.class), any(SubsetDto.class), any(Instant.class), any(UUID.class)))
                .thenReturn(QUERY_1_ID);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(databaseService.inspectView(any(DatabaseDto.class), anyString()))
                .thenReturn(QUERY_1_VIEW_DTO);
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_1_BRIEF_DTO, IDENTIFIER_2_BRIEF_DTO));
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        subsetEndpoint.create(DATABASE_1_ID, QUERY_1_SUBSET_DTO, USER_1_PRINCIPAL, httpServletRequest, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void create_privateDataPublicSchemaAnonymous_fails() throws DatabaseNotFoundException, SQLException,
            MetadataServiceException, UserNotFoundException, QueryNotFoundException, QueryMalformedException,
            TableNotFoundException, RemoteUnavailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_2_ID, true))
                .thenReturn(DATABASE_2_PRIVILEGED_DTO);
        when(cacheService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_PRIVILEGED_DTO);
        when(subsetService.findById(eq(DATABASE_2_PRIVILEGED_DTO), any(UUID.class)))
                .thenReturn(QUERY_8_DTO);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(metadataServiceGateway.getIdentifiers(DATABASE_2_ID, QUERY_8_ID))
                .thenReturn(List.of());
        when(httpServletRequest.getMethod())
                .thenReturn("POST");

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.create(DATABASE_2_ID, QUERY_8_SUBSET_DTO, null, httpServletRequest, null, null, null);
        });
    }

    @Test
    public void getData_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            NotAllowedException, SQLException, QueryNotFoundException, QueryMalformedException,
            DatabaseUnavailableException, PaginationException, MetadataServiceException, TableNotFoundException,
            ViewNotFoundException, ViewMalformedException, StorageUnavailableException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.reExecuteCount(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO))
                .thenReturn(QUERY_5_RESULT_NUMBER);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(databaseService.inspectView(any(DatabaseDto.class), anyString()))
                .thenReturn(QUERY_5_VIEW_DTO);
        when(metadataServiceGateway.getIdentifiers(DATABASE_3_ID, QUERY_5_ID))
                .thenReturn(List.of());
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = subsetEndpoint.getData(DATABASE_3_ID, QUERY_5_ID,
                null, "application/json", httpServletRequest, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void getData_head_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, NotAllowedException, SQLException, QueryNotFoundException, QueryMalformedException,
            DatabaseUnavailableException, PaginationException, MetadataServiceException, TableNotFoundException,
            ViewNotFoundException, ViewMalformedException, StorageUnavailableException, FormatNotAvailableException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);
        when(subsetService.reExecuteCount(DATABASE_3_PRIVILEGED_DTO, QUERY_5_DTO))
                .thenReturn(QUERY_5_RESULT_NUMBER);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<?> response = subsetEndpoint.getData(DATABASE_3_ID, QUERY_5_ID,
                null, "application/json", httpServletRequest, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_5_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_private_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, NotAllowedException, QueryMalformedException,
            QueryNotFoundException, PaginationException, SQLException, MetadataServiceException,
            TableNotFoundException, ViewNotFoundException, ViewMalformedException, StorageUnavailableException,
            FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);
        when(subsetService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO))
                .thenReturn(QUERY_1_RESULT_NUMBER);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(databaseService.inspectView(any(DatabaseDto.class), anyString()))
                .thenReturn(QUERY_1_VIEW_DTO);
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_1_BRIEF_DTO, IDENTIFIER_2_BRIEF_DTO));
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID,
                USER_1_PRINCIPAL, "application/json", httpServletRequest, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithAnonymousUser
    public void getData_privateAnonymous_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, null, "application/json", httpServletRequest, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_privateNoAccess_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(NotAllowedException.class)
                .when(cacheService)
                .getAccess(DATABASE_1_ID, USER_1_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, "application/json", httpServletRequest, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getData_privateHead_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseUnavailableException, NotAllowedException, QueryMalformedException,
            QueryNotFoundException, PaginationException, SQLException, MetadataServiceException,
            TableNotFoundException, ViewNotFoundException, ViewMalformedException, StorageUnavailableException,
            FormatNotAvailableException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);
        when(subsetService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO))
                .thenReturn(QUERY_1_RESULT_NUMBER);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<?> response = subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID,
                USER_1_PRINCIPAL, "GET", httpServletRequest, null, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(QUERY_1_RESULT_NUMBER, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
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
        when(cacheService.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        when(cacheService.getDatabase(DATABASE_3_ID))
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
                .when(cacheService)
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
        when(cacheService.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        doThrow(DatabaseNotFoundException.class)
                .when(cacheService)
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
        when(cacheService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(cacheService.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(subsetService)
                .persist(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID, true);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            subsetEndpoint.persist(DATABASE_3_ID, QUERY_5_ID, request, USER_3_PRINCIPAL);
        });
    }

    protected List<QueryDto> generic_list(UUID databaseId, DatabaseDto database, Principal principal)
            throws NotAllowedException, DatabaseUnavailableException, QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException, UserNotFoundException {

        /* mock */
        if (database != null) {
            when(cacheService.getDatabase(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(cacheService)
                    .getDatabase(databaseId);
        }

        /* test */
        final ResponseEntity<List<QueryDto>> response = subsetEndpoint.list(databaseId, null, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    protected void generic_findById(UUID databaseId, UUID subsetId, Instant timestamp, Principal principal)
            throws UserNotFoundException, DatabaseUnavailableException, NotAllowedException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException {

        /* test */
        final ResponseEntity<?> response = subsetEndpoint.findById(databaseId, subsetId, timestamp, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
