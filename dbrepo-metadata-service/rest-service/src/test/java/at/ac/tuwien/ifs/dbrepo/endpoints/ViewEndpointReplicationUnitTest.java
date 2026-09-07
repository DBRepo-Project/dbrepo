package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewEndpointReplicationUnitTest extends BaseTest {

    @MockitoBean
    private DatabaseService databaseService;

    @MockitoBean
    private ViewService viewService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private ReplicationService replicationService;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void replicate_existingReplica_returnsExistingView() throws DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, DashboardServiceException,
            SearchServiceConnectionException, DataServiceException, SearchServiceException,
            DashboardServiceConnectionException, ViewNotFoundException {
        final ViewNotificationDto notification = ViewNotificationDto.builder()
                .creationId(VIEW_1_ID)
                .viewDto(VIEW_1_DTO)
                .build();

        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(viewService.findById(DATABASE_1, VIEW_1_ID))
                .thenReturn(VIEW_1);

        final ResponseEntity<ViewBriefDto> response = viewEndpoint.replicate(DATABASE_1_ID, notification,
                USER_1_PRINCIPAL);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        verify(viewService, never()).createReplicated(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void replicate_missingViewId_usesCreationId() throws DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, DashboardServiceException,
            SearchServiceConnectionException, DataServiceException, SearchServiceException,
            DashboardServiceConnectionException, ViewNotFoundException {
        final ViewDto request = VIEW_1_DTO.toBuilder()
                .id(null)
                .build();
        final ViewNotificationDto notification = ViewNotificationDto.builder()
                .creationId(VIEW_1_ID)
                .viewDto(request)
                .build();

        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(viewService.findById(DATABASE_1, VIEW_1_ID))
                .thenThrow(new ViewNotFoundException("not found"));
        when(viewService.createReplicated(DATABASE_1, USER_1_USERNAME, request))
                .thenReturn(VIEW_1);

        final ResponseEntity<ViewBriefDto> response = viewEndpoint.replicate(DATABASE_1_ID, notification,
                USER_1_PRINCIPAL);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(VIEW_1_ID, request.getId());
        verify(viewService).createReplicated(eq(DATABASE_1), eq(USER_1_USERNAME), eq(request));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void replicate_mismatchedViewId_fails() throws DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceException, SearchServiceException, ViewNotFoundException {
        final ViewNotificationDto notification = ViewNotificationDto.builder()
                .creationId(VIEW_1_ID)
                .viewDto(VIEW_1_DTO.toBuilder()
                        .id(VIEW_2_ID)
                        .build())
                .build();

        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(viewService.findById(DATABASE_1, VIEW_1_ID))
                .thenThrow(new ViewNotFoundException("not found"));

        assertThrows(MalformedException.class, () -> {
            viewEndpoint.replicate(DATABASE_1_ID, notification, USER_1_PRINCIPAL);
        });
        verify(viewService, never()).createReplicated(any(), any(), any());
    }
}
