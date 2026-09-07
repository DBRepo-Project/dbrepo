package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.ReplicationSynchronisationDataDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.ColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceMariaDbImpl;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    public void getReplicationData_pagedRows_succeeds() throws Exception {
        final TestTableServiceMariaDbImpl tableService = testTableService("replication_data");
        final Database database = database();
        final Table table = table();
        tableService.createReplicationTable(database);

        final ReplicationSynchronisationDataDto response = tableService.getReplicationData(database, table, 0, 1,
                "http://site.local/");

        assertEquals(1, response.getTuples().size());
        final TupleWithTimestampsDto tuple = response.getTuples().get(0);
        assertEquals("key-a", tuple.getReplicationKey());
        assertEquals("key-a", tuple.getData().get("replication_key"));
        assertEquals(1, tuple.getData().get("sample_value"));
        assertNotNull(tuple.getInsertedAt());
        assertNull(tuple.getDeletedAt());
        assertEquals(1, response.getReplicationTimestamps().size());
        assertEquals("http://site.local", response.getReplicationTimestamps().get(0).getSiteUrl());
        assertEquals("key-a", response.getReplicationTimestamps().get(0).getReplicationId());
        assertEquals(database.getId(), response.getReplicationTimestamps().get(0).getDatabaseId());
        assertEquals(table.getId(), response.getReplicationTimestamps().get(0).getTableId());
        assertNull(response.getReplicationTimestamps().get(0).getRowEnd());
    }

    @Test
    public void getReplicationData_missingReplicationKey_fails() {
        final TestTableServiceMariaDbImpl tableService = testTableService("replication_data_missing_key");
        final Database database = database();
        final Table table = Table.builder()
                .id(UUID.randomUUID())
                .internalName("test_table")
                .columns(List.of(Column.builder()
                        .internalName("sample_value")
                        .columnType(ColumnType.INT)
                        .build()))
                .build();

        assertThrows(TableMalformedException.class, () -> {
            tableService.getReplicationData(database, table, 0, 100, "http://site.local");
        });
    }

    private TestTableServiceMariaDbImpl testTableService(String name) {
        return new TestTableServiceMariaDbImpl(mock(DataMapper.class), mock(MariaDbMapper.class),
                mock(SubsetService.class), mock(StorageService.class), mock(DataService.class),
                "jdbc:h2:mem:table_service_" + name + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }

    private Database database() {
        return Database.builder()
                .id(UUID.randomUUID())
                .internalName("test_database")
                .build();
    }

    private Table table() {
        return Table.builder()
                .id(UUID.randomUUID())
                .internalName("test_table")
                .columns(List.of(Column.builder()
                                .internalName("replication_key")
                                .columnType(ColumnType.VARCHAR)
                                .build(),
                        Column.builder()
                                .internalName("sample_value")
                                .columnType(ColumnType.INT)
                                .build()))
                .build();
    }

    private static class TestTableServiceMariaDbImpl extends TableServiceMariaDbImpl {

        private final String jdbcUrl;

        TestTableServiceMariaDbImpl(DataMapper dataMapper, MariaDbMapper mariaDbMapper, SubsetService subsetService,
                                    StorageService storageService, DataService computeService, String jdbcUrl) {
            super(dataMapper, mariaDbMapper, subsetService, storageService, computeService);
            this.jdbcUrl = jdbcUrl;
        }

        @Override
        public ComboPooledDataSource getDataSource(Database database) {
            try {
                final ComboPooledDataSource dataSource = new ComboPooledDataSource();
                dataSource.setDriverClass("org.h2.Driver");
                dataSource.setJdbcUrl(jdbcUrl);
                dataSource.setUser("sa");
                dataSource.setPassword("");
                dataSource.setInitialPoolSize(1);
                dataSource.setMinPoolSize(1);
                dataSource.setMaxPoolSize(2);
                return dataSource;
            } catch (PropertyVetoException e) {
                throw new IllegalStateException(e);
            }
        }

        void createReplicationTable(Database database) throws Exception {
            try (ComboPooledDataSource dataSource = getDataSource(database);
                 Connection connection = dataSource.getConnection();
                 java.sql.Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + database.getInternalName());
                statement.execute("CREATE TABLE " + database.getInternalName()
                        + ".test_table (replication_key VARCHAR(255), sample_value INT, ROW_START TIMESTAMP, "
                        + "ROW_END TIMESTAMP)");
                statement.execute("INSERT INTO " + database.getInternalName()
                        + ".test_table (replication_key, sample_value, ROW_START, ROW_END) VALUES "
                        + "('key-b', 2, TIMESTAMP '2026-01-02 00:00:00', TIMESTAMP '2038-01-19 03:14:07'), "
                        + "('key-a', 1, TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2038-01-19 03:14:07')");
            }
        }
    }

}
