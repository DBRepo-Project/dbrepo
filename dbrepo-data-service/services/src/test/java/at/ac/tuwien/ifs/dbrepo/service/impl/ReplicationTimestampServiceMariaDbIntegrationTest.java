package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Opt-in SQL test: only resets its table in the dedicated timestamp_replication_test database. */
@EnabledIfEnvironmentVariable(named = "TIMESTAMP_SQL_TEST_PORT", matches = "[0-9]+")
class ReplicationTimestampServiceMariaDbIntegrationTest {
    private static final String SCHEMA = "timestamp_replication_test";
    private static final UUID DATABASE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TABLE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant START = Instant.parse("2026-10-03T12:00:00.000001Z");
    private static final Instant NEXT = START.plusSeconds(1);
    private static final Instant END = START.plusSeconds(2);
    // Matches dbrepo-data-db/1_setup-schema.sql; keep upgrade coverage while that initializer is legacy.
    private static final String LEGACY_SCHEMA = """
            CREATE TABLE tuple_replication_timestamps (
                site_url TEXT NOT NULL, replication_id VARCHAR(255) NOT NULL,
                database_id VARCHAR(36) NOT NULL, table_id VARCHAR(36) NOT NULL,
                row_start TIMESTAMP(6) NOT NULL, row_end TIMESTAMP(6),
                PRIMARY KEY (site_url(255), replication_id, row_start)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    private final Database database = Database.builder().id(UUID.randomUUID()).internalName(SCHEMA).build();
    private final ReplicationTimestampServiceMariaDbImpl service = spy(new ReplicationTimestampServiceMariaDbImpl());
    private String url;
    private String password;

    @BeforeEach
    void setup() throws Exception {
        final int port = Integer.parseInt(System.getenv("TIMESTAMP_SQL_TEST_PORT"));
        assertTrue(port >= 1024 && port <= 65535 && port != 3306, "Use a dedicated loopback SQL test port");
        password = System.getenv("TIMESTAMP_SQL_TEST_PASSWORD");
        assertNotNull(password, "TIMESTAMP_SQL_TEST_PASSWORD must be set explicitly");
        url = "jdbc:mariadb://127.0.0.1:" + port;
        try (Connection connection = DriverManager.getConnection(url + "?connectTimeout=5000&socketTimeout=30000", "root", password)) {
            connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + SCHEMA);
        }
        sql("DROP TABLE IF EXISTS tuple_replication_timestamps");
        final ComboPooledDataSource pool = mock(ComboPooledDataSource.class);
        when(pool.getConnection()).thenAnswer(invocation -> connection());
        doReturn(pool).when(service).getDataSource(any(Database.class));
    }

    @Test
    void migratesLegacyKeyWithoutGuessingLostIdentitiesAndSeparatesSourceScopes() throws Exception {
        sql(LEGACY_SCHEMA);
        sql("""
                INSERT INTO tuple_replication_timestamps VALUES
                ('https://origin.example', 'legacy', '00000000-0000-0000-0000-000000000001',
                 '00000000-0000-0000-0000-000000000002', '2026-10-03 12:00:00.000001', NULL)
                """);
        // Reproduce an already-lost identity under the old key, rather than pretend migration can recover it.
        sql("""
                INSERT INTO tuple_replication_timestamps VALUES
                ('https://origin.example', 'legacy', '00000000-0000-0000-0000-000000000001',
                 '00000000-0000-0000-0000-000000000003', '2026-10-03 12:00:00.000001', '2026-10-03 12:00:02.000001')
                ON DUPLICATE KEY UPDATE table_id=VALUES(table_id), row_end=VALUES(row_end)
                """);
        final List<String> surviving = rows("WHERE replication_id='legacy'");
        assertEquals(1, surviving.size());
        service.saveTimestamps(database, List.of(timestamp("probe", START, null)));
        assertEquals(surviving, rows("WHERE replication_id='legacy'"));
        assertEquals(List.of("site_url", "replication_id", "database_id", "table_id", "row_start"), primaryKey());
        assertEquals(2, rows("").size(), "Only the surviving legacy row and the supplied probe may exist");

        final var sameTableDifferentDatabase = timestamp("legacy", START, NEXT);
        sameTableDifferentDatabase.setDatabaseId(UUID.randomUUID());
        final var otherSite = timestamp("legacy", START, null);
        otherSite.setSiteUrl("https://other.example");
        service.saveTimestamps(database, List.of(timestamp("legacy", START, null), sameTableDifferentDatabase, otherSite));
        assertEquals(4, rows("WHERE replication_id='legacy'").size());
        assertTrue(rows("").contains(surviving.getFirst()), "The ambiguous surviving legacy evidence must remain untouched");
        final List<String> beforeRetry = rows("");
        service.saveTimestamps(database, List.of(timestamp("legacy", START, null), sameTableDifferentDatabase, otherSite));
        assertEquals(beforeRetry, rows(""), "Repeated schema checks and delivery must be idempotent");
    }

    @Test
    void oldPostAndLaterPatchCannotReopenOrExtendKnownClosedVersion() throws Exception {
        service.saveTimestamps(database, List.of(timestamp("key", START, null)));
        service.updateTimestampRowEnds(database, List.of(timestamp("key", START, END)));
        final List<String> closed = rows("");
        service.saveTimestamps(database, List.of(timestamp("key", START, null)));
        service.updateTimestampRowEnds(database, List.of(timestamp("key", START, END.plusSeconds(1))));
        service.saveTimestamps(database, List.of(timestamp("key", START, END.plusSeconds(2))));
        assertEquals(closed, rows(""));
        service.updateTimestampRowEnds(database, List.of(timestamp("key", START, NEXT)));
        assertEquals(NEXT, end("key", START));
        service.saveTimestamps(database, List.of(timestamp("key", START, null)));
        assertEquals(NEXT, end("key", START));
    }

    @Test
    void putAndPatchNeverCloseAnotherSourceTableOrDatabase() throws Exception {
        final var otherTable = timestamp("key", START, null);
        otherTable.setTableId(UUID.randomUUID());
        final var otherDatabase = timestamp("key", START, null);
        otherDatabase.setDatabaseId(UUID.randomUUID());
        service.saveTimestamps(database, List.of(timestamp("key", START, null), otherTable, otherDatabase));
        final List<String> otherRows = rows("WHERE database_id!='" + DATABASE + "' OR table_id!='" + TABLE + "'");
        service.closeAndSaveTimestamps(database, List.of(timestamp("key", NEXT, null)));
        service.updateTimestampRowEnds(database, List.of(timestamp("key", NEXT, END)));
        assertEquals(otherRows, rows("WHERE database_id!='" + DATABASE + "' OR table_id!='" + TABLE + "'"));
        assertEquals(4, rows("").size());
    }

    @Test
    void reorderedPostPutPatchForSameVersionConvergeAndAreIdempotent() throws Exception {
        final List<List<String>> orders = List.of(List.of("POST", "PUT", "PATCH"), List.of("POST", "PATCH", "PUT"),
                List.of("PUT", "POST", "PATCH"), List.of("PUT", "PATCH", "POST"),
                List.of("PATCH", "POST", "PUT"), List.of("PATCH", "PUT", "POST"));
        for (List<String> order : orders) {
            final String key = String.join("_", order);
            service.saveTimestamps(database, List.of(timestamp(key, START, null)));
            for (String operation : order) {
                deliver(operation, timestamp(key, NEXT, operation.equals("PATCH") ? END : null));
            }
            assertEquals(NEXT, end(key, START), order.toString());
            assertEquals(END, end(key, NEXT), order.toString());
            final List<String> before = rows("");
            for (String operation : order.reversed()) {
                deliver(operation, timestamp(key, NEXT, operation.equals("PATCH") ? END : null));
            }
            service.saveTimestamps(database, List.of(timestamp(key, START, null)));
            assertEquals(before, rows(""), order.toString());
        }
        assertEquals(12, rows("").size());
    }

    @Test
    void allThreeBatchMethodsRollBackEarlierWritesWhenLaterSqlFails() throws Exception {
        for (String operation : List.of("POST", "PUT", "PATCH")) {
            sql("DROP TABLE IF EXISTS tuple_replication_timestamps");
            service.saveTimestamps(database, List.of(timestamp("existing", START, null)));
            sql("""
                    CREATE TRIGGER reject_timestamp BEFORE INSERT ON tuple_replication_timestamps FOR EACH ROW
                    BEGIN
                        IF NEW.replication_id='fail' THEN
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected timestamp failure';
                        END IF;
                    END
                    """);
            final List<String> before = rows("");
            final var valid = timestamp("existing", operation.equals("PUT") ? NEXT : START, END);
            final var invalid = timestamp("fail", START, END);
            assertThrows(SQLException.class, () -> deliver(operation, valid, invalid), operation);
            assertEquals(before, rows(""), operation + " must roll back both closures and inserts");
        }
    }

    @Test
    void refusesUnknownSchemaWithoutRewritingSurvivingEvidence() throws Exception {
        sql(LEGACY_SCHEMA);
        sql("ALTER TABLE tuple_replication_timestamps DROP PRIMARY KEY, ADD PRIMARY KEY(site_url(255), replication_id)");
        sql("""
                INSERT INTO tuple_replication_timestamps VALUES
                ('https://origin.example', 'existing', 'source-db', 'source-table',
                 '2026-10-03 12:00:00.000001', NULL)
                """);
        final List<String> before = rows("");
        final SQLException error = assertThrows(SQLException.class,
                () -> service.saveTimestamps(database, List.of(timestamp("new", START, null))));
        assertTrue(error.getMessage().contains("Unrecognized timestamp primary key"));
        assertEquals(before, rows(""));
        assertEquals(List.of("site_url", "replication_id"), primaryKey());
    }

    @Test
    void refusesNontransactionalLegacyTableInsteadOfClaimingRollbackSafety() throws Exception {
        sql(LEGACY_SCHEMA.replace("ENGINE=InnoDB", "ENGINE=MyISAM").replace("utf8mb4", "latin1"));
        final SQLException error = assertThrows(SQLException.class,
                () -> service.saveTimestamps(database, List.of(timestamp("key", START, null))));
        assertTrue(error.getMessage().contains("must use InnoDB"));
        assertEquals(List.of("site_url", "replication_id", "row_start"), primaryKey());
        assertEquals(List.of(), rows(""));
    }

    @Test
    void concurrentFirstWritersMigrateLegacySchemaAndKeepEarliestEnd() throws Exception {
        sql(LEGACY_SCHEMA);
        final CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var later = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                service.updateTimestampRowEnds(database, List.of(timestamp("key", START, END)));
                return null;
            });
            final var earlier = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                service.updateTimestampRowEnds(database, List.of(timestamp("key", START, NEXT)));
                return null;
            });
            start.countDown();
            later.get(30, TimeUnit.SECONDS);
            earlier.get(30, TimeUnit.SECONDS);
        }
        assertEquals(NEXT, end("key", START));
        assertEquals(1, rows("").size());
    }

    private void deliver(String method, TupleReplicationTimestampDto... timestamps) throws SQLException {
        switch (method) {
            case "POST" -> service.saveTimestamps(database, List.of(timestamps));
            case "PUT" -> service.closeAndSaveTimestamps(database, List.of(timestamps));
            case "PATCH" -> service.updateTimestampRowEnds(database, List.of(timestamps));
            default -> throw new IllegalArgumentException(method);
        }
    }

    private TupleReplicationTimestampDto timestamp(String key, Instant start, Instant end) {
        return TupleReplicationTimestampDto.builder().siteUrl("https://origin.example").replicationId(key)
                .databaseId(DATABASE).tableId(TABLE).rowStart(start).rowEnd(end).build();
    }

    private Connection connection() throws SQLException {
        final Connection connection = DriverManager.getConnection(url + "/" + SCHEMA
                + "?connectTimeout=5000&socketTimeout=30000", "root", password);
        try (var statement = connection.createStatement()) {
            statement.execute("SET time_zone='+00:00'");
        }
        return connection;
    }

    private void sql(String statement) throws SQLException {
        try (var connection = connection(); var query = connection.createStatement()) {
            query.execute(statement);
        }
    }

    private List<String> rows(String predicate) throws SQLException {
        final List<String> rows = new ArrayList<>();
        try (var connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT site_url, replication_id, database_id, table_id, row_start, row_end "
                     + "FROM tuple_replication_timestamps " + predicate + " ORDER BY site_url, replication_id, database_id, table_id, row_start")) {
            while (result.next()) {
                final List<String> columns = new ArrayList<>();
                for (int i = 1; i <= 6; i++) {
                    columns.add(result.getString(i));
                }
                rows.add(columns.toString());
            }
        }
        return rows;
    }

    private List<String> primaryKey() throws SQLException {
        final List<String> columns = new ArrayList<>();
        try (var connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COLUMN_NAME FROM information_schema.STATISTICS "
                     + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tuple_replication_timestamps' "
                     + "AND INDEX_NAME='PRIMARY' ORDER BY SEQ_IN_INDEX")) {
            while (result.next()) {
                columns.add(result.getString(1));
            }
        }
        return columns;
    }

    private Instant end(String key, Instant start) throws SQLException {
        try (var connection = connection(); var statement = connection.prepareStatement("""
                SELECT row_end FROM tuple_replication_timestamps WHERE replication_id=? AND row_start=?
                """)) {
            statement.setString(1, key);
            statement.setTimestamp(2, java.sql.Timestamp.from(start));
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                final Instant end = result.getTimestamp(1).toInstant();
                assertFalse(result.next());
                return end;
            }
        }
    }
}
