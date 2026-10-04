package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.ReplicationSynchronisationDataDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableDeleteNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.service.DataSynchronisationResult;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseSynchronisationResult;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxOperationType;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplicationServiceImplUnitTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"online", "offline", "unmapped"})
    void partialDeliveryStillDistributesOtherPeersTimestampEvidence(String targetState) {
        final RestTemplate metadata = mock(RestTemplate.class);
        final RestTemplate external = mock(RestTemplate.class);
        final ReplicationOutboxService outbox = mock(ReplicationOutboxService.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(metadata,
                mock(RestTemplate.class), external, new ObjectMapper(), outbox);
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID(), tableId = UUID.randomUUID();
        final UUID databaseB = UUID.randomUUID(), tableB = UUID.randomUUID(), tableC = UUID.randomUUID();
        final UUID databaseC = targetState.equals("unmapped") ? null : UUID.randomUUID();
        final Map<String, UUID> databases = new java.util.HashMap<>(Map.of("http://b.test", databaseB));
        if (databaseC != null) databases.put("http://c.test", databaseC);
        final String key = UUID.randomUUID().toString();
        final var tuple = TupleWithTimestampsDto.builder().replicationKey(key)
                .insertedAt(Instant.parse("2026-10-03T10:00:00Z")).build();
        final var request = DataReplicationDto.builder()
                .database(DatabaseDto.builder().id(databaseId).replicaUrls(databases).build())
                .table(TableDto.builder().id(tableId).replicaUrls(Map.of("http://b.test", tableB, "http://c.test", tableC)).build())
                .tuple(tuple).build();
        when(metadata.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok(request.getDatabase()));
        when(metadata.exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.ok(request.getTable()));
        when(external.exchange(eq("http://b.test/api/v1/database/" + databaseB + "/table/" + tableB + "/data/replicate"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(TupleWithTimestampsDto.class))).thenReturn(ResponseEntity.ok(tuple));
        when(external.exchange(eq("http://c.test/api/v1/database/" + databaseC + "/table/" + tableC + "/data/replicate"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(TupleWithTimestampsDto.class)))
                .thenThrow(new ResourceAccessException("tuple delivery unavailable"));
        final String timestamps = "http://c.test/api/v1/database/" + databaseC + "/table/" + tableC + "/timestamps";
        if (targetState.equals("offline")) {
            when(external.exchange(eq(timestamps), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("site unavailable"));
        }
        assertEquals(1, service.replicateData(request, HttpMethod.POST));
        final var body = org.mockito.ArgumentCaptor.forClass(List.class);
        if (targetState.equals("online")) {
            final var entity = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
            verify(external).exchange(eq(timestamps), eq(HttpMethod.POST), entity.capture(), eq(Map.class));
            assertEquals(2, ((List<?>) entity.getValue().getBody()).size());
            org.junit.jupiter.api.Assertions.assertTrue(((List<?>) entity.getValue().getBody()).stream()
                    .map(value -> (TupleReplicationTimestampDto) value).anyMatch(value -> value.getSiteUrl().equals("http://b.test")));
        } else {
            verify(outbox).enqueue(eq(ReplicationOutboxOperationType.TIMESTAMP_SYNC), eq("http://c.test"), eq(HttpMethod.POST),
                    body.capture(), eq(databaseId), eq(tableId), eq(databaseC), eq(tableC), any());
            assertEquals(2, body.getValue().size());
        }
    }

    @Test
    public void failedDurableHandoffIsNotAcknowledged() {
        final RestTemplate external = mock(RestTemplate.class);
        final ReplicationOutboxService outbox = mock(ReplicationOutboxService.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(mock(RestTemplate.class),
                mock(RestTemplate.class), external, new ObjectMapper(), outbox);
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        when(external.exchange(any(String.class), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("peer offline"));
        when(outbox.enqueue(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("outbox unavailable"));
        assertThrows(IllegalStateException.class, () -> service.replicateTableDelete(
                TableDeleteNotificationDto.builder().databaseId(UUID.randomUUID()).tableId(UUID.randomUUID())
                        .databaseReplicaIds(Map.of("http://remote.test", UUID.randomUUID()))
                        .tableReplicaIds(Map.of("http://remote.test", UUID.randomUUID())).build()));
    }

    @Test
    public void retryTableDelete_preservesSourceArchiveTimestamp() throws Exception {
        final RestTemplate external = mock(RestTemplate.class);
        final ReplicationOutboxService outbox = mock(ReplicationOutboxService.class);
        final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        final ReplicationServiceImpl service = new ReplicationServiceImpl(mock(RestTemplate.class),
                mock(RestTemplate.class), external, mapper, outbox);
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final Instant archived = Instant.parse("2026-10-03T10:00:00.123456Z");
        final ReplicationOutboxEntry entry = ReplicationOutboxEntry.builder().id(UUID.randomUUID())
                .operationType(ReplicationOutboxOperationType.TABLE_DELETE).targetSiteUrl("http://remote.test")
                .remoteDatabaseId(UUID.randomUUID()).remoteTableId(UUID.randomUUID())
                .payloadJson(mapper.writeValueAsString(TableDeleteNotificationDto.builder().archivedAt(archived).build()))
                .build();
        when(outbox.findById(entry.getId())).thenReturn(java.util.Optional.of(entry));

        assertEquals(true, service.retryOutboxEntry(entry.getId()));

        verify(external).exchange(eq("http://remote.test/api/v1/database/" + entry.getRemoteDatabaseId()
                + "/table/" + entry.getRemoteTableId() + "/replicate?archivedAt=" + archived),
                eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class));
        verify(outbox).markSucceeded(entry.getId());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    public void synchronisationQueuesDurableHistoryJobsWithoutBlockingOnPeers(boolean wholeDatabase) {
        final RestTemplate metadata = mock(RestTemplate.class);
        final RestTemplate data = mock(RestTemplate.class);
        final RestTemplate remote = mock(RestTemplate.class);
        final ReplicationOutboxService outbox = mock(ReplicationOutboxService.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(metadata, data, remote,
                new ObjectMapper().findAndRegisterModules(), outbox);
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID();
        final UUID tableId = UUID.randomUUID();
        final UUID remoteDatabaseId = UUID.randomUUID();
        final UUID remoteTableId = UUID.randomUUID();
        final UUID jobId = UUID.randomUUID();
        final TableDto table = TableDto.builder().id(tableId)
                .replicaUrls(Map.of("http://remote.test", remoteTableId)).build();
        final DatabaseDto database = DatabaseDto.builder().id(databaseId).creationLocation("http://local.test")
                .replicaUrls(Map.of("http://remote.test", remoteDatabaseId))
                .tables(List.of(table, TableDto.builder().id(UUID.randomUUID()).build())).build();
        when(metadata.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY), eq(DatabaseDto.class))).thenReturn(ResponseEntity.ok(database));
        when(metadata.exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId), eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY), eq(TableDto.class))).thenReturn(ResponseEntity.ok(table));
        when(outbox.enqueue(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ReplicationOutboxEntry.builder().id(jobId).build());

        if (wholeDatabase) {
            final DatabaseSynchronisationResult result = service.synchroniseDatabase(databaseId, 100);
            assertEquals(1, result.tables());
            assertEquals(List.of(jobId), result.jobs());
        } else {
            assertEquals(List.of(jobId), service.synchroniseData(databaseId, tableId, 100).jobs());
        }

        final var request = org.mockito.ArgumentCaptor.forClass(ReplicationServiceImpl.HistorySyncRequest.class);
        verify(outbox).enqueue(eq(ReplicationOutboxOperationType.HISTORY_SYNC), eq("http://remote.test"),
                eq(HttpMethod.POST), request.capture(), eq(databaseId), eq(tableId), eq(remoteDatabaseId),
                eq(remoteTableId), org.mockito.ArgumentMatchers.isNull());
        assertEquals(100, request.getValue().pageSize());
        org.junit.jupiter.api.Assertions.assertNotNull(request.getValue().snapshotId());
        org.mockito.Mockito.verifyNoInteractions(data, remote);
    }

    @Test
    public void synchroniseDatabase_invalidPageSize_fails() {
        final ReplicationServiceImpl service = new ReplicationServiceImpl(mock(RestTemplate.class),
                mock(RestTemplate.class), mock(RestTemplate.class), new ObjectMapper().findAndRegisterModules(),
                mock(ReplicationOutboxService.class));

        assertThrows(IllegalArgumentException.class, () -> {
            service.synchroniseDatabase(UUID.randomUUID(), 0);
        });
    }

    @Test
    public void synchroniseData_invalidPageSize_fails() {
        final ReplicationServiceImpl service = new ReplicationServiceImpl(mock(RestTemplate.class),
                mock(RestTemplate.class), mock(RestTemplate.class), new ObjectMapper().findAndRegisterModules(),
                mock(ReplicationOutboxService.class));

        assertThrows(IllegalArgumentException.class, () -> {
            service.synchroniseData(UUID.randomUUID(), UUID.randomUUID(), 0);
        });
    }

    @Test
    public void synchroniseDatabase_secondaryReplica_fails() {
        final RestTemplate metadataRestTemplate = mock(RestTemplate.class);
        final RestTemplate dataRestTemplate = mock(RestTemplate.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(metadataRestTemplate, dataRestTemplate,
                mock(RestTemplate.class), new ObjectMapper().findAndRegisterModules(),
                mock(ReplicationOutboxService.class));
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID();
        when(metadataRestTemplate.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok(DatabaseDto.builder()
                        .id(databaseId)
                        .creationLocation("http://primary.test")
                        .build()));

        assertThrows(IllegalArgumentException.class, () -> service.synchroniseDatabase(databaseId, 100));
        verify(dataRestTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class),
                eq(ReplicationSynchronisationDataDto.class));
    }

    @Test
    public void synchroniseData_secondaryReplica_fails() {
        final RestTemplate metadataRestTemplate = mock(RestTemplate.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(metadataRestTemplate,
                mock(RestTemplate.class), mock(RestTemplate.class), new ObjectMapper().findAndRegisterModules(),
                mock(ReplicationOutboxService.class));
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID();
        final UUID tableId = UUID.randomUUID();
        when(metadataRestTemplate.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok(DatabaseDto.builder()
                        .id(databaseId)
                        .creationLocation("http://primary.test")
                        .build()));

        assertThrows(IllegalArgumentException.class, () -> service.synchroniseData(databaseId, tableId, 100));
        verify(metadataRestTemplate, never()).exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId),
                eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class));
    }

    @Test
    public void replicateTableDelete_usesResolvedRemoteIds() {
        final RestTemplate externalRestTemplate = mock(RestTemplate.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(mock(RestTemplate.class),
                mock(RestTemplate.class), externalRestTemplate, new ObjectMapper().findAndRegisterModules(),
                mock(ReplicationOutboxService.class));
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID();
        final UUID tableId = UUID.randomUUID();
        final UUID remoteDatabaseId = UUID.randomUUID();
        final UUID remoteTableId = UUID.randomUUID();
        final TableDeleteNotificationDto notification = TableDeleteNotificationDto.builder()
                .databaseId(databaseId)
                .tableId(tableId)
                .archivedAt(java.time.Instant.parse("2026-10-03T10:00:00.123456Z"))
                .databaseReplicaIds(Map.of("http://remote.test/", remoteDatabaseId))
                .tableReplicaIds(Map.of("http://remote.test", remoteTableId))
                .build();

        final int replicated = service.replicateTableDelete(notification);

        assertEquals(1, replicated);
        verify(externalRestTemplate).exchange(eq("http://remote.test/api/v1/database/" + remoteDatabaseId
                        + "/table/" + remoteTableId + "/replicate?archivedAt=2026-10-03T10:00:00.123456Z"), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY),
                eq(Void.class));
    }

    @Test
    public void replicateTableDelete_unavailableTargetEnqueuesRetry() {
        final RestTemplate externalRestTemplate = mock(RestTemplate.class);
        final ReplicationOutboxService outboxService = mock(ReplicationOutboxService.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(mock(RestTemplate.class),
                mock(RestTemplate.class), externalRestTemplate, new ObjectMapper().findAndRegisterModules(),
                outboxService);
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID();
        final UUID tableId = UUID.randomUUID();
        final UUID remoteDatabaseId = UUID.randomUUID();
        final UUID remoteTableId = UUID.randomUUID();
        final TableDeleteNotificationDto notification = TableDeleteNotificationDto.builder()
                .databaseId(databaseId)
                .tableId(tableId)
                .databaseReplicaIds(Map.of("http://remote.test", remoteDatabaseId))
                .tableReplicaIds(Map.of("http://remote.test", remoteTableId))
                .build();
        when(externalRestTemplate.exchange(any(String.class), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY),
                eq(Void.class))).thenThrow(new ResourceAccessException("offline"));

        final int replicated = service.replicateTableDelete(notification);

        assertEquals(0, replicated);
        verify(outboxService).enqueue(eq(ReplicationOutboxOperationType.TABLE_DELETE),
                eq("http://remote.test"), eq(HttpMethod.DELETE), eq(notification), eq(databaseId), eq(tableId),
                eq(remoteDatabaseId), eq(remoteTableId), eq("offline"));
    }

    @Test
    public void replicateTableDelete_missingTargetIsSuccessful() {
        final RestTemplate externalRestTemplate = mock(RestTemplate.class);
        final ReplicationOutboxService outboxService = mock(ReplicationOutboxService.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(mock(RestTemplate.class),
                mock(RestTemplate.class), externalRestTemplate, new ObjectMapper().findAndRegisterModules(),
                outboxService);
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID();
        final UUID tableId = UUID.randomUUID();
        final UUID remoteDatabaseId = UUID.randomUUID();
        final UUID remoteTableId = UUID.randomUUID();
        final TableDeleteNotificationDto notification = TableDeleteNotificationDto.builder()
                .databaseId(databaseId)
                .tableId(tableId)
                .databaseReplicaIds(Map.of("http://remote.test", remoteDatabaseId))
                .tableReplicaIds(Map.of("http://remote.test", remoteTableId))
                .build();
        when(externalRestTemplate.exchange(any(String.class), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY),
                eq(Void.class))).thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        final int replicated = service.replicateTableDelete(notification);

        assertEquals(1, replicated);
        verify(outboxService, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
