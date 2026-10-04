package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.TableCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseBootstrapDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKeyReference;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ReferenceType;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatabaseReplicaServiceUnitTest {
    private static final String ORIGIN = "https://source.example", TARGET = "https://target.example";
    private final UserService users = mock(UserService.class);
    private final ReplicationNotificationOutboxService outbox = mock(ReplicationNotificationOutboxService.class);
    private final ReplicationNotificationDispatcher dispatcher = mock(ReplicationNotificationDispatcher.class);
    private final DatabaseCacheRepository cache = mock(DatabaseCacheRepository.class);
    private final TableCacheRepository tableCache = mock(TableCacheRepository.class);
    private final EntityManager entities = mock(EntityManager.class);
    private final DatabaseReplicaService service = new DatabaseReplicaService(Mappers.getMapper(MetadataMapper.class),
            new ReplicationPeers(ORIGIN + "," + TARGET), users, outbox, dispatcher, cache, tableCache);
    private final Database database = new Database();
    private final UUID notificationId = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        ReflectionTestUtils.setField(service, "baseUrl", ORIGIN);
        ReflectionTestUtils.setField(service, "issuer", ORIGIN + "/auth/realms/dbrepo");
        ReflectionTestUtils.setField(service, "entityManager", entities);
        database.setId(UUID.randomUUID()); database.setName("Existing database"); database.setOwnedBy("alice");
        database.setIsPublic(false); database.setIsSchemaPublic(false); database.setCreationLocation(null);
        database.setContainer(Container.builder().id(UUID.randomUUID()).image(new ContainerImage()).build());
        when(entities.find(Database.class, database.getId(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(database);
        when(users.findByUsername("alice")).thenReturn(UserDto.builder().id(UUID.randomUUID()).username("alice").build());
        when(outbox.enqueue(any(), any(), anyString(), any(), any()))
                .thenReturn(ReplicationNotificationOutbox.builder().id(notificationId).build());
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test
    void registrationPreparesMetadataOnceOrdersForeignKeysAndQueuesBootstrapAfterCommit() throws Exception {
        final Table parent = table("parent"), child = table("child");
        child.getConstraints().getForeignKeys().add(ForeignKey.builder().table(child).referencedTable(parent)
                .references(List.of(ForeignKeyReference.builder().column(child.getColumns().getFirst())
                        .referencedColumn(parent.getColumns().getFirst()).build()))
                .onDelete(ReferenceType.RESTRICT).onUpdate(ReferenceType.RESTRICT).build());
        database.getTables().addAll(List.of(child, parent));
        final var prepared = Set.of(parent.getId(), child.getId());
        service.register(database.getId(), "https://TARGET.example:443/", prepared);
        final var payload = org.mockito.ArgumentCaptor.forClass(DatabaseBootstrapDto.class);
        verify(outbox).enqueue(eq(ReplicationNotificationType.DATABASE_BOOTSTRAP), eq(HttpMethod.POST),
                eq("/api/replication/database/bootstrap"), payload.capture(), eq(database.getId()));
        assertEquals(List.of(parent.getId(), child.getId()), payload.getValue().tables().stream().map(t -> t.getCreationId()).toList());
        assertEquals(ORIGIN, database.getCreationLocation());
        assertEquals(TARGET, database.getReplicaUrls().getFirst().getUrl());
        assertEquals("alice", payload.getValue().database().getOwner().getUsername());
        for (var notification : payload.getValue().tables()) {
            final var definition = notification.getCreateTableDto();
            assertEquals("replication_key", definition.getColumns().getLast().getName());
            assertFalse(definition.getColumns().getLast().getNullAllowed());
            assertTrue(definition.getConstraints().getUniques().contains(List.of("replication_key")));
        }
        assertEquals(List.of("value"), payload.getValue().tables().getLast().getCreateTableDto()
                .getConstraints().getForeignKeys().getFirst().getReferencedColumns());
        verifyNoInteractions(dispatcher, cache, tableCache);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(dispatcher).dispatchAsync(notificationId);
        verify(cache).deleteById(database.getId());
        verify(tableCache).deleteById(parent.getId());
        service.register(database.getId(), TARGET, prepared);
        assertEquals(1, database.getReplicaUrls().size());
        assertEquals(2, parent.getColumns().size());
        verify(outbox, times(1)).enqueue(any(), any(), anyString(), any(), any());
    }

    @Test
    void changedInventoryAndCyclicDependenciesAreRejectedBeforeTopologyMutation() {
        final Table table = table("cycle");
        database.getTables().add(table);
        assertThrows(ResponseStatusException.class, () -> service.register(database.getId(), TARGET, Set.of()));
        final Table unavailable = table("unavailable");
        table.getConstraints().getForeignKeys().add(ForeignKey.builder().table(table).referencedTable(unavailable).build());
        assertThrows(ResponseStatusException.class, () -> service.register(database.getId(), TARGET, Set.of(table.getId())));
        assertTrue(database.getReplicaUrls().isEmpty());
        assertEquals(1, table.getColumns().size());
        verifyNoInteractions(outbox, dispatcher);
    }

    @Test
    void onlyPrimaryAndTrustedNonlocalSitesCanRequestActivation() {
        for (String site : List.of(ORIGIN, "https://untrusted.example")) {
            assertThrows(ResponseStatusException.class, () -> service.request(database, site));
        }
        database.setCreationLocation(TARGET);
        assertThrows(ResponseStatusException.class, () -> service.request(database, TARGET));
        verifyNoInteractions(outbox, dispatcher);
    }

    @Test
    void activationNotificationIsDispatchedOnlyAfterCommit() {
        service.request(database, TARGET);
        verify(outbox).enqueue(eq(ReplicationNotificationType.DATABASE_PREPARE), eq(HttpMethod.POST),
                eq("/api/replication/database/" + database.getId() + "/prepare"), any(), eq(database.getId()));
        verifyNoInteractions(dispatcher);
        // A rollback does not invoke afterCommit; the notification stays in the originating transaction.
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(dispatcher).dispatchAsync(notificationId);
    }

    private Table table(String name) {
        final Table table = new Table();
        table.setId(UUID.randomUUID()); table.setName(name); table.setInternalName(name); table.setDatabase(database);
        table.setTdbid(database.getId()); table.setOwnedBy("alice"); table.setConstraints(new Constraints());
        table.getColumns().add(TableColumn.builder().id(UUID.randomUUID()).table(table).name("value").internalName("value")
                .columnType(TableColumnType.INT).isNullAllowed(false).ordinalPosition(0).build());
        return table;
    }
}
