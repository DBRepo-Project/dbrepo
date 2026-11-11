package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplicationInboxRetryUnitTest {
    @Test
    void deadlocksAreBoundedAndEveryAttemptClosesItsPool() throws Exception {
        check(List.of(deadlock(), deadlock(), deadlock(), deadlock()), 3);
    }

    @Test
    void permanentSqlErrorsStopEvenAfterADeadlock() throws Exception {
        check(List.of(deadlock(), new SQLException("missing table", "42S02", 1146)), 2);
    }

    @Test
    void otherSerializationFailuresAreNotAssumedToBeDeadlocks() throws Exception {
        check(List.of(new SQLException("SQL clock has not advanced", "40001")), 1);
    }

    private SQLException deadlock() { return new SQLException("deadlock", "40001", 1213); }

    private void check(List<SQLException> failures, int attempts) throws Exception {
        final String origin = "https://origin.example", target = "https://target.example";
        final var database = Database.builder().id(UUID.randomUUID()).creationLocation(origin).build();
        final var table = Table.builder().id(UUID.randomUUID()).creationLocation(origin)
                .columns(List.of(Column.builder().internalName("replication_key").build())).build();
        final var event = DataReplicationDto.builder().eventId(UUID.randomUUID()).eventSequence(1L)
                .database(DatabaseDto.builder().id(UUID.randomUUID()).creationLocation(origin)
                        .replicaUrls(Map.of(target, database.getId())).build())
                .table(TableDto.builder().id(UUID.randomUUID()).replicaUrls(Map.of(target, table.getId())).build())
                .tuple(TupleWithTimestampsDto.builder().replicationKey("K").data(Map.of("replication_key", "K"))
                        .insertedAt(Instant.parse("2026-01-01T00:00:00Z")).build()).build();
        final var service = spy(new ReplicationInboxService(null, new ObjectMapper().findAndRegisterModules(), target));
        final List<ComboPooledDataSource> pools = new ArrayList<>();
        doAnswer(call -> {
            final var pool = mock(ComboPooledDataSource.class);
            when(pool.getConnection()).thenThrow(failures.get(pools.size()));
            pools.add(pool);
            return pool;
        }).when(service).getDataSource(database);
        assertSame(failures.get(attempts - 1), assertThrows(SQLException.class,
                () -> service.apply(database, table, event, HttpMethod.POST)));
        assertEquals(attempts, pools.size());
        for (var pool : pools) verify(pool).close();
    }
}
