package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.service.outbox.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReplicationDependencyUnitTest {
    private final RestTemplate metadata = mock(RestTemplate.class);
    private final RestTemplate data = mock(RestTemplate.class);
    private final RestTemplate external = mock(RestTemplate.class);
    private final ReplicationOutboxService outbox = mock(ReplicationOutboxService.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final ReplicationServiceImpl service = new ReplicationServiceImpl(metadata, data, external, mapper, outbox);
    private final UUID databaseId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();
    private final UUID remoteDatabaseId = UUID.randomUUID();
    private final UUID remoteTableId = UUID.randomUUID();

    ReplicationDependencyUnitTest() {
        ReflectionTestUtils.setField(service, "baseUrl", "https://origin.example");
    }

    @Test
    void unresolvedObjectsAreQueuedInsteadOfDropped() {
        final ReplicaLocation target = ReplicaLocation.builder().url("https://replica.example").build();
        service.replicateTable(TableNotificationDto.builder().databaseId(databaseId).creationId(tableId)
                .createTableDto(CreateTableDto.builder().build()).replicas(List.of(target)).build());
        service.replicateView(ViewNotificationDto.builder().databaseId(databaseId).creationId(UUID.randomUUID())
                .viewDto(ViewDto.builder().build()).replicas(List.of(target)).build());
        final Map<String, UUID> pending = new HashMap<>();
        pending.put("https://replica.example", null);
        service.replicateTableDelete(TableDeleteNotificationDto.builder().databaseId(databaseId).tableId(tableId)
                .databaseReplicaIds(pending).tableReplicaIds(pending).archivedAt(Instant.now()).build());
        final DataReplicationDto payload = payload(pending, Map.of());
        when(data.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
        service.replicateData(payload, HttpMethod.POST);

        for (ReplicationOutboxOperationType type : List.of(ReplicationOutboxOperationType.TABLE_CREATE,
                ReplicationOutboxOperationType.VIEW_CREATE, ReplicationOutboxOperationType.TABLE_DELETE,
                ReplicationOutboxOperationType.DATA_CREATE)) {
            verify(outbox).enqueue(eq(type), eq("https://replica.example"), any(), any(), eq(databaseId),
                    any(), isNull(), isNull(), contains("Waiting"));
        }
        verifyNoInteractions(external);
    }

    @Test
    void waitingForParentDoesNotExhaustRetries() throws Exception {
        final TableNotificationDto payload = TableNotificationDto.builder().databaseId(databaseId)
                .creationId(tableId).createTableDto(CreateTableDto.builder().build()).build();
        final ReplicationOutboxEntry entry = entry(ReplicationOutboxOperationType.TABLE_CREATE, payload);
        when(outbox.findById(entry.getId())).thenReturn(Optional.of(entry));
        metadata(DatabaseDto.builder().id(databaseId).replicaUrls(Map.of()).build(), null);

        assertFalse(service.retryOutboxEntry(entry.getId()));
        verify(outbox).defer(eq(entry.getId()), contains("database mapping"), any());
        verify(outbox, never()).markFailed(any(), any(), any(), anyInt());
        verifyNoInteractions(external);
    }

    @Test
    void retryResolvesCurrentMappingsAndDistributesRecoveredTimestampsToOtherPeer() throws Exception {
        final UUID thirdDatabaseId = UUID.randomUUID();
        final UUID thirdTableId = UUID.randomUUID();
        final Map<String, UUID> databases = Map.of("https://replica.example", remoteDatabaseId,
                "https://third.example", thirdDatabaseId);
        final Map<String, UUID> tables = Map.of("https://replica.example", remoteTableId,
                "https://third.example", thirdTableId);
        final DataReplicationDto payload = payload(Map.of(), Map.of());
        final ReplicationOutboxEntry entry = entry(ReplicationOutboxOperationType.DATA_CREATE, payload);
        entry.setHttpMethod("POST");
        when(outbox.findById(entry.getId())).thenReturn(Optional.of(entry));
        metadata(DatabaseDto.builder().id(databaseId).replicaUrls(databases).build(),
                TableDto.builder().id(tableId).replicaUrls(tables).build());
        when(external.exchange(endsWith("/data/replicate"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(TupleWithTimestampsDto.class))).thenReturn(ResponseEntity.ok(payload.getTuple()));
        when(data.exchange(endsWith("/timestamps"), any(HttpMethod.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
        when(external.exchange(endsWith("/timestamps"), any(HttpMethod.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        assertTrue(service.retryOutboxEntry(entry.getId()));
        verify(external).exchange(eq("https://replica.example/api/v1/database/" + remoteDatabaseId
                + "/table/" + remoteTableId + "/data/replicate"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(TupleWithTimestampsDto.class));
        verify(external).exchange(eq("https://third.example/api/v1/database/" + thirdDatabaseId
                + "/table/" + thirdTableId + "/timestamps"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Map.class));
        verify(outbox).markSucceeded(entry.getId());
    }

    @Test
    void staleCreateCannotLeaveAnArchivedSourceTableActive() throws Exception {
        final Instant archived = Instant.parse("2026-10-03T12:00:00.123456Z");
        final TableNotificationDto payload = TableNotificationDto.builder().databaseId(databaseId)
                .creationId(tableId).createTableDto(CreateTableDto.builder().build()).build();
        final ReplicationOutboxEntry entry = entry(ReplicationOutboxOperationType.TABLE_CREATE, payload);
        when(outbox.findById(entry.getId())).thenReturn(Optional.of(entry));
        metadata(DatabaseDto.builder().id(databaseId)
                        .replicaUrls(Map.of("https://replica.example", remoteDatabaseId)).build(),
                TableDto.builder().id(tableId).archivedAt(archived).replicaUrls(Map.of()).build());
        when(external.exchange(endsWith("/table/replicate"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(TableBriefDto.class))).thenReturn(ResponseEntity.ok(TableBriefDto.builder().id(remoteTableId).build()));

        assertTrue(service.retryOutboxEntry(entry.getId()));
        verify(external).exchange(eq("https://replica.example/api/v1/database/" + remoteDatabaseId + "/table/"
                + remoteTableId + "/replicate?archivedAt=" + archived), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY),
                eq(Void.class));
        verify(outbox).markSucceeded(entry.getId());
    }

    private DataReplicationDto payload(Map<String, UUID> databases, Map<String, UUID> tables) {
        return DataReplicationDto.builder()
                .database(DatabaseDto.builder().id(databaseId).replicaUrls(databases).build())
                .table(TableDto.builder().id(tableId).replicaUrls(tables).build())
                .tuple(TupleWithTimestampsDto.builder().replicationKey("key")
                        .insertedAt(Instant.parse("2026-10-03T10:00:00Z"))
                        .data(Map.of("replication_key", "key", "value", 1)).build()).build();
    }

    private ReplicationOutboxEntry entry(ReplicationOutboxOperationType type, Object payload) throws Exception {
        return ReplicationOutboxEntry.builder().id(UUID.randomUUID()).operationType(type)
                .targetSiteUrl("https://replica.example").localDatabaseId(databaseId).localTableId(tableId)
                .payloadJson(mapper.writeValueAsString(payload)).status(ReplicationOutboxStatus.PENDING).build();
    }

    private void metadata(DatabaseDto database, TableDto table) {
        when(metadata.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(DatabaseDto.class))).thenReturn(ResponseEntity.ok(database));
        if (table != null) {
            when(metadata.exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId),
                    eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class))).thenReturn(ResponseEntity.ok(table));
        }
    }
}
