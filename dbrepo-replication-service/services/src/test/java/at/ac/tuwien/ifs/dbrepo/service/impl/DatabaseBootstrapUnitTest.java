package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.*;
import at.ac.tuwien.ifs.dbrepo.service.outbox.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatabaseBootstrapUnitTest {
    private static final String ORIGIN = "https://source.example", TARGET = "https://target.example";
    @TempDir Path directory;
    private final RestTemplate metadata = mock(RestTemplate.class), data = mock(RestTemplate.class), external = mock(RestTemplate.class);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final UUID databaseId = UUID.randomUUID(), tableId = UUID.randomUUID(), viewId = UUID.randomUUID();

    @Test
    void bootstrapPersistsOrderedJobsOnlyOnceAndResumesAfterPreparationFailureAndRestart() throws Exception {
        final String path = directory.resolve("outbox.json").toString();
        final var request = new DatabaseBootstrapDto(DatabaseBootstrapDto.idFor(databaseId, TARGET), TARGET,
                DatabaseNotificationDto.builder().creationId(databaseId).createDatabaseDto(CreateDatabaseDto.builder().build()).build(),
                List.of(TableNotificationDto.builder().databaseId(databaseId).creationId(tableId)
                        .createTableDto(CreateTableDto.builder().build()).build()),
                List.of(ViewNotificationDto.builder().databaseId(databaseId).creationId(viewId)
                        .viewDto(ViewDto.builder().build()).build()));
        final List<UUID> ids;
        final UUID preparation;
        when(metadata.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok(DatabaseDto.builder().id(databaseId).creationLocation(ORIGIN).build()));
        try (var outbox = new FileReplicationOutboxService(json, path)) {
            final var service = service(outbox);
            preparation = service.prepareDatabase(databaseId, TARGET);
            ids = service.bootstrapDatabase(request);
            assertEquals(ids, service.bootstrapDatabase(request));
            assertEquals(preparation, service.prepareDatabase(databaseId, TARGET));
            assertEquals(6, outbox.findAll().size());
            assertEquals(List.of(preparation), outbox.findById(ids.getFirst()).orElseThrow().getDependencies());
            for (int i = 1; i < ids.size(); i++) {
                assertEquals(List.of(ids.get(i - 1)), outbox.findById(ids.get(i)).orElseThrow().getDependencies());
            }
            final var jobs = ids.stream().map(id -> outbox.findById(id).orElseThrow().getOperationType()).toList();
            assertEquals(List.of(ReplicationOutboxOperationType.DATABASE_CREATE, ReplicationOutboxOperationType.TABLE_CREATE,
                    ReplicationOutboxOperationType.HISTORY_SYNC, ReplicationOutboxOperationType.VIEW_CREATE,
                    ReplicationOutboxOperationType.SUBSET_BACKFILL), jobs);
            when(data.postForEntity(contains("/replication/activate"), isNull(), eq(Void.class)))
                    .thenThrow(new ResourceAccessException("source temporarily unavailable"));
            assertFalse(service.retryOutboxEntry(preparation));
            assertFalse(service.retryOutboxEntry(ids.getFirst()));
            assertNotNull(outbox.findById(preparation).orElseThrow().getNextAttemptAt());
            verifyNoInteractions(external);
        }
        try (var outbox = new FileReplicationOutboxService(json, path)) {
            final var service = service(outbox);
            assertEquals(List.of(preparation), outbox.findById(ids.getFirst()).orElseThrow().getDependencies());
            when(data.postForEntity(contains("/replication/activate"), isNull(), eq(Void.class)))
                    .thenReturn(ResponseEntity.noContent().build());
            assertTrue(service.retryOutboxEntry(preparation));
            assertEquals(ReplicationOutboxStatus.SUCCEEDED, outbox.findById(preparation).orElseThrow().getStatus());
            assertEquals(ids, service.bootstrapDatabase(request));
            assertEquals(6, outbox.findAll().size());
            outbox.cancel(ids.getFirst(), "Target retired", "administrator");
            assertFalse(service.retryOutboxEntry(ids.get(1)));
            assertTrue(outbox.findById(ids.get(1)).orElseThrow().getLastError().contains("cancelled"));
            verifyNoInteractions(external);
        }
    }

    @Test
    void untrustedSelfAndSecondaryTargetsAreRejectedBeforeWritingJobs() throws Exception {
        try (var outbox = new FileReplicationOutboxService(json, directory.resolve("outbox.json").toString())) {
            final var service = service(outbox);
            assertThrows(IllegalArgumentException.class, () -> service.prepareDatabase(databaseId, ORIGIN));
            assertThrows(IllegalArgumentException.class, () -> service.prepareDatabase(databaseId, "https://evil.example"));
            when(metadata.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                    .thenReturn(ResponseEntity.ok(DatabaseDto.builder().id(databaseId).creationLocation(TARGET).build()));
            assertThrows(IllegalArgumentException.class, () -> service.prepareDatabase(databaseId, TARGET));
            assertTrue(outbox.findAll().isEmpty());
        }
    }

    private ReplicationServiceImpl service(ReplicationOutboxService outbox) {
        final var service = new ReplicationServiceImpl(metadata, data, external, json, outbox);
        ReflectionTestUtils.setField(service, "baseUrl", ORIGIN);
        ReflectionTestUtils.setField(service, "allowedSites", ORIGIN + "," + TARGET);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        return service;
    }

    @Test
    void immediateDeliveryUsesNewTopologyEvenWhenCommittedEventContainsOlderRoutes() {
        final var outbox = mock(ReplicationOutboxService.class);
        final var service = service(outbox);
        final UUID remoteDatabase = UUID.randomUUID(), remoteTable = UUID.randomUUID();
        when(metadata.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok(DatabaseDto.builder().id(databaseId).replicaUrls(Map.of(TARGET, remoteDatabase)).build()));
        when(metadata.exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.ok(TableDto.builder().id(tableId).replicaUrls(Map.of(TARGET, remoteTable)).build()));
        final var tuple = TupleWithTimestampsDto.builder().replicationKey("identity")
                .insertedAt(java.time.Instant.parse("2026-10-04T12:00:00Z")).data(Map.of("value", 5)).build();
        final var request = DataReplicationDto.builder().tuple(tuple)
                .database(DatabaseDto.builder().id(databaseId).replicaUrls(Map.of()).build())
                .table(TableDto.builder().id(tableId).replicaUrls(Map.of()).build()).build();
        when(external.exchange(endsWith("/data/replicate"), eq(HttpMethod.POST), any(HttpEntity.class), eq(TupleWithTimestampsDto.class)))
                .thenReturn(ResponseEntity.ok(tuple));
        when(data.exchange(endsWith("/timestamps"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
        when(external.exchange(endsWith("/timestamps"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
        assertEquals(1, service.replicateData(request, HttpMethod.POST));
        verify(external).exchange(eq(TARGET + "/api/v1/database/" + remoteDatabase + "/table/" + remoteTable + "/data/replicate"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(TupleWithTimestampsDto.class));
    }
}
