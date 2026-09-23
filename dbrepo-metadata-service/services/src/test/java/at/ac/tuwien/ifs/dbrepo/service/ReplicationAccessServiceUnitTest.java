package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationAccessStatus;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationAccessServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplicationAccessServiceUnitTest {

    @Mock
    private DatabaseService databaseService;

    @Mock
    private AccessService accessService;

    @Mock
    private UserService userService;

    @Mock
    private ReplicationIdentityService identityService;

    private ReplicationAccessServiceImpl service;

    @BeforeEach
    public void beforeEach() {
        service = new ReplicationAccessServiceImpl(databaseService, accessService, userService, identityService);
        ReflectionTestUtils.setField(service, "baseUrl", "https://target.example/");
        ReflectionTestUtils.setField(service, "replicationUsername", "replication");
    }

    @Test
    public void initialize_unmappedIdentity_keepsReplicaPending() throws Exception {
        final Database database = targetReplica();
        final ReplicationOwnerDto owner = owner();
        when(databaseService.modifyReplicationAccess(database, owner, ReplicationAccessStatus.PENDING, null))
                .thenReturn(database);
        when(identityService.resolve(owner)).thenReturn(Optional.empty());

        final Database result = service.initialize(database, owner);

        assertEquals(database, result);
        verify(accessService, never()).create(database, "alice", AccessTypeDto.WRITE_ALL);
        verify(databaseService, never()).modifyReplicationAccess(
                database, owner, ReplicationAccessStatus.MAPPED, "alice");
    }

    @Test
    public void initialize_resolvedIdentity_grantsWriteAccessAndTransfersLogicalOwnership() throws Exception {
        final Database database = targetReplica();
        final ReplicationOwnerDto owner = owner();
        final UserDto localUser = UserDto.builder().id(UUID.randomUUID()).username("alice").build();
        when(databaseService.modifyReplicationAccess(database, owner, ReplicationAccessStatus.PENDING, null))
                .thenReturn(database);
        when(identityService.resolve(owner)).thenReturn(Optional.of(localUser));
        when(databaseService.modifyReplicationAccess(database, owner, ReplicationAccessStatus.MAPPED, "alice"))
                .thenReturn(database);

        service.initialize(database, owner);

        verify(accessService).create(database, "alice", AccessTypeDto.WRITE_ALL);
        verify(databaseService).modifyReplicationAccess(database, owner, ReplicationAccessStatus.MAPPED, "alice");
    }

    @Test
    public void initialize_accessGrantFails_keepsReplicaPendingForAdminRetry() throws Exception {
        final Database database = targetReplica();
        final ReplicationOwnerDto owner = owner();
        final UserDto localUser = UserDto.builder().id(UUID.randomUUID()).username("alice").build();
        when(databaseService.modifyReplicationAccess(database, owner, ReplicationAccessStatus.PENDING, null))
                .thenReturn(database);
        when(identityService.resolve(owner)).thenReturn(Optional.of(localUser));
        org.mockito.Mockito.doThrow(new DataServiceException("unavailable"))
                .when(accessService).create(database, "alice", AccessTypeDto.WRITE_ALL);

        final Database result = service.initialize(database, owner);

        assertEquals(database, result);
        verify(databaseService, never()).modifyReplicationAccess(
                database, owner, ReplicationAccessStatus.MAPPED, "alice");
    }

    @Test
    public void map_legacyReplica_allowsOneTimeLocalAssignmentWithoutIdentityMapping() throws Exception {
        final Database database = targetReplica();
        final UserDto localUser = UserDto.builder().id(UUID.randomUUID()).username("alice").build();
        when(userService.findByUsername("alice")).thenReturn(localUser);
        when(databaseService.modifyReplicationAccess(database, null, ReplicationAccessStatus.MAPPED, "alice"))
                .thenAnswer(invocation -> {
                    database.setReplicationAccessStatus(ReplicationAccessStatus.MAPPED);
                    database.setReplicationLocalUsername("alice");
                    return database;
                });

        final ReplicationAccessDto result = service.map(database, "alice");

        assertEquals(ReplicationAccessStatus.MAPPED, result.getStatus());
        assertEquals("alice", result.getLocalUsername());
        assertNull(result.getOriginOwner());
        verify(identityService, never()).map(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void map_replicationUser_fails() throws Exception {
        final Database database = targetReplica();
        when(userService.findByUsername("replication"))
                .thenReturn(UserDto.builder().id(UUID.randomUUID()).username("replication").build());

        assertThrows(NotAllowedException.class, () -> service.map(database, "replication"));

        verify(accessService, never()).create(
                database, "replication", AccessTypeDto.WRITE_ALL);
    }

    @Test
    public void map_mappedReplica_transfersAccessToNewLocalOwner() throws Exception {
        final Database database = targetReplica();
        database.setReplicationAccessStatus(ReplicationAccessStatus.MAPPED);
        database.setReplicationLocalUsername("alice");
        database.getAccesses().add(DatabaseAccess.builder()
                .database(database)
                .hdbid(database.getId())
                .username("alice")
                .build());
        final UserDto localUser = UserDto.builder().id(UUID.randomUUID()).username("bob").build();
        when(userService.findByUsername("bob")).thenReturn(localUser);
        when(databaseService.modifyReplicationAccess(database, null, ReplicationAccessStatus.MAPPED, "bob"))
                .thenAnswer(invocation -> {
                    database.setReplicationLocalUsername("bob");
                    return database;
                });

        final ReplicationAccessDto result = service.map(database, "bob");

        assertEquals("bob", result.getLocalUsername());
        verify(accessService).create(database, "bob", AccessTypeDto.WRITE_ALL);
        verify(accessService).delete(database, "alice");
    }

    @Test
    public void findAll_includesPendingAndMappedTargetReplicas() {
        final Database pendingTarget = targetReplica();
        final Database mappedTarget = targetReplica();
        mappedTarget.setId(UUID.randomUUID());
        mappedTarget.setReplicationAccessStatus(ReplicationAccessStatus.MAPPED);
        mappedTarget.setReplicationLocalUsername("alice");
        final Database local = targetReplica();
        local.setCreationLocation("https://target.example");
        when(databaseService.findAll()).thenReturn(List.of(local, mappedTarget, pendingTarget));

        final List<ReplicationAccessDto> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("alice", result.getFirst().getLocalUsername());
        assertEquals(ReplicationAccessStatus.PENDING, result.get(1).getStatus());
    }

    @Test
    public void findPending_includesLegacyTargetAndExcludesLocalAndMappedReplicas() {
        final Database legacyTarget = targetReplica();
        final Database local = targetReplica();
        local.setCreationLocation("https://target.example");
        final Database mappedTarget = targetReplica();
        mappedTarget.setId(UUID.randomUUID());
        mappedTarget.setReplicationAccessStatus(ReplicationAccessStatus.MAPPED);
        when(databaseService.findAll()).thenReturn(List.of(local, mappedTarget, legacyTarget));

        final List<ReplicationAccessDto> result = service.findPending();

        assertEquals(1, result.size());
        assertEquals(legacyTarget.getId(), result.getFirst().getDatabaseId());
        assertEquals(ReplicationAccessStatus.PENDING, result.getFirst().getStatus());
    }

    private Database targetReplica() {
        return Database.builder()
                .id(UUID.randomUUID())
                .name("replicated database")
                .creationLocation("https://origin.example")
                .accesses(new LinkedList<DatabaseAccess>())
                .build();
    }

    private ReplicationOwnerDto owner() {
        return ReplicationOwnerDto.builder()
                .siteUrl("https://origin.example")
                .issuer("https://identity.example/realms/dbrepo")
                .subject(UUID.randomUUID().toString())
                .username("remote-alice")
                .build();
    }
}
