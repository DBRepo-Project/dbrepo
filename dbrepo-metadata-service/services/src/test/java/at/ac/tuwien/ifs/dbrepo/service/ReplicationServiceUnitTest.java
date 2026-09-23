package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableDeleteNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaTableLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplicationServiceUnitTest {

    @Mock
    private MetadataMapper metadataMapper;

    @Mock
    private ReplicationNotificationOutboxService outboxService;

    @Mock
    private ReplicationNotificationDispatcher dispatcher;

    private ReplicationServiceImpl service;

    @BeforeEach
    public void beforeEach() {
        service = new ReplicationServiceImpl(metadataMapper, outboxService, dispatcher);
        ReflectionTestUtils.setField(service, "baseUrl", "https://origin.example");
        ReflectionTestUtils.setField(service, "issuer", "https://identity.example/realms/dbrepo");
    }

    @Test
    public void replicateDatabase_enqueuesStableOriginOwnerIdentity() {
        final UUID databaseId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final CreateDatabaseDto request = CreateDatabaseDto.builder().name("replicated database").build();
        final UserDto owner = UserDto.builder().id(userId).username("alice").build();
        final ReplicationNotificationOutbox entry = ReplicationNotificationOutbox.builder()
                .id(UUID.randomUUID())
                .build();
        when(outboxService.enqueue(eq(ReplicationNotificationType.DATABASE_CREATE), eq(HttpMethod.POST),
                eq("/api/replication/database"), any(), eq(databaseId))).thenReturn(entry);

        service.replicateDatabase(request, databaseId, owner);

        final ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).enqueue(eq(ReplicationNotificationType.DATABASE_CREATE), eq(HttpMethod.POST),
                eq("/api/replication/database"), payloadCaptor.capture(), eq(databaseId));
        final DatabaseNotificationDto notification = (DatabaseNotificationDto) payloadCaptor.getValue();
        assertEquals(databaseId, notification.getCreationId());
        assertEquals("https://origin.example", notification.getOwner().getSiteUrl());
        assertEquals("https://identity.example/realms/dbrepo", notification.getOwner().getIssuer());
        assertEquals(userId.toString(), notification.getOwner().getSubject());
        assertEquals("alice", notification.getOwner().getUsername());
        assertEquals("https://origin.example", request.getCreationLocation());
        verify(dispatcher).dispatchAsync(entry.getId());
    }

    @Test
    public void replicateTableDelete_enqueuesResolvedReplicaIds() {
        final UUID databaseId = UUID.randomUUID();
        final UUID tableId = UUID.randomUUID();
        final UUID remoteDatabaseId = UUID.randomUUID();
        final UUID remoteTableId = UUID.randomUUID();
        final Database database = Database.builder()
                .id(databaseId)
                .replicaUrls(List.of(ReplicaLocation.builder()
                        .url("https://replica.example")
                        .replicaDatabaseId(remoteDatabaseId)
                        .build()))
                .build();
        final Table table = Table.builder()
                .id(tableId)
                .replicaUrls(List.of(ReplicaTableLocation.builder()
                        .url("https://replica.example")
                        .replicaTableId(remoteTableId)
                        .build()))
                .build();
        final ReplicationNotificationOutbox entry = ReplicationNotificationOutbox.builder()
                .id(UUID.randomUUID())
                .build();
        when(outboxService.enqueue(eq(ReplicationNotificationType.TABLE_DELETE), eq(HttpMethod.DELETE),
                eq("/api/replication/table"), any(), eq(tableId))).thenReturn(entry);

        service.replicateTableDelete(database, table);

        final ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).enqueue(eq(ReplicationNotificationType.TABLE_DELETE), eq(HttpMethod.DELETE),
                eq("/api/replication/table"), payloadCaptor.capture(), eq(tableId));
        final TableDeleteNotificationDto notification = (TableDeleteNotificationDto) payloadCaptor.getValue();
        assertEquals(databaseId, notification.getDatabaseId());
        assertEquals(tableId, notification.getTableId());
        assertEquals(remoteDatabaseId, notification.getDatabaseReplicaIds().get("https://replica.example"));
        assertEquals(remoteTableId, notification.getTableReplicaIds().get("https://replica.example"));
        verify(dispatcher).dispatchAsync(entry.getId());
    }
}
