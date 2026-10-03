package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
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
        final var mapper = mock(MariaDbMapper.class);
        final var service = spy(new SubsetServiceMariaDbImpl(null, null, mapper, null, null, null));
        doReturn(pool).when(service).getDataSource((at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database) any());
        when(pool.getConnection()).thenReturn(connection);
        when(mapper.queryStoreStoreQueryRawQuery()).thenReturn("{call _store_query(?, ?, ?, ?, ?)}");
        when(connection.prepareCall(anyString())).thenReturn(statement);
        final UUID id = UUID.randomUUID();
        when(statement.getString(5)).thenReturn(id.toString());
        final Instant selection = Instant.parse("2020-02-29T12:34:56.123456Z");
        final var database = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database.builder().internalName("test").build();

        assertEquals(id, service.storeQuery(database, "query", "timestamped", selection, "alice"));
        verify(statement).setTimestamp(eq(4), eq(Timestamp.from(selection)),
                argThat((Calendar calendar) -> calendar.getTimeZone().getID().equals("UTC")));
        verify(connection).commit();
        assertThrows(NullPointerException.class, () -> service.storeQuery(database, "query", "timestamped", null, "alice"));
    }
}
