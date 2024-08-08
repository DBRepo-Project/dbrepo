package at.tuwien.endpoint;

import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.endpoints.ViewEndpoint;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.ViewService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewEndpointUnitTest extends AbstractUnitTest {

    @MockBean
    private ViewService viewService;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockBean
    private HttpServletRequest httpServletRequest;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, DatabaseUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(viewService.create(DATABASE_1_PRIVILEGED_DTO, VIEW_1_CREATE_DTO))
                .thenReturn(VIEW_1_DTO);

        /* test */
        final ResponseEntity<ViewDto> response = viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(viewService.create(DATABASE_1_PRIVILEGED_DTO, VIEW_1_CREATE_DTO))
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
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, DatabaseUnavailableException, ViewNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(viewService)
                .delete(VIEW_1_PRIVILEGED_DTO);

        /* test */
        final ResponseEntity<Void> response = viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void delete_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException,
            SQLException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(viewService)
                .delete(VIEW_1_PRIVILEGED_DTO);

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
                .when(metadataServiceGateway)
                .getViewById(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_succeeds() throws RemoteUnavailableException, ViewNotFoundException, ViewMalformedException,
            SQLException, DatabaseUnavailableException, QueryMalformedException, PaginationException,
            NotAllowedException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");
        when(viewService.data(eq(VIEW_1_PRIVILEGED_DTO), any(Instant.class), eq(0L), eq(10L)))
                .thenReturn(VIEW_1_DATA_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Access-Control-Expose-Headers"));
        assertEquals(1, response.getHeaders().get("Access-Control-Expose-Headers").size());
        assertEquals("X-Count", response.getHeaders().get("Access-Control-Expose-Headers").get(0));
        assertNotNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_onlyHead_succeeds() throws RemoteUnavailableException, ViewNotFoundException,
            ViewMalformedException, SQLException, DatabaseUnavailableException, QueryMalformedException,
            PaginationException, NotAllowedException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");
        when(viewService.count(eq(VIEW_1_PRIVILEGED_DTO), any(Instant.class)))
                .thenReturn(VIEW_1_DATA_COUNT);

        /* test */
        final ResponseEntity<QueryResultDto> response = viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals(1, response.getHeaders().get("X-Count").size());
        assertEquals(VIEW_1_DATA_COUNT, Long.parseLong(response.getHeaders().get("X-Count").get(0)));
        assertNotNull(response.getHeaders().get("Access-Control-Expose-Headers"));
        assertEquals(1, response.getHeaders().get("Access-Control-Expose-Headers").size());
        assertEquals("X-Count", response.getHeaders().get("Access-Control-Expose-Headers").get(0));
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void getData_viewNotFound_fails() throws RemoteUnavailableException, ViewNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(ViewNotFoundException.class)
                .when(metadataServiceGateway)
                .getViewById(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, null, null, null, httpServletRequest, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"view-database-view-data"})
    public void getData_privateNoAccess_fails() throws RemoteUnavailableException, ViewNotFoundException,
            NotAllowedException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_3_ID))
                .thenReturn(VIEW_3_PRIVILEGED_DTO);
        doThrow(NotAllowedException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_1_ID, USER_3_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_3_ID, null, null, null, httpServletRequest, USER_3_PRINCIPAL);
        });
    }

}
