package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetServiceMariaDbImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "REPLICA_SQL_TEST_PORT", matches = "[0-9]+")
class QueryFixityIntegrationTest {
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("REPLICA_SQL_TEST_PORT");
    private final String password = System.getenv("REPLICA_SQL_TEST_PASSWORD");
    private final MariaDbMapper mapper = Mappers.getMapper(MariaDbMapper.class);
    private final SubsetServiceMariaDbImpl service = new SubsetServiceMariaDbImpl(null, null, mapper, null, null, null,
            org.mockito.Mockito.mock(SubsetReplicationService.class));
    private final Database database = Database.builder().internalName("query_fixity_test")
            .container(Container.builder().host("127.0.0.1").port(Integer.valueOf(System.getenv("REPLICA_SQL_TEST_PORT")))
                    .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build()).build();

    @BeforeEach
    void schema() throws Exception {
        try (Connection connection = DriverManager.getConnection(url + "?sslMode=disable", "root", password)) {
            connection.createStatement().execute("DROP DATABASE IF EXISTS query_fixity_test");
            connection.createStatement().execute("CREATE DATABASE query_fixity_test");
            connection.setCatalog("query_fixity_test");
            connection.createStatement().execute(mapper.queryStoreCreateTableRawQuery());
            connection.createStatement().execute(mapper.queryStoreCreateMetadataLockRawQuery());
            connection.createStatement().execute(mapper.queryStoreCreateHashTableProcedureRawQuery());
            connection.createStatement().execute(mapper.queryStoreCreateInternalStoreQueryProcedureRawQuery());
            connection.createStatement().execute(mapper.queryStoreCreateStoreQueryProcedureRawQuery());
            connection.createStatement().execute(mapper.queryStoreCreateInternalHashQueryProcedureRawQuery());
            connection.createStatement().execute("CREATE TABLE data (a VARCHAR(32), b VARCHAR(32))");
        }
    }

    @Test
    void hashDistinguishesBoundariesNullPositionsTypesAndDuplicateRows() throws Exception {
        try (Connection connection = connect()) {
            connection.createStatement().execute("INSERT INTO data VALUES ('ab','c')");
            final String first = hash(connection, "SELECT * FROM data");
            connection.createStatement().execute("UPDATE data SET a='a', b='bc'");
            assertNotEquals(first, hash(connection, "SELECT * FROM data"));
            connection.createStatement().execute("DELETE FROM data");
            assertNotEquals(hash(connection, "SELECT 'ab' a,'c' b"), hash(connection, "SELECT 'a' a,'bc' b"));
            assertNotEquals(hash(connection, "SELECT NULL a,'x' b"), hash(connection, "SELECT 'x' a,NULL b"));
            assertNotEquals(hash(connection, "SELECT NULL a"), hash(connection, "SELECT '' a"));
            assertNotEquals(hash(connection, "SELECT 1 a"), hash(connection, "SELECT '1' a"));
            assertNotEquals(hash(connection, "SELECT 'x' a"), hash(connection, "SELECT 'x' a UNION ALL SELECT 'x' a"));
            assertNotNull(hash(connection, "SELECT * FROM data WHERE FALSE"));
            connection.createStatement().execute("INSERT INTO data VALUES ('z',NULL),('a',''),('a','')");
            assertEquals(hash(connection, "SELECT * FROM data ORDER BY a ASC"),
                    hash(connection, "SELECT * FROM data ORDER BY a DESC"));
            connection.createStatement().execute("SET SESSION group_concat_max_len=10");
            final String smallLimit = hash(connection, "SELECT * FROM data");
            connection.createStatement().execute("SET SESSION group_concat_max_len=100000");
            assertEquals(smallLimit, hash(connection, "SELECT * FROM data"));
            assertNotEquals(hash(connection, "SELECT CONCAT(REPEAT('x',4096),'a') a"),
                    hash(connection, "SELECT CONCAT(REPEAT('x',4096),'b') a"));
        }
    }

    @Test
    void changedColumnBoundariesProduceDifferentSubsetIds() throws Exception {
        try (Connection connection = connect(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO data VALUES ('ab','c')");
            statement.execute("CALL store_query('SELECT * FROM data','SELECT * FROM data',NOW(6),@first)");
            statement.execute("UPDATE data SET a='a',b='bc'");
            statement.execute("CALL store_query('SELECT * FROM data','SELECT * FROM data',NOW(6),@second)");
            try (var result = statement.executeQuery("SELECT @first <> @second, COUNT(*) FROM qs_queries")) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
                assertEquals(2, result.getInt(2));
            }
        }
    }

    @Test
    void directStoreRetainsHistoricalSqlAfterTheCurrentRowChanges() throws Exception {
        final String original = "SELECT value FROM versions";
        final String historical = "SELECT value FROM versions FOR SYSTEM_TIME AS OF TIMESTAMP '2023-11-14 22:13:25.000000'";
        try (Connection connection = connect(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE versions(id INT PRIMARY KEY, value INT) WITH SYSTEM VERSIONING");
            statement.execute("SET timestamp=1700000000");
            statement.execute("INSERT INTO versions VALUES(1,11)");
            statement.execute("SET timestamp=1700000010");
            statement.execute("UPDATE versions SET value=22");
            statement.execute("SET timestamp=DEFAULT");
            try (var call = connection.prepareCall("{call store_query(?,?,?,?)}")) {
                call.setString(1, original);
                call.setString(2, historical);
                call.setTimestamp(3, Timestamp.from(Instant.parse("2023-11-14T22:13:25Z")));
                call.registerOutParameter(4, Types.VARCHAR);
                call.execute();
            }
            final String stored;
            try (var result = statement.executeQuery("SELECT query,query_normalized FROM qs_queries")) {
                assertTrue(result.next());
                assertEquals(original, result.getString(1));
                stored = result.getString(2);
                assertEquals(historical, stored);
            }
            try (var result = statement.executeQuery(stored)) {
                assertTrue(result.next());
                assertEquals(11, result.getInt(1));
            }
            try (var result = statement.executeQuery(original)) {
                assertTrue(result.next());
                assertEquals(22, result.getInt(1));
            }
        }
    }

    @Test
    void storesMicrosecondSelectionAndNormalizedQueryAndReusesCanonicalIdentity() throws Exception {
        final Instant selected = Instant.parse("2020-02-29T12:34:56.123456Z");
        final var id = service.storeQuery(database, "select 'original' as a", "SELECT 'original' a", selected, "alice");
        assertEquals(id, service.storeQuery(database, "select 'original' as a", "SELECT 'original' a",
                selected.plusSeconds(1), "bob"));
        try (Connection connection = connect(); var result = connection.createStatement().executeQuery("SELECT * FROM qs_queries")) {
            assertTrue(result.next());
            assertEquals(selected, result.getTimestamp("executed", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant());
            assertEquals("select 'original' as a", result.getString("query"));
            assertEquals("SELECT 'original' a", result.getString("query_normalized"));
            assertTrue(result.getString("result_hash").startsWith("v2:"));
            assertFalse(result.next());
        }
        service.upgradeQueryStore(database);
        service.upgradeQueryStore(database);
        try (Connection connection = connect(); var count = connection.createStatement().executeQuery("SELECT COUNT(*) FROM qs_queries FOR SYSTEM_TIME ALL")) {
            assertTrue(count.next());
            assertEquals(1, count.getInt(1));
        }
    }

    @Test
    void parallelCallsNeverShareWorkTablesOrCreateDuplicateIdentities() throws Exception {
        try (var executor = Executors.newFixedThreadPool(6)) {
            final var calls = new ArrayList<Callable<String>>();
            for (int i = 0; i < 18; i++) {
                calls.add(() -> {
                    try (Connection connection = connect(); CallableStatement call = connection.prepareCall("{call store_query(?,?,?,?)}")) {
                        call.setString(1, "SELECT a FROM data");
                        call.setString(2, "SELECT 'same' a");
                        call.setTimestamp(3, Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")));
                        call.registerOutParameter(4, Types.VARCHAR);
                        call.execute();
                        return call.getString(4);
                    }
                });
            }
            final var results = executor.invokeAll(calls);
            final String id = results.getFirst().get();
            for (var result : results) assertEquals(id, result.get());
        }
        try (Connection connection = connect(); var tables = connection.createStatement().executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE '_dbrepo_query_%'")) {
            assertTrue(tables.next());
            assertEquals(0, tables.getInt(1));
        }
    }

    @Test
    void routinesDoNotElevateSqlReaderPrivileges() throws Exception {
        try (Connection connection = connect()) {
            connection.createStatement().execute("DROP USER IF EXISTS fixity_reader@'%'");
            connection.createStatement().execute("CREATE USER fixity_reader@'%' IDENTIFIED BY 'test-only'");
            connection.createStatement().execute("GRANT SELECT,EXECUTE ON query_fixity_test.* TO fixity_reader@'%'");
        }
        try (Connection reader = DriverManager.getConnection(url + "/query_fixity_test" + "?sslMode=disable", "fixity_reader", "test-only")) {
            assertThrows(SQLException.class, () -> hash(reader, "SELECT * FROM mysql.user"));
        }
    }

    @Test
    void missingGuardFailsWithoutLeavingWorkTablesOrChangingSessionTimeout() throws Exception {
        try (Connection connection = connect()) {
            connection.createStatement().execute("DELETE FROM qs_metadata_lock");
            connection.createStatement().execute("SET SESSION innodb_lock_wait_timeout=37");
            assertThrows(SQLException.class, () -> connection.createStatement().execute(
                    "CALL store_query('SELECT 1 a','SELECT 1 a',NOW(6),@id)"));
            try (var rows = connection.createStatement().executeQuery(
                    "SELECT @@innodb_lock_wait_timeout, @@in_transaction, (SELECT COUNT(*) FROM qs_queries), "
                            + "(SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() "
                            + "AND table_name LIKE '_dbrepo_query_%')")) {
                assertTrue(rows.next());
                assertEquals(37, rows.getInt(1));
                assertEquals(0, rows.getInt(2));
                assertEquals(0, rows.getInt(3));
                assertEquals(0, rows.getInt(4));
            }
        }
    }

    @Test
    void upgradeRejectsUnrelatedGuardTableWithoutAdoptingIt() throws Exception {
        try (Connection connection = connect()) {
            connection.createStatement().execute("DROP TABLE qs_metadata_lock");
            connection.createStatement().execute("CREATE TABLE qs_metadata_lock(id INT PRIMARY KEY, marker INT)");
            connection.createStatement().execute("INSERT INTO qs_metadata_lock VALUES(1,42)");
        }
        assertThrows(SQLException.class, () -> service.upgradeQueryStore(database));
        try (Connection connection = connect(); var rows = connection.createStatement().executeQuery(
                "SELECT marker FROM qs_metadata_lock")) {
            assertTrue(rows.next());
            assertEquals(42, rows.getInt(1));
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url + "/query_fixity_test" + "?sslMode=disable", "root", password);
    }

    private String hash(Connection connection, String sql) throws SQLException {
        try (CallableStatement call = connection.prepareCall("{call hash_query(?,?)}")) {
            call.setString(1, sql);
            call.registerOutParameter(2, Types.VARCHAR);
            call.execute();
            return call.getString(2);
        }
    }
}
