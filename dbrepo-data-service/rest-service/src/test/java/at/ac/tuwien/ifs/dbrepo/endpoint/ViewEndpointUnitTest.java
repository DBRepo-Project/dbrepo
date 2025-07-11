package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.ViewEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewEndpointUnitTest extends BaseTest {

    @MockitoBean
    private ViewService viewService;

    @MockitoBean
    private DatabaseService databaseService;

    @MockitoBean
    private CacheService cacheService;

    @MockitoBean
    private HttpServletRequest httpServletRequest;

    @MockitoBean
    private SubsetService subsetService;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @Autowired
    private SparkSession sparkSession;

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, DatabaseUnavailableException, MetadataServiceException, TableNotFoundException,
            ImageNotFoundException, QueryMalformedException, ViewNotFoundException, ColumnNotFoundException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_DTO);
        when(databaseService.createView(any(DatabaseDto.class), anyString(), anyString()))
                .thenReturn(VIEW_1_DTO);

        /* test */
        final ResponseEntity<ViewDto> response = viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            ViewMalformedException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_DTO);
        doThrow(SQLException.class)
                .when(databaseService)
                .createView(any(DatabaseDto.class), anyString(), anyString());

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_DTO);
        when(databaseService.createView(DATABASE_1_DTO, VIEW_1_NAME, VIEW_1_QUERY))
                .thenReturn(VIEW_1_DTO);

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(cacheService)
                .getDatabase(DATABASE_1_ID, true);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void getSchema_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            DatabaseMalformedException, DatabaseUnavailableException, ViewNotFoundException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(databaseService.exploreViews(DATABASE_1_DTO))
                .thenReturn(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO));

        /* test */
        final ResponseEntity<List<ViewDto>> response = viewEndpoint.getSchema(DATABASE_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getSchema_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            viewEndpoint.getSchema(DATABASE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void getSchema_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(cacheService)
                .getDatabase(DATABASE_1_ID, true);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            viewEndpoint.getSchema(DATABASE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void getSchema_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            SQLException, DatabaseMalformedException, ViewNotFoundException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_DTO);
        doThrow(SQLException.class)
                .when(databaseService)
                .exploreViews(DATABASE_1_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            viewEndpoint.getSchema(DATABASE_1_ID);
        });
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_succeeds() throws RemoteUnavailableException, ViewMalformedException, ViewNotFoundException,
            SQLException, DatabaseUnavailableException, MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(cacheService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(viewService)
                .delete(DATABASE_1_PRIVILEGED_DTO, VIEW_1_DTO);

        /* test */
        final ResponseEntity<Void> response = viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_unavailable_fails() throws RemoteUnavailableException, ViewMalformedException, SQLException,
            MetadataServiceException, ViewNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(cacheService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);
        doThrow(SQLException.class)
                .when(viewService)
                .delete(DATABASE_1_PRIVILEGED_DTO, VIEW_1_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void delete_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(viewService)
                .delete(DATABASE_1_PRIVILEGED_DTO, VIEW_1_DTO);

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_databaseNotFound_fails() throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(ViewNotFoundException.class)
                .when(cacheService)
                .getView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateDataPrivateSchema_succeeds() throws RemoteUnavailableException, ViewNotFoundException,
            DatabaseUnavailableException, QueryMalformedException, PaginationException, NotAllowedException,
            MetadataServiceException, TableNotFoundException, DatabaseNotFoundException, ViewMalformedException, StorageUnavailableException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_DTO);
        when(cacheService.getAccess(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "application/json", USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithAnonymousUser
    public void getData_privateDataPrivateSchemaAnonymous_fails() throws RemoteUnavailableException,
            ViewNotFoundException, QueryMalformedException, NotAllowedException, MetadataServiceException,
            TableNotFoundException, DatabaseNotFoundException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(cacheService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(cacheService.getAccess(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO);
        when(subsetService.getData(any(DatabaseDto.class), anyString()))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "application/json", null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateHead_succeeds() throws RemoteUnavailableException, ViewNotFoundException,
            SQLException, DatabaseUnavailableException, QueryMalformedException, PaginationException,
            NotAllowedException, MetadataServiceException, TableNotFoundException, DatabaseNotFoundException, ViewMalformedException, StorageUnavailableException, FormatNotAvailableException {

        /* mock */
        when(cacheService.getView(DATABASE_1_ID, VIEW_3_ID))
                .thenReturn(VIEW_3_DTO);
        when(cacheService.getDatabase(DATABASE_1_ID, true))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(cacheService.getAccess(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");
        when(viewService.count(any(DatabaseDto.class), eq(VIEW_3_DTO), any(Instant.class)))
                .thenReturn(VIEW_3_DATA_COUNT);

        /* test */
        final ResponseEntity<?> response = viewEndpoint.getData(DATABASE_1_ID, VIEW_3_ID, null, null, null, httpServletRequest, "application/json", USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(VIEW_3_DATA_COUNT, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
        assertNotNull(response.getHeaders().get("Access-Control-Expose-Headers"));
        assertEquals(1, response.getHeaders().get("Access-Control-Expose-Headers").size());
        assertEquals("X-Count", response.getHeaders().get("Access-Control-Expose-Headers").get(0));
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateNoAccess_succeeds() throws RemoteUnavailableException, ViewNotFoundException,
            NotAllowedException, MetadataServiceException {

        /* mock */
        when(cacheService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");
        doThrow(NotAllowedException.class)
                .when(cacheService)
                .getAccess(DATABASE_1_ID, USER_4_USERNAME);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "application/json", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_viewNotFoundTextCsv_fails() throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(ViewNotFoundException.class)
                .when(cacheService)
                .getView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "text/csv", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateNoAccess_fails() throws RemoteUnavailableException, ViewNotFoundException,
            NotAllowedException, MetadataServiceException {

        /* mock */
        when(cacheService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);
        doThrow(NotAllowedException.class)
                .when(cacheService)
                .getAccess(DATABASE_1_ID, USER_4_USERNAME);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "application/json", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateNoAccessTextCsv_fails() throws RemoteUnavailableException, ViewNotFoundException,
            NotAllowedException, MetadataServiceException {

        /* mock */
        when(cacheService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);
        doThrow(NotAllowedException.class)
                .when(cacheService)
                .getAccess(DATABASE_1_ID, USER_4_USERNAME);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "text/csv", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_viewNotFound_fails() throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(ViewNotFoundException.class)
                .when(cacheService)
                .getView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "application/json", USER_4_PRINCIPAL);
        });
    }

}
