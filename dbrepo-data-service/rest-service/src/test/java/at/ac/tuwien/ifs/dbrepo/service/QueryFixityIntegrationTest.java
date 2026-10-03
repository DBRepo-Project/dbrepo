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
    private final SubsetServiceMariaDbImpl service = new SubsetServiceMariaDbImpl(null, null, mapper, null, null, null);
    private final Database database = Database.builder().internalName("query_fixity_test")
            .container(Container.builder().host("127.0.0.1").port(Integer.valueOf(System.getenv("REPLICA_SQL_TEST_PORT")))
                    .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build()).build();

    @BeforeEach
    void schema() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "root", password)) {
            connection.createStatement().execute("DROP DATABASE IF EXISTS query_fixity_test");
            connection.createStatement().execute("CREATE DATABASE query_fixity_test");
            connection.setCatalog("query_fixity_test");
            connection.createStatement().execute(mapper.queryStoreCreateTableRawQuery());
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
        }
    }

    @Test
    void storesMicrosecondSelectionAndNormalizedQueryAndReusesCanonicalIdentity() throws Exception {
        final Instant selected = Instant.parse("2020-02-29T12:34:56.123456Z");
        final var id = service.storeQuery(database, "SELECT a FROM data", "SELECT 'original' a", selected, "alice");
        assertEquals(id, service.storeQuery(database, "SELECT a FROM data", "SELECT 'original' a",
                selected.plusSeconds(1), "bob"));
        try (Connection connection = connect(); var result = connection.createStatement().executeQuery("SELECT * FROM qs_queries")) {
            assertTrue(result.next());
            assertEquals(selected, result.getTimestamp("executed", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant());
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
        try (Connection reader = DriverManager.getConnection(url + "/query_fixity_test", "fixity_reader", "test-only")) {
            assertThrows(SQLException.class, () -> hash(reader, "SELECT * FROM mysql.user"));
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url + "/query_fixity_test", "root", password);
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
