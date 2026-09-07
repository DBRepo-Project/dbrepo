package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.ReplicationSynchronisationDataDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.service.DataSynchronisationResult;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplicationServiceImplUnitTest {

    @Test
    public void synchroniseData_succeeds() {
        final RestTemplate metadataRestTemplate = mock(RestTemplate.class);
        final RestTemplate dataRestTemplate = mock(RestTemplate.class);
        final RestTemplate externalRestTemplate = mock(RestTemplate.class);
        final ReplicationServiceImpl service = new ReplicationServiceImpl(metadataRestTemplate, dataRestTemplate,
                externalRestTemplate, new ObjectMapper().findAndRegisterModules(), mock(ReplicationOutboxService.class));
        ReflectionTestUtils.setField(service, "baseUrl", "http://local.test");
        final UUID databaseId = UUID.randomUUID();
        final UUID tableId = UUID.randomUUID();
        final UUID remoteDatabaseId = UUID.randomUUID();
        final UUID remoteTableId = UUID.randomUUID();
        final DatabaseDto database = DatabaseDto.builder()
                .id(databaseId)
                .replicaUrls(Map.of("http://remote.test", remoteDatabaseId))
                .build();
        final TableDto table = TableDto.builder()
                .id(tableId)
                .replicaUrls(Map.of("http://remote.test", remoteTableId))
                .build();
        final TupleWithTimestampsDto sourceTuple = TupleWithTimestampsDto.builder()
                .replicationKey("key-1")
                .insertedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .data(Map.of("replication_key", "key-1", "value", 1))
                .build();
        final TupleWithTimestampsDto remoteTuple = TupleWithTimestampsDto.builder()
                .replicationKey("key-1")
                .insertedAt(Instant.parse("2026-01-01T00:00:05Z"))
                .data(Map.of("replication_key", "key-1", "value", 1))
                .build();

        when(metadataRestTemplate.exchange(eq("/api/v1/database/" + databaseId), eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok(database));
        when(metadataRestTemplate.exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId),
                eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.ok(table));
        when(dataRestTemplate.exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId
                        + "/data/replicate?page=0&size=100"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(ReplicationSynchronisationDataDto.class)))
                .thenReturn(ResponseEntity.ok(ReplicationSynchronisationDataDto.builder()
                        .tuples(List.of(sourceTuple))
                        .build()));
        when(externalRestTemplate.exchange(eq("http://remote.test/api/v1/database/" + remoteDatabaseId
                        + "/table/" + remoteTableId + "/data/replicate"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(TupleWithTimestampsDto.class)))
                .thenReturn(ResponseEntity.ok(remoteTuple));
        when(dataRestTemplate.exchange(eq("/api/v1/database/" + databaseId + "/table/" + tableId + "/timestamps"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("processed", 2)));
        when(externalRestTemplate.exchange(eq("http://remote.test/api/v1/database/" + remoteDatabaseId
                        + "/table/" + remoteTableId + "/timestamps"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("processed", 2)));

        final DataSynchronisationResult result = service.synchroniseData(databaseId, tableId, 100);

        assertEquals(1, result.pages());
        assertEquals(1, result.tuples());
        assertEquals(1, result.replicaWrites());
        verify(externalRestTemplate).exchange(eq("http://remote.test/api/v1/database/" + remoteDatabaseId
                        + "/table/" + remoteTableId + "/data/replicate"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(TupleWithTimestampsDto.class));
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
}
