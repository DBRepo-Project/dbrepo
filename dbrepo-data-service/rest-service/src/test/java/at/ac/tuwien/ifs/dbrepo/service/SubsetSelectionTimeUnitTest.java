package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetHistory;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubsetSelectionTimeUnitTest {
    @Test
    void storeRecordsChosenTimeInUtcInsteadOfCurrentTime() throws Exception {
        final var pool = mock(ComboPooledDataSource.class);
        final var connection = mock(Connection.class);
        final var statement = mock(CallableStatement.class);
        final var settings = mock(PreparedStatement.class);
        final var mapper = mock(MariaDbMapper.class);
        final var replication = mock(SubsetReplicationService.class);
        final var service = spy(new SubsetServiceMariaDbImpl(null, null, mapper, null, null, null, replication));
        doReturn(pool).when(service).getDataSource(any(Database.class));
        when(pool.getConnection()).thenReturn(connection);
        when(mapper.queryStoreStoreQueryRawQuery()).thenReturn("{call _store_query(?, ?, ?, ?, ?)}");
        when(connection.prepareCall(anyString())).thenReturn(statement);
        when(connection.prepareStatement("SET @dbrepo_subset_context=?,@dbrepo_subset_execution_sql=?"))
                .thenReturn(settings);
        final String site = "https://site.example";
        when(replication.localSite()).thenReturn(site);
        final UUID id = UUID.randomUUID();
        when(statement.getString(5)).thenReturn(id.toString());
        final Instant selection = Instant.parse("2020-02-29T12:34:56.123456Z");
        final var database = Database.builder().id(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"))
                .internalName("test").build();
        final String query = "SELECT 1 AS value";
        final String executionContext = """
                {"format":2,"databaseName":"test","databaseId":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                 "cut":0,"tables":{},"views":{}}
                """;

        // History capture has SQL integration coverage; this test checks the query-store binding and commit.
        try (var history = mockStatic(SubsetHistory.class)) {
            history.when(() -> SubsetHistory.capture(connection, database, site, query, selection))
                    .thenReturn(new SubsetHistory.Prepared(query, executionContext));
            assertEquals(id, service.storeQuery(database, query, query, selection, "alice"));
            verify(statement).setTimestamp(eq(4), eq(Timestamp.from(selection)),
                    argThat((Calendar calendar) -> calendar.getTimeZone().getID().equals("UTC")));
            verify(settings).setString(1, executionContext);
            verify(settings).setString(2, query);
            final var order = inOrder(statement, connection);
            order.verify(statement).executeUpdate();
            order.verify(connection).commit();
            verify(replication).prepare(connection, database, null);
            assertThrows(NullPointerException.class, () -> service.storeQuery(database, query, query, null, "alice"));
        }
    }

    @Test
    void replicationCannotBeOmittedByConstructedCallers() {
        assertThrows(NullPointerException.class,
                () -> new SubsetServiceMariaDbImpl(null, null, null, null, null, null, null));
    }
}
