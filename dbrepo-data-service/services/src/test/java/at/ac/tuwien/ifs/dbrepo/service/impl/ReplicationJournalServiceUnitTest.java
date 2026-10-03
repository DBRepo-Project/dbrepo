package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplicationJournalServiceUnitTest {
    private final TupleReplicationOutboxServiceMariaDbImpl journal = mock(TupleReplicationOutboxServiceMariaDbImpl.class);
    private final Connection connection = mock(Connection.class);
    private final ComboPooledDataSource pool = mock(ComboPooledDataSource.class);
    private final Database database = Database.builder().id(UUID.randomUUID()).build();
    private final ObjectMapper json = new ObjectMapper();
    private final ReplicationJournalService service = spy(new ReplicationJournalService(journal, json));

    @BeforeEach
    void setup() throws Exception {
        doReturn(pool).when(service).getDataSource(database);
        when(pool.getConnection()).thenReturn(connection);
        when(journal.readJournalState(connection)).thenReturn(new TupleReplicationOutboxServiceMariaDbImpl.JournalState(8, 2));
    }

    @Test
    void capturesCommittedBoundaryAndReturnsOriginalEvent() throws Exception {
        final UUID eventId = UUID.randomUUID();
        when(journal.readRange(connection, 2, 8, 100)).thenReturn(List.of(
                new TupleReplicationOutboxServiceMariaDbImpl.JournalEntry(3, eventId, database.getId(), UUID.randomUUID(),
                        HttpMethod.DELETE, json.writeValueAsString(DataReplicationDto.builder()
                        .eventId(eventId).eventSequence(3L).build()))));
        final var page = service.read(database, 2, null, 100);
        assertEquals(8, page.through());
        assertEquals(2, page.legacyThrough());
        assertEquals(3, page.nextAfter());
        assertEquals("DELETE", page.events().getFirst().method());
        assertEquals(eventId, page.events().getFirst().payload().getEventId());
        final var order = inOrder(journal, connection);
        order.verify(journal).ensureTableExists(connection);
        order.verify(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        order.verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(pool).close();
    }

    @Test
    void laterPagesKeepRequestedBoundaryDespiteNewCommits() throws Exception {
        when(journal.readRange(connection, 3, 5, 2)).thenReturn(List.of());
        final var page = service.read(database, 3, 5L, 2);
        assertEquals(5, page.through());
        assertEquals(5, page.nextAfter());
        verify(journal).readRange(connection, 3, 5, 2);
    }

    @Test
    void rejectsUncommittedOrUnboundedRanges() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.read(database, 0, 9L, 100));
        verify(connection).rollback();
        assertThrows(IllegalArgumentException.class, () -> service.read(database, -1, null, 100));
        assertThrows(IllegalArgumentException.class, () -> service.read(database, 0, null, 1001));
        verify(connection, never()).commit();
    }

    @Test
    void damagedPayloadAndMissingEventsNeverBecomeEmptySuccess() throws Exception {
        when(journal.readRange(connection, 0, 8, 100)).thenThrow(new SQLException("missing source event"));
        assertThrows(SQLException.class, () -> service.read(database, 0, null, 100));
        doReturn(List.of(
                new TupleReplicationOutboxServiceMariaDbImpl.JournalEntry(1, UUID.randomUUID(), database.getId(),
                        UUID.randomUUID(), HttpMethod.POST, "{broken"))).when(journal).readRange(connection, 0, 8, 100);
        assertThrows(SQLException.class, () -> service.read(database, 0, null, 100));
        verify(connection, times(2)).rollback();
        verify(connection, never()).commit();
    }
}
