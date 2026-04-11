package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.ViewEndpoint;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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

    @Autowired
    private ViewEndpoint viewEndpoint;

    @MockitoBean
    private ViewService viewService;

    @MockitoBean
    private TableService tableService;

    @MockitoBean
    private DatabaseService databaseService;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private HttpServletRequest httpServletRequest;

    @MockitoBean
    private SubsetService subsetService;

    @MockitoBean
    private DataMapper dataMapper;

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, DatabaseUnavailableException, MetadataServiceException, TableNotFoundException,
            ImageNotFoundException, QueryMalformedException, ViewNotFoundException, ColumnNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(viewService.create(any(Database.class), anyString(), anyString(), anyBoolean()))
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
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(SQLException.class)
                .when(viewService)
                .create(any(Database.class), anyString(), anyString(), anyBoolean());

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            ViewMalformedException, SQLException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(viewService.create(DATABASE_1_CACHE, VIEW_1_NAME, VIEW_1_QUERY, false))
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
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void findAll_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            DatabaseMalformedException, DatabaseUnavailableException, ViewNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(viewService.explore(DATABASE_1_CACHE))
                .thenReturn(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO));

        /* test */
        final ResponseEntity<List<ViewDto>> response = viewEndpoint.findAll(DATABASE_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            viewEndpoint.findAll(DATABASE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void findAll_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            viewEndpoint.findAll(DATABASE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void findAll_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            SQLException, DatabaseMalformedException, ViewNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(SQLException.class)
                .when(viewService)
                .explore(DATABASE_1_CACHE);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            viewEndpoint.findAll(DATABASE_1_ID);
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
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doNothing()
                .when(viewService)
                .delete(DATABASE_1_CACHE, VIEW_1_CACHE);

        /* test */
        final ResponseEntity<Void> response = viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_unavailable_fails() throws RemoteUnavailableException, ViewMalformedException, SQLException,
            MetadataServiceException, ViewNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        doThrow(SQLException.class)
                .when(viewService)
                .delete(DATABASE_1_CACHE, VIEW_1_CACHE);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void delete_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            ViewMalformedException, SQLException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doNothing()
                .when(viewService)
                .delete(DATABASE_1_CACHE, VIEW_1_CACHE);

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
                .when(metadataService)
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
            MetadataServiceException, TableNotFoundException, DatabaseNotFoundException, ViewMalformedException,
            FormatNotAvailableException, MalformedException, ColumnNotFoundException, StorageNotFoundException,
            ImageInvalidException, AnalyseDataTypesException, DatabaseMalformedException {

        /* mock */
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
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
            ViewNotFoundException, MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
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
            NotAllowedException, MetadataServiceException, TableNotFoundException, DatabaseNotFoundException,
            ViewMalformedException, FormatNotAvailableException, MalformedException, ColumnNotFoundException,
            StorageNotFoundException, ImageInvalidException, AnalyseDataTypesException, DatabaseMalformedException {

        /* mock */
        when(metadataService.getView(DATABASE_1_ID, VIEW_3_ID))
                .thenReturn(VIEW_3_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");
        when(viewService.count(any(Database.class), eq(VIEW_3_CACHE), any(Instant.class)))
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
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

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
                .when(metadataService)
                .getView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "text/csv", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateNoAccess_fails() throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "application/json", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateNoAccessTextCsv_fails() throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

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
                .when(metadataService)
                .getView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, "application/json", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getStatistic_succeeds() throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException, DatabaseUnavailableException, TableNotFoundException, TableMalformedException,
            NotAllowedException, DatabaseNotFoundException, SQLException {

        /* mock */
        when(metadataService.getView(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(tableService.getStatistics(DATABASE_1_CACHE, VIEW_1_ID, VIEW_1_INTERNAL_NAME))
                .thenReturn(VIEW_1_STATISTIC_DTO);

        /* test */
        final ResponseEntity<TableStatisticDto> response = viewEndpoint.getStatistic(DATABASE_1_ID, VIEW_1_ID, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
