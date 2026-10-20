package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubsetReplicationDispatcherUnitTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void evidencePagesCloseBeforeHttpAndKeepVersionsAcrossTheSameKeyBoundary(boolean failSecondPage) throws Exception {
        final String origin = "https://a.example", target = "https://b.example";
        final var table = Table.builder().id(UUID.randomUUID()).internalName("samples")
                .replicaUrls(Map.of(target, UUID.randomUUID())).build();
        final var database = Database.builder().id(UUID.randomUUID()).internalName("test")
                .tables(List.of(table)).build();
        final UUID queryId = UUID.randomUUID(), targetId = UUID.randomUUID();
        final Instant start = Instant.parse("2026-01-01T00:00:00.123456Z");
        final String context = new ObjectMapper().findAndRegisterModules().writeValueAsString(
                new SubsetHistory.Execution(2, "test", database.getId(), 0,
                        Map.of("samples", new SubsetHistory.Binding(table.getId(), false,
                                new SubsetHistory.Proof(1, "a".repeat(64)))), Map.of()));
        final var payload = new SubsetReplicationDto(queryId, origin, origin, database.getId(),
                "SELECT * FROM samples", "SELECT * FROM samples", start.plusSeconds(1), false,
                "v2:" + "a".repeat(64), 1L, 1, context);
        final var pool = mock(ComboPooledDataSource.class);
        final var connection = mock(Connection.class);
        final var control = mock(Statement.class);
        final var pending = mock(Statement.class);
        final var lock = mock(ResultSet.class);
        final var jobs = mock(ResultSet.class);
        final var first = mock(PreparedStatement.class);
        final var second = mock(PreparedStatement.class);
        final var acknowledgement = mock(PreparedStatement.class);
        final var failure = mock(PreparedStatement.class);
        final var replication = mock(SubsetReplicationService.class);
        final var client = mock(RestTemplate.class);
        final var mapper = mock(MariaDbMapper.class);
        final var firstClosed = new AtomicBoolean();
        final var secondClosed = new AtomicBoolean();
        final var firstRows = page(256, start, false, firstClosed);
        final var secondRows = page(1, start.plusNanos(1000), true, secondClosed);
        when(pool.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(control, pending);
        when(control.executeQuery(anyString())).thenReturn(lock);
        when(lock.next()).thenReturn(true, false);
        when(lock.getInt(1)).thenReturn(1);
        when(pending.executeQuery(anyString())).thenReturn(jobs);
        when(jobs.next()).thenReturn(true, false);
        when(jobs.getString(1)).thenReturn(queryId.toString());
        when(jobs.getString(2)).thenReturn(target);
        when(jobs.getLong(3)).thenReturn(1L);
        when(connection.prepareStatement(startsWith("SELECT * FROM tuple_replication_timestamps")))
                .thenReturn(first, second);
        when(first.executeQuery()).thenReturn(firstRows);
        when(second.executeQuery()).thenReturn(secondRows);
        when(connection.prepareStatement(startsWith("DELETE FROM qs_subset_outbox"))).thenReturn(acknowledgement);
        when(connection.prepareStatement(startsWith("UPDATE qs_subset_outbox"))).thenReturn(failure);
        when(replication.routes(database)).thenReturn(Map.of(target, targetId));
        when(replication.read(connection, database, queryId)).thenReturn(payload);
        when(mapper.queryStoreCreateSubsetOutboxRawQuery()).thenReturn("CREATE TABLE IF NOT EXISTS qs_subset_outbox()");
        final List<TupleReplicationTimestampDto> delivered = new ArrayList<>();
        when(client.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenAnswer(call -> {
                    assertTrue(firstClosed.get(), "A streaming query must not remain open during HTTP delivery");
                    if (!delivered.isEmpty()) assertTrue(secondClosed.get());
                    final HttpEntity<List<TupleReplicationTimestampDto>> request = call.getArgument(2);
                    delivered.addAll(Objects.requireNonNull(request.getBody()));
                    if (failSecondPage && secondClosed.get()) throw new ResourceAccessException("Lost evidence acknowledgement");
                    return ResponseEntity.noContent().build();
                });
        when(client.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenAnswer(call -> {
                    assertEquals(257, delivered.size(), "Deliver evidence before canonical query metadata");
                    return ResponseEntity.noContent().build();
                });
        final var dispatcher = new SubsetReplicationDispatcher(null, replication, client, mapper) {
            @Override public ComboPooledDataSource getDataSource(Database ignored) { return pool; }
        };
        assertEquals(failSecondPage ? 0 : 1, dispatcher.dispatch(database));
        verify(second).setString(5, "K255");
        verify(second).setString(6, "K255");
        verify(second).setTimestamp(eq(7), eq(Timestamp.from(start)), any(Calendar.class));
        assertEquals(delivered.get(255).getReplicationId(), delivered.get(256).getReplicationId());
        assertEquals(start.plusNanos(1000), delivered.get(256).getRowStart());
        if (failSecondPage) {
            verify(acknowledgement, never()).executeUpdate();
            verify(client, never()).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));
            verify(failure).executeUpdate();
        } else {
            verify(acknowledgement).executeUpdate();
        }
    }

    private ResultSet page(int count, Instant start, boolean continuation, AtomicBoolean closed) throws Exception {
        final var rows = mock(ResultSet.class);
        final var index = new AtomicInteger(-1);
        when(rows.next()).thenAnswer(call -> index.incrementAndGet() < count);
        when(rows.getString("replication_id")).thenAnswer(call -> "K" + String.format("%03d", continuation ? 255 : index.get()));
        when(rows.getTimestamp(eq("row_start"), any(Calendar.class))).thenReturn(Timestamp.from(start));
        when(rows.getTimestamp(eq("master_site_ts"), any(Calendar.class))).thenReturn(Timestamp.from(start));
        doAnswer(call -> { closed.set(true); return null; }).when(rows).close();
        return rows;
    }
}
