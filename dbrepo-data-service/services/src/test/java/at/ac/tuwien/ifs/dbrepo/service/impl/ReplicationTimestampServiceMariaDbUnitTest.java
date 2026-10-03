package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReplicationTimestampServiceMariaDbUnitTest {
    private final ReplicationTimestampServiceMariaDbImpl service = spy(new ReplicationTimestampServiceMariaDbImpl());
    private final ComboPooledDataSource pool = mock(ComboPooledDataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement ddl = mock(PreparedStatement.class);
    private final PreparedStatement merge = mock(PreparedStatement.class);
    private final PreparedStatement close = mock(PreparedStatement.class);
    private final Database database = Database.builder().internalName("test").build();
    private final TupleReplicationTimestampDto timestamp = TupleReplicationTimestampDto.builder()
            .siteUrl("https://origin.example").replicationId("key").databaseId(UUID.randomUUID())
            .tableId(UUID.randomUUID()).rowStart(Instant.parse("2026-10-03T12:00:00Z")).build();

    @BeforeEach
    void setup() throws Exception {
        doReturn(pool).when(service).getDataSource(any(Database.class));
        when(pool.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            final String sql = invocation.getArgument(0);
            if (sql.startsWith("CREATE")) {
                return ddl;
            }
            if (sql.startsWith("SELECT")) {
                final PreparedStatement query = mock(PreparedStatement.class);
                final ResultSet result = mock(ResultSet.class);
                final List<String> names = List.of("site_url", "replication_id", "database_id", "table_id", "row_start");
                final int[] row = {-1};
                when(result.next()).thenAnswer(ignored -> ++row[0] < names.size());
                when(result.getString("COLUMN_NAME")).thenAnswer(ignored -> names.get(row[0]));
                when(result.getString("SUB_PART")).thenAnswer(ignored -> row[0] == 0 ? "255" : null);
                when(result.getString("ENGINE")).thenReturn("InnoDB");
                when(query.executeQuery()).thenReturn(result);
                return query;
            }
            return sql.startsWith("UPDATE") ? close : merge;
        });
    }

    @Test
    void ddlPrecedesTransactionAndCommitFollowsAllDml() throws Exception {
        service.closeAndSaveTimestamps(database, List.of(timestamp));
        final var order = inOrder(ddl, connection, close, merge, pool);
        order.verify(ddl).executeUpdate();
        order.verify(connection).setAutoCommit(false);
        order.verify(close).executeUpdate();
        order.verify(merge).executeUpdate();
        order.verify(connection).commit();
        order.verify(connection).close();
        order.verify(pool).close();
        verify(connection, never()).rollback();
    }

    @Test
    void rollbackFailureDoesNotHideOriginalSqlFailure() throws Exception {
        final SQLException failure = new SQLException("merge failed");
        final SQLException rollback = new SQLException("rollback failed");
        when(merge.executeUpdate()).thenThrow(failure);
        doThrow(rollback).when(connection).rollback();
        final SQLException actual = assertThrows(SQLException.class,
                () -> service.saveTimestamps(database, List.of(timestamp)));
        assertSame(failure, actual);
        assertArrayEquals(new Throwable[]{rollback}, actual.getSuppressed());
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
        verify(pool).close();
    }

    @Test
    void runtimeFailureAlsoRollsBack() throws Exception {
        when(merge.executeUpdate()).thenThrow(new IllegalStateException("binding failed"));
        assertThrows(IllegalStateException.class, () -> service.closeAndSaveTimestamps(database, List.of(timestamp)));
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
        verify(pool).close();
    }

    @Test
    void existingTransactionNeverGetsImplicitlyCommittedByDdl() throws Exception {
        when(connection.getAutoCommit()).thenReturn(false);
        assertThrows(SQLException.class, () -> service.saveTimestamps(database, List.of(timestamp)));
        verify(connection, never()).prepareStatement(anyString());
        verify(connection, never()).setAutoCommit(false);
        verify(connection, never()).commit();
    }

    @Test
    void incompleteOrInvertedPeriodsFailBeforeOpeningConnection() throws Exception {
        assertThrows(SQLException.class, () -> service.updateTimestampRowEnds(database, List.of(timestamp)));
        timestamp.setRowEnd(timestamp.getRowStart().minusSeconds(1));
        assertThrows(SQLException.class, () -> service.saveTimestamps(database, List.of(timestamp)));
        timestamp.setRowEnd(null);
        timestamp.setDatabaseId(null);
        assertThrows(SQLException.class, () -> service.saveTimestamps(database, List.of(timestamp)));
        verify(pool, never()).getConnection();
    }
}
