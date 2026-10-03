package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.ColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MariaDbReplicationBindingTest {
    private static final String EXACT = "12345678901234567890123456789012345.123456789012345678901234567890";
    private final MariaDbMapper mapper = Mappers.getMapper(MariaDbMapper.class);
    private final Table table = Table.builder().internalName("mapper_binding_test")
            .columns(List.of(Column.builder().internalName("amount").columnType(ColumnType.DECIMAL).build(),
                    Column.builder().internalName("key_text").columnType(ColumnType.VARCHAR).build())).build();

    @Test
    void decimalBindingNeverConvertsThroughFloatingPointAndKeepsTypedNull() throws Exception {
        final PreparedStatement statement = mock(PreparedStatement.class);
        final StorageService storage = mock(StorageService.class);
        int position = 1;
        for (Object value : List.of(EXACT, new BigDecimal(EXACT), 42, "-0.000000000000000000000000000001", "1E+30")) {
            mapper.prepareStatementWithColumnTypeObject(storage, statement, ColumnType.DECIMAL, position, "amount", value);
            verify(statement).setBigDecimal(position++, new BigDecimal(String.valueOf(value)));
        }
        mapper.prepareStatementWithColumnTypeObject(storage, statement, ColumnType.DECIMAL, position, "amount", null);
        verify(statement).setNull(position, Types.DECIMAL);
        verifyNoMoreInteractions(statement);
        verifyNoInteractions(storage);
    }

    @Test
    void deleteKeepsOnePlaceholderForEveryNullableOrNonNullKey() throws Exception {
        final Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("key_text", null);
        keys.put("amount", EXACT);
        final String sql = mapper.tupleToRawDeleteQuery("subset_replication_test", table, new TupleDeleteDto(keys));
        assertEquals("DELETE FROM `subset_replication_test`.`mapper_binding_test` WHERE `key_text` <=> ? AND `amount` <=> ?", sql);
        keys.put("key_text", "present");
        keys.put("amount", null);
        assertEquals(sql, mapper.tupleToRawDeleteQuery("subset_replication_test", table, new TupleDeleteDto(keys)));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SUBSET_SQL_TEST_PORT", matches = "[0-9]+")
    void mariaDbRoundTripsExactDecimalAndExecutesMixedNullableDeleteKeys() throws Exception {
        final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("SUBSET_SQL_TEST_PORT");
        try (Connection connection = DriverManager.getConnection(url, "root", System.getenv("SUBSET_SQL_TEST_PASSWORD"));
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS subset_replication_test");
            connection.setCatalog("subset_replication_test");
            statement.execute("CREATE TEMPORARY TABLE mapper_binding_test (marker INT, amount DECIMAL(65,30), key_text VARCHAR(32))");
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO mapper_binding_test VALUES (1, ?, NULL), (2, NULL, 'keep'), (3, ?, 'keep')")) {
                mapper.prepareStatementWithColumnTypeObject(null, insert, ColumnType.DECIMAL, 1, "amount", EXACT);
                mapper.prepareStatementWithColumnTypeObject(null, insert, ColumnType.DECIMAL, 2, "amount", new BigDecimal(EXACT));
                assertEquals(3, insert.executeUpdate());
            }
            try (ResultSet row = statement.executeQuery("SELECT amount FROM mapper_binding_test WHERE marker=1")) {
                assertTrue(row.next());
                assertEquals(new BigDecimal(EXACT), row.getBigDecimal(1));
            }
            final Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("key_text", null);
            keys.put("amount", EXACT);
            assertEquals(1, delete(connection, keys));
            keys.put("key_text", "keep");
            keys.put("amount", null);
            assertEquals(1, delete(connection, keys));
            try (ResultSet rows = statement.executeQuery("SELECT marker FROM mapper_binding_test")) {
                assertTrue(rows.next());
                assertEquals(3, rows.getInt(1));
                assertFalse(rows.next());
            }
        }
    }

    private int delete(Connection connection, Map<String, Object> keys) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                mapper.tupleToRawDeleteQuery("subset_replication_test", table, new TupleDeleteDto(keys)))) {
            int position = 1;
            for (var key : keys.entrySet()) {
                mapper.prepareStatementWithColumnTypeObject(null, statement,
                        key.getKey().equals("amount") ? ColumnType.DECIMAL : ColumnType.VARCHAR,
                        position++, key.getKey(), key.getValue());
            }
            return statement.executeUpdate();
        }
    }
}
