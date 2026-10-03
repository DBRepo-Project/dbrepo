package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
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
    private final ComboPooledDataSource pool = mock(ComboPooledDataSource.class);
    private String url;
    private String password;
    private String serviceTimeZone = "+00:00";

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
        when(pool.getConnection()).thenAnswer(invocation -> {
            final Connection connection = connection();
            try (var statement = connection.createStatement()) {
                statement.execute("SET time_zone='" + serviceTimeZone + "'");
            }
            return connection;
        });
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
        assertFullOriginSchema();
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

    @Test
    void migratesFiveColumnPrefixKeyAndDistinguishesLongOriginsWithIdenticalPrefixes() throws Exception {
        sql(LEGACY_SCHEMA.replace("replication_id, row_start", "replication_id, database_id, table_id, row_start"));
        // Four valid DNS labels totaling 253 characters; both origins share their first 255 characters.
        final String common = "https://" + ("a".repeat(63) + ".").repeat(3) + "b".repeat(60);
        final String first = common + "c";
        final String second = common + "d";
        assertEquals(261, first.length());
        assertEquals(first.substring(0, 255), second.substring(0, 255));
        insertLegacy(first, "long-origin");
        final List<String> before = rows("");
        service.saveTimestamps(database, List.of(timestamp("probe", START, null)));
        assertEquals(before, rows("WHERE replication_id='long-origin'"));
        assertFullOriginSchema();

        final var a = timestamp("long-origin", START, null);
        a.setSiteUrl(first);
        final var b = timestamp("long-origin", START, NEXT);
        b.setSiteUrl(second);
        service.saveTimestamps(database, List.of(a, b));
        assertEquals(2, rows("WHERE replication_id='long-origin'").size());
        assertTrue(rows("").contains(before.getFirst()), "The legacy origin and closed interval must remain unchanged");
        final List<String> after = rows("");
        service.saveTimestamps(database, List.of(b, a));
        assertEquals(after, rows(""));
    }

    @Test
    void invalidLegacyOriginsLeaveBothRecognizedSchemasAndEvidenceUntouched() throws Exception {
        for (boolean fiveColumns : List.of(false, true)) {
            for (String site : List.of("https://" + "a".repeat(505), "https://\u00e9.example",
                    "https://origin.example/path", "https://ORIGIN.example", "https://origin.example/",
                    "https://origin.example:443", "https://" + "a".repeat(64) + ".example")) {
                sql("DROP TABLE IF EXISTS tuple_replication_timestamps");
                sql(fiveColumns ? LEGACY_SCHEMA.replace("replication_id, row_start",
                        "replication_id, database_id, table_id, row_start") : LEGACY_SCHEMA);
                insertLegacy(site, "invalid-origin");
                final List<String> before = rows("");
                final String schema = schema();
                final SQLException failure = assertThrows(SQLException.class,
                        () -> service.saveTimestamps(database, List.of(timestamp("probe", START, null))));
                assertTrue(failure.getMessage().contains("canonical ASCII replication origin"));
                assertEquals(before, rows(""));
                assertEquals(schema, schema(), "Validation must fail before ALTER, without normalizing or truncating");
            }
        }
    }

    @Test
    void fullKeyWithUnexpectedOriginColumnFailsClosed() throws Exception {
        sql(LEGACY_SCHEMA.replace("site_url TEXT", "site_url VARCHAR(512) CHARACTER SET ascii COLLATE ascii_general_ci")
                .replace("site_url(255), replication_id, row_start", "site_url, replication_id, database_id, table_id, row_start"));
        insertLegacy("https://origin.example", "legacy");
        final List<String> before = rows("");
        final String schema = schema();
        final SQLException failure = assertThrows(SQLException.class,
                () -> service.saveTimestamps(database, List.of(timestamp("probe", START, null))));
        assertTrue(failure.getMessage().contains("Unrecognized timestamp origin column"));
        assertEquals(before, rows(""));
        assertEquals(schema, schema());
    }

    @Test
    void migrationLocksOutLegacyWritersBetweenValidationAndAlter() throws Exception {
        sql(LEGACY_SCHEMA);
        insertLegacy("https://origin.example", "legacy");
        final List<String> before = rows("");
        try (var connection = connection()) {
            final Connection migrating = spy(connection);
            doAnswer(invocation -> {
                // An old writer must not sneak a truncating or noncanonical value past the preflight scan.
                try (var writer = connection(); var statement = writer.createStatement()) {
                    statement.execute("SET lock_wait_timeout=1");
                    final SQLException failure = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                            INSERT INTO tuple_replication_timestamps VALUES
                            ('https://invalid.example/path', 'racing-writer', 'source-db', 'source-table',
                             '2026-10-03 12:00:00.000001', NULL)
                            """));
                    assertEquals(1205, failure.getErrorCode(), "The writer must time out on the migration's table lock");
                }
                return connection.prepareStatement(invocation.getArgument(0));
            }).when(migrating).prepareStatement(startsWith("ALTER TABLE"));
            when(pool.getConnection()).thenReturn(migrating);
            service.saveTimestamps(database, List.of(timestamp("probe", START, null)));
        }
        assertEquals(before, rows("WHERE replication_id='legacy'"));
        assertEquals(2, rows("").size());
        assertFullOriginSchema();
    }

    @Test
    void migrationAndAllOperationsPreserveInstantsAcrossNonUtcJvmAndSessionTimeZones() throws Exception {
        sql(LEGACY_SCHEMA);
        insertLegacy("https://origin.example", "legacy");
        final List<String> before = rows("");
        final TimeZone original = TimeZone.getDefault();
        try {
            serviceTimeZone = "-07:00";
            for (String zone : List.of("Asia/Kathmandu", "Pacific/Honolulu")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                service.saveTimestamps(database, List.of(timestamp("zone", START, null)));
                service.closeAndSaveTimestamps(database, List.of(timestamp("zone", NEXT, null)));
                service.updateTimestampRowEnds(database, List.of(timestamp("zone", NEXT, END)));
                service.saveTimestamps(database, List.of(timestamp("zone", START, null), timestamp("zone", NEXT, null)));
                assertEquals(NEXT, end("zone", START));
                assertEquals(END, end("zone", NEXT));
                assertEquals(before, rows("WHERE replication_id='legacy'"));
                try (var connection = connection(); var statement = connection.createStatement();
                     var result = statement.executeQuery("""
                             SELECT row_start, row_end, UNIX_TIMESTAMP(row_start), UNIX_TIMESTAMP(row_end)
                             FROM tuple_replication_timestamps WHERE replication_id='zone' ORDER BY row_start
                             """)) {
                    for (Instant start : List.of(START, NEXT)) {
                        assertTrue(result.next());
                        final Instant end = start.equals(START) ? NEXT : END;
                        final Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        assertEquals(start, result.getTimestamp(1, utc).toInstant());
                        assertEquals(end, result.getTimestamp(2, utc).toInstant());
                        assertEquals(0, epoch(start).compareTo(result.getBigDecimal(3)));
                        assertEquals(0, epoch(end).compareTo(result.getBigDecimal(4)));
                    }
                    assertFalse(result.next());
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private BigDecimal epoch(Instant instant) {
        return BigDecimal.valueOf(instant.getEpochSecond()).add(BigDecimal.valueOf(instant.getNano(), 9));
    }

    private void insertLegacy(String site, String key) throws SQLException {
        try (var connection = connection(); var statement = connection.prepareStatement("""
                INSERT INTO tuple_replication_timestamps VALUES
                    (?, ?, ?, ?, '2026-10-03 12:00:00.000001', '2026-10-03 12:00:02.000001')
                """)) {
            statement.setString(1, site);
            statement.setString(2, key);
            statement.setString(3, DATABASE.toString());
            statement.setString(4, TABLE.toString());
            statement.executeUpdate();
        }
    }

    private String schema() throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SHOW CREATE TABLE tuple_replication_timestamps")) {
            assertTrue(result.next());
            return result.getString(2);
        }
    }

    private void assertFullOriginSchema() throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT c.COLUMN_TYPE, c.CHARACTER_SET_NAME, c.COLLATION_NAME, s.SUB_PART
                     FROM information_schema.COLUMNS c JOIN information_schema.STATISTICS s
                         ON s.TABLE_SCHEMA=c.TABLE_SCHEMA AND s.TABLE_NAME=c.TABLE_NAME AND s.COLUMN_NAME=c.COLUMN_NAME
                     WHERE c.TABLE_SCHEMA=DATABASE() AND c.TABLE_NAME='tuple_replication_timestamps'
                         AND c.COLUMN_NAME='site_url' AND s.INDEX_NAME='PRIMARY'
                     """)) {
            assertTrue(result.next());
            assertEquals("varchar(512)", result.getString(1));
            assertEquals("ascii", result.getString(2));
            assertEquals("ascii_bin", result.getString(3));
            assertNull(result.getString(4), "The entire origin must be indexed");
            assertFalse(result.next());
        }
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
            final Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            statement.setTimestamp(2, java.sql.Timestamp.from(start), utc);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                final Instant end = result.getTimestamp(1, utc).toInstant();
                assertFalse(result.next());
                return end;
            }
        }
    }
}
