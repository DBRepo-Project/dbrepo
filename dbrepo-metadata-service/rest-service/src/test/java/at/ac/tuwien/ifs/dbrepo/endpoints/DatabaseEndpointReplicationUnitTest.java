package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.LocalDatabaseIdDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.ContainerService;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationAccessService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseEndpointReplicationUnitTest extends BaseTest {

    @MockitoBean
    private ContainerService containerService;

    @MockitoBean
    private DatabaseServiceImpl databaseService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private ReplicationService replicationService;

    @MockitoBean
    private ReplicationAccessService replicationAccessService;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void replicate_existingReplica_returnsExistingDatabase() throws DataServiceConnectionException,
            ContainerQuotaException, MalformedException, SearchServiceConnectionException, ContainerNotFoundException,
            DashboardServiceException, DataServiceException, SearchServiceException, DatabaseNotFoundException,
            DashboardServiceConnectionException {
        final UUID creationId = UUID.randomUUID();
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1.getName())
                .isPublic(DATABASE_1.getIsPublic())
                .isSchemaPublic(DATABASE_1.getIsSchemaPublic())
                .replicaUrls(List.of("http://local.test"))
                .creationLocation("http://local.test")
                .build();
        final DatabaseNotificationDto notification = DatabaseNotificationDto.builder()
                .creationId(creationId)
                .createDatabaseDto(request)
                .build();

        when(databaseService.findLocalDatabaseIdByReplicaDatabaseId(creationId))
                .thenReturn(new LocalDatabaseIdDto(DATABASE_1_ID, creationId));
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);

        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.replicate(notification);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(DATABASE_1_ID, response.getBody().getId());
        verify(containerService, never()).find(any(UUID.class));
        verify(databaseService, never()).create(any(), any(), any(), eq(creationId));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void replicate_missingCreationId_fails() throws DataServiceConnectionException, ContainerQuotaException,
            SearchServiceConnectionException, ContainerNotFoundException, DashboardServiceException,
            DataServiceException, SearchServiceException, DatabaseNotFoundException,
            DashboardServiceConnectionException {
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1.getName())
                .isPublic(DATABASE_1.getIsPublic())
                .isSchemaPublic(DATABASE_1.getIsSchemaPublic())
                .build();
        final DatabaseNotificationDto notification = DatabaseNotificationDto.builder()
                .createDatabaseDto(request)
                .build();

        assertThrows(MalformedException.class, () -> {
            databaseEndpoint.replicate(notification);
        });
        verify(containerService, never()).find(any(UUID.class));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"replication"})
    public void replicate_newReplica_usesDedicatedReplicationUserAndInitializesAccess() throws Exception {
        final UUID creationId = UUID.randomUUID();
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1.getName())
                .isPublic(false)
                .isSchemaPublic(false)
                .creationLocation("https://origin.example")
                .replicaUrls(List.of("http://localhost"))
                .build();
        final ReplicationOwnerDto owner = ReplicationOwnerDto.builder()
                .siteUrl("https://origin.example")
                .issuer("https://identity.example/realms/dbrepo")
                .subject(USER_1_ID.toString())
                .username(USER_1_USERNAME)
                .build();
        final DatabaseNotificationDto notification = DatabaseNotificationDto.builder()
                .creationId(creationId)
                .createDatabaseDto(request)
                .owner(owner)
                .build();

        doThrow(new DatabaseNotFoundException("not found"))
                .when(databaseService).findLocalDatabaseIdByReplicaDatabaseId(creationId);
        when(containerService.find(CONTAINER_1_ID)).thenReturn(CONTAINER_1);
        when(databaseService.create(eq(CONTAINER_1), eq(request), any(UserDto.class), eq(creationId)))
                .thenReturn(DATABASE_1);
        when(dashboardService.create(DATABASE_1)).thenReturn(CreateDashboardResponseDto.builder()
                .uid(DATABASE_1_DASHBOARD_UID)
                .build());
        when(replicationAccessService.initialize(DATABASE_1, owner)).thenReturn(DATABASE_1);

        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.replicate(notification);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final ArgumentCaptor<UserDto> userCaptor = ArgumentCaptor.forClass(UserDto.class);
        verify(databaseService).create(eq(CONTAINER_1), eq(request), userCaptor.capture(), eq(creationId));
        assertEquals("replication", userCaptor.getValue().getUsername());
        assertNotEquals("admin", userCaptor.getValue().getUsername());
        assertEquals("replication", userCaptor.getValue().getAttributes().getMariadbPassword());
        verify(replicationAccessService).initialize(DATABASE_1, owner);
    }
}
