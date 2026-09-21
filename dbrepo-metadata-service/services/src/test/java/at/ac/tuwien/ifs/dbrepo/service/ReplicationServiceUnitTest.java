package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
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
}
