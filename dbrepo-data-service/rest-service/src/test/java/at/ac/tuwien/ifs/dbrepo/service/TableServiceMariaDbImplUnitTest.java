package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TableServiceMariaDbImplUnitTest {

    @Test
    public void explore_nullTables_succeeds() throws SQLException, TableNotFoundException,
            DatabaseMalformedException {
        final DataMapper dataMapper = mock(DataMapper.class);
        final MariaDbMapper mariaDbMapper = mock(MariaDbMapper.class);
        final SubsetService subsetService = mock(SubsetService.class);
        final StorageService storageService = mock(StorageService.class);
        final DataService dataService = mock(DataService.class);
        final TableServiceMariaDbImpl tableService = spy(new TableServiceMariaDbImpl(dataMapper, mariaDbMapper,
                subsetService, storageService, dataService));
        final Database database = Database.builder()
                .internalName("db")
                .tables(null)
                .build();
        final ComboPooledDataSource dataSource = mock(ComboPooledDataSource.class);
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet resultSet = mock(ResultSet.class);
        when(mariaDbMapper.databaseTablesSelectRawQuery()).thenReturn("select tables");
        doReturn(dataSource).when(tableService).getDataSource(database);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select tables")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn("weather_aus");
        doReturn(TableDto.builder()
                .internalName("weather_aus")
                .build()).when(tableService).inspect(database, "weather_aus");

        final List<TableDto> response = tableService.explore(database);

        assertEquals(1, response.size());
        assertEquals("weather_aus", response.get(0).getInternalName());
        verify(tableService).inspect(database, "weather_aus");
    }

}
