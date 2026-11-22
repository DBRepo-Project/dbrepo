package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseAvailabilityUnitTest {

    private final Container container = mock(Container.class);
    private final CreateDatabaseDto request = mock(CreateDatabaseDto.class);
    private final ComboPooledDataSource pool = mock(ComboPooledDataSource.class);
    private final MariaDbMapper mapper = mock(MariaDbMapper.class);
    private final DatabaseServiceMariaDbImpl service = spy(new DatabaseServiceMariaDbImpl(mapper));

    DatabaseAvailabilityUnitTest() {
        doReturn(pool).when(service).getDataSource(container);
        when(request.getInternalName()).thenReturn("fixture");
        when(mapper.databaseCreateDatabaseQuery("fixture")).thenReturn("CREATE DATABASE fixture");
    }

    @Test
    void reportsLossOfQuorumAsDatabaseUnavailability() throws SQLException {
        var failure = new SQLException("WSREP has not yet prepared node for application use", "08S01", 1047);
        var connection = mock(Connection.class);
        when(pool.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("CREATE DATABASE fixture")).thenThrow(failure);
        assertSame(failure, assertThrows(SQLException.class, () -> service.create(container, request)));
        verify(connection).rollback();
        verify(pool).close();
    }

    @Test
    void releasesPoolIfNoConnectionCanBeOpened() throws SQLException {
        var failure = new SQLException("Connection unavailable", "08001");
        when(pool.getConnection()).thenThrow(failure);
        assertSame(failure, assertThrows(SQLException.class, () -> service.create(container, request)));
        verify(pool).close();
    }

    @Test
    void stillReportsInvalidSqlAsMalformed() throws SQLException {
        var connection = mock(Connection.class);
        when(pool.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("CREATE DATABASE fixture")).thenThrow(new SQLException("Invalid name", "42000"));
        assertThrows(DatabaseMalformedException.class, () -> service.create(container, request));
        verify(connection).rollback();
    }
}
