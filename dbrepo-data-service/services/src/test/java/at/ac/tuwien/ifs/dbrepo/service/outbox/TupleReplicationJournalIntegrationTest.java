package at.ac.tuwien.ifs.dbrepo.service.outbox;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "JOURNAL_SQL_TEST_PORT", matches = "13366")
class TupleReplicationJournalIntegrationTest {
    private static final String URL = "jdbc:mariadb://127.0.0.1:13366/";
    private static final String SCHEMA = System.getenv().getOrDefault("JOURNAL_SQL_TEST_SCHEMA", "journal_replication_test");
    private static final String OUTBOX = "tuple_replication_notification_outbox";
    private static final String FIRST = "00000000-0000-0000-0000-000000000001";
    private static final String SECOND = "00000000-0000-0000-0000-000000000002";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final TupleReplicationOutboxServiceMariaDbImpl service = new TupleReplicationOutboxServiceMariaDbImpl(mapper);
    private Database database;
    private Table table;

    @BeforeEach
    void schema() throws Exception {
        assertTrue(List.of("journal_replication_test", "recovery_tuple_test").contains(SCHEMA));
        try (Connection c = DriverManager.getConnection(URL, "root", password())) {
            sql(c, "DROP DATABASE IF EXISTS " + SCHEMA);
            sql(c, "CREATE DATABASE " + SCHEMA);
        }
        database = Database.builder().id(UUID.randomUUID()).internalName(SCHEMA)
                .container(Container.builder().host("127.0.0.1").port(13366).username("root").password(password())
                        .image(Image.builder().jdbcMethod("mariadb").build()).build()).build();
        table = Table.builder().id(UUID.randomUUID()).internalName("samples").build();
        try (Connection c = connection()) {
            sql(c, "CREATE TABLE samples (id INT PRIMARY KEY, value INT) ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
    }

    @Test
    void counterBlocksLaterWriterAndPublishesMultirowTransactionAtomically() throws Exception {
        try (Connection first = prepared(); Connection reader = connection(); var executor = Executors.newSingleThreadExecutor()) {
            first.setAutoCommit(false);
            sql(first, "INSERT INTO samples VALUES (1,10),(2,20)");
            final var one = enqueue(first);
            final CountDownLatch attempting = new CountDownLatch(1);
            final CountDownLatch allocated = new CountDownLatch(1);
            final CountDownLatch finish = new CountDownLatch(1);
            final var later = executor.submit(() -> {
                try (Connection second = connection()) {
                    second.setAutoCommit(false);
                    sql(second, "INSERT INTO samples VALUES (3,30)");
                    attempting.countDown();
                    final var three = enqueue(second);
                    allocated.countDown();
                    assertTrue(finish.await(10, TimeUnit.SECONDS));
                    second.commit();
                    return three;
                }
            });
            try {
                assertTrue(attempting.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> later.get(250, TimeUnit.MILLISECONDS));
                final var two = enqueue(first);
                assertEquals(0, service.readJournalState(reader).committedThrough());
                assertTrue(service.readRange(reader, 0, 0, 10).isEmpty());
                assertEquals(0, count(reader, "samples"));
                first.commit();
                assertTrue(allocated.await(5, TimeUnit.SECONDS));
                assertEquals(2, service.readJournalState(reader).committedThrough());
                assertEquals(List.of(one.getId(), two.getId()), service.readRange(reader, 0, 2, 10)
                        .stream().map(TupleReplicationOutboxServiceMariaDbImpl.JournalEntry::eventId).toList());
                assertEquals(2, count(reader, "samples"));
                finish.countDown();
                final var three = later.get(5, TimeUnit.SECONDS);
                assertEquals(3, sequence(three));
                assertEquals(3, service.readJournalState(reader).committedThrough());
                assertEquals(List.of(1L, 2L, 3L), service.readRange(reader, 0, 3, 10)
                        .stream().map(TupleReplicationOutboxServiceMariaDbImpl.JournalEntry::sequence).toList());
            } finally {
                first.rollback();
                finish.countDown();
            }
        }
    }

    @Test
    void consistentSnapshotAndCatchupCannotLoseLateWriter() throws Exception {
        try (Connection writer = prepared(); Connection reader = connection()) {
            writer.setAutoCommit(false);
            sql(writer, "INSERT INTO samples VALUES (1,1)");
            enqueue(writer);
            sql(reader, "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ");
            sql(reader, "START TRANSACTION WITH CONSISTENT SNAPSHOT");
            final long boundary = service.readJournalState(reader).committedThrough();
            assertEquals(0, boundary);
            writer.commit();
            assertEquals(0, service.readJournalState(reader).committedThrough());
            assertEquals(0, count(reader, "samples"));
            reader.commit();
            final long through = service.readJournalState(reader).committedThrough();
            assertEquals(1, through);
            assertEquals(1, service.readRange(reader, boundary, through, 1).size());
        }
    }

    @Test
    void insertUpdateDeleteFailuresRollbackDataHistoryAndCounter() throws Exception {
        try (Connection c = prepared()) {
            sql(c, "INSERT INTO samples VALUES (1,1),(2,2)");
            sql(c, "CREATE TRIGGER reject_event BEFORE INSERT ON " + OUTBOX
                    + " FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected journal failure'");
            for (String mutation : List.of("INSERT INTO samples VALUES (3,3)",
                    "UPDATE samples SET value=5 WHERE id=1", "DELETE FROM samples WHERE id=2")) {
                c.setAutoCommit(false);
                sql(c, mutation);
                assertThrows(SQLException.class, () -> enqueue(c));
                c.rollback();
                c.setAutoCommit(true);
                assertEquals(2, count(c, "samples"));
                assertEquals(2, count(c, "samples FOR SYSTEM_TIME ALL"));
                assertEquals(0, count(c, OUTBOX));
                assertEquals(0, service.readJournalState(c).committedThrough());
            }
            sql(c, "DROP TRIGGER reject_event");
            c.setAutoCommit(false);
            assertEquals(1, sequence(enqueue(c)));
            c.commit();
        }
    }

    @Test
    void failureOfSecondEventRollsBackTheEntireMultirowTransaction() throws Exception {
        try (Connection c = prepared()) {
            sql(c, "CREATE TRIGGER reject_second BEFORE INSERT ON " + OUTBOX
                    + " FOR EACH ROW BEGIN IF NEW.event_sequence=2 THEN SIGNAL SQLSTATE '45000'"
                    + " SET MESSAGE_TEXT='injected second event failure'; END IF; END");
            c.setAutoCommit(false);
            sql(c, "INSERT INTO samples VALUES (1,1),(2,2)");
            assertEquals(1, sequence(enqueue(c)));
            assertThrows(SQLException.class, () -> enqueue(c));
            c.rollback();
            assertEquals(0, count(c, "samples FOR SYSTEM_TIME ALL"));
            assertEquals(0, count(c, OUTBOX));
            assertEquals(0, service.readJournalState(c).committedThrough());
            c.setAutoCommit(true);
            sql(c, "DROP TRIGGER reject_second");
            c.setAutoCommit(false);
            assertEquals(1, sequence(enqueue(c)));
            c.commit();
        }
    }

    @Test
    void serializationFailureAndDisconnectedWriterReleaseCounterWithoutGap() throws Exception {
        try (Connection c = prepared()) {
            final ObjectMapper broken = spy(mapper);
            doThrow(new JsonProcessingException("injected serialization failure") { }).when(broken)
                    .writeValueAsString(any(DataReplicationDto.class));
            final var failing = new TupleReplicationOutboxServiceMariaDbImpl(broken);
            c.setAutoCommit(false);
            sql(c, "INSERT INTO samples VALUES (1,1)");
            assertThrows(IllegalArgumentException.class, () -> failing.enqueue(c, database, table, HttpMethod.POST,
                    DataReplicationDto.builder().build()));
            c.rollback();
            assertEquals(0, service.readJournalState(c).committedThrough());
        }
        try (Connection abandoned = connection()) {
            abandoned.setAutoCommit(false);
            sql(abandoned, "INSERT INTO samples VALUES (2,2)");
            enqueue(abandoned);
        }
        try (Connection c = connection()) {
            assertEquals(0, count(c, "samples FOR SYSTEM_TIME ALL"));
            c.setAutoCommit(false);
            assertEquals(1, sequence(enqueue(c)));
            c.commit();
        }
    }

    @Test
    void schemaPreparationRefusesToCommitAnExistingDataTransaction() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            sql(c, "INSERT INTO samples VALUES (1,1)");
            assertThrows(SQLException.class, () -> service.ensureTableExists(c));
            assertThrows(SQLException.class, () -> enqueue(c));
            c.rollback();
            assertEquals(0, count(c, "samples FOR SYSTEM_TIME ALL"));
        }
    }

    @Test
    void legacyMigrationUsesOriginalIdsStableOrderAndExplicitCoverageFloor() throws Exception {
        try (Connection c = connection()) {
            legacySchema(c, false);
            legacy(c, SECOND, "{\"unknownField\":\"keep\"}", null);
            legacy(c, FIRST, "{}", null);
            service.ensureTableExists(c);
            final var entries = service.readRange(c, 0, 2, 10);
            assertEquals(List.of(UUID.fromString(FIRST), UUID.fromString(SECOND)), entries.stream()
                    .map(TupleReplicationOutboxServiceMariaDbImpl.JournalEntry::eventId).toList());
            assertEquals(2, service.readJournalState(c).legacyThrough());
            assertEquals("keep", mapper.readTree(entries.get(1).payloadJson()).get("unknownField").asText());
            service.ensureTableExists(c);
            assertEquals(entries, service.readRange(c, 0, 2, 10));
            c.setAutoCommit(false);
            assertEquals(3, sequence(enqueue(c)));
            c.commit();
            assertEquals(2, service.readJournalState(c).legacyThrough());
            final var claim = service.claim(database, UUID.fromString(FIRST), Duration.ofMinutes(5)).orElseThrow();
            assertTrue(service.markSucceeded(database, claim.getId(), claim.getClaimToken()));
            assertEquals(entries, service.readRange(c, 0, 2, 10));
        }
    }

    @Test
    void prototypeMigrationPreservesAssignedIdentityPayloadBytesAndSequenceGaps() throws Exception {
        try (Connection c = connection()) {
            legacySchema(c, true);
            final String payload = "{ \"eventId\": \"" + FIRST + "\", \"eventSequence\": 7 }";
            legacy(c, FIRST, payload, 7L);
            legacy(c, SECOND, "{\"eventId\":\"" + SECOND + "\",\"eventSequence\":20}", 20L);
            service.ensureTableExists(c);
            assertEquals(20, service.readJournalState(c).legacyThrough());
            assertEquals(payload, service.readRange(c, 0, 20, 1).getFirst().payloadJson());
            try (var columns = c.getMetaData().getColumns(SCHEMA, null, OUTBOX, "event_sequence")) {
                assertTrue(columns.next());
                assertEquals("NO", columns.getString("IS_AUTOINCREMENT"));
            }
            c.setAutoCommit(false);
            assertEquals(21, sequence(enqueue(c)));
            c.commit();
        }
    }

    @Test
    void corruptOrConflictingLegacyMigrationIsAtomicAndRetryable() throws Exception {
        try (Connection c = connection()) {
            legacySchema(c, false);
            legacy(c, FIRST, "{}", null);
            legacy(c, SECOND, "broken JSON", null);
            assertThrows(SQLException.class, () -> service.ensureTableExists(c));
            assertTrue(c.getAutoCommit());
            assertEquals(2, count(c, OUTBOX + " WHERE event_sequence IS NULL"));
            assertEquals(1, count(c, OUTBOX + " WHERE id='" + FIRST + "' AND payload='{}'"));
            assertThrows(SQLException.class, () -> service.readJournalState(c));
            sql(c, "UPDATE " + OUTBOX + " SET payload='{\"eventId\":\"wrong\"}' WHERE id='" + SECOND + "'");
            assertThrows(SQLException.class, () -> service.ensureTableExists(c));
            assertEquals(2, count(c, OUTBOX + " WHERE event_sequence IS NULL"));
            sql(c, "UPDATE " + OUTBOX + " SET payload='{}' WHERE id='" + SECOND + "'");
            service.ensureTableExists(c);
            assertEquals(2, service.readJournalState(c).legacyThrough());
        }
    }

    @Test
    void deliveryStatusChangesNeverChangeJournalAndPaginationUsesSequence() throws Exception {
        try (Connection c = prepared()) {
            c.setAutoCommit(false);
            final var one = enqueue(c);
            final var two = enqueue(c);
            c.commit();
            c.setAutoCommit(true);
            final var before = service.readRange(c, 0, 2, 10);
            final var first = service.claim(database, one.getId(), Duration.ofMinutes(5)).orElseThrow();
            final var second = service.claim(database, two.getId(), Duration.ofMinutes(5)).orElseThrow();
            assertTrue(service.markSucceeded(database, one.getId(), first.getClaimToken()));
            assertTrue(service.markFailed(database, two.getId(), second.getClaimToken(), "offline", Duration.ZERO, 1, false));
            assertEquals(List.of(two.getId()), service.findAll(database).stream()
                    .map(TupleReplicationOutboxEntry::getId).toList());
            assertEquals(before, service.readRange(c, 0, 2, 10));
            assertEquals(before.getFirst(), service.readRange(c, 0, 2, 1).getFirst());
            assertEquals(before.getLast(), service.readRange(c, 1, 2, 1).getFirst());
            assertThrows(SQLException.class, () -> service.readRange(c, 0, 3, 1));
            assertThrows(IllegalArgumentException.class, () -> service.readRange(c, -1, 2, 1));
        }
    }

    @Test
    void missingNewJournalEntriesAndDirtyReadsFailClosed() throws Exception {
        try (Connection c = prepared()) {
            c.setAutoCommit(false);
            enqueue(c);
            enqueue(c);
            enqueue(c);
            c.commit();
            c.setAutoCommit(true);
            sql(c, "DELETE FROM " + OUTBOX + " WHERE event_sequence=2");
            assertThrows(SQLException.class, () -> service.readRange(c, 0, 3, 10));
            assertThrows(SQLException.class, () -> service.readRange(c, 1, 2, 10));
            c.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            assertThrows(SQLException.class, () -> service.readJournalState(c));
        }
    }

    @Test
    void nontransactionalLegacyOutboxCannotBeMarkedReady() throws Exception {
        try (Connection c = connection()) {
            legacySchema(c, false);
            sql(c, "ALTER TABLE " + OUTBOX + " ENGINE=MyISAM");
            legacy(c, FIRST, "{}", null);
            final var error = assertThrows(SQLException.class, () -> service.ensureTableExists(c));
            assertTrue(error.getMessage().contains("InnoDB"));
            assertThrows(SQLException.class, () -> service.readJournalState(c));
            assertEquals(1, count(c, OUTBOX + " WHERE event_sequence IS NULL AND payload='{}'"));
        }
    }

    private Connection prepared() throws Exception {
        final Connection c = connection();
        service.ensureTableExists(c);
        return c;
    }

    private Connection connection() throws SQLException {
        final Connection c = DriverManager.getConnection(URL + SCHEMA, "root", password());
        sql(c, "SET SESSION innodb_lock_wait_timeout=5");
        return c;
    }

    private String password() { return System.getenv("JOURNAL_SQL_TEST_PASSWORD"); }

    private TupleReplicationOutboxEntry enqueue(Connection c) throws SQLException {
        return service.enqueue(c, database, table, HttpMethod.POST, DataReplicationDto.builder().build());
    }

    private long sequence(TupleReplicationOutboxEntry entry) throws Exception {
        final var payload = mapper.readTree(entry.getPayloadJson());
        assertEquals(entry.getId().toString(), payload.get("eventId").asText());
        return payload.get("eventSequence").asLong();
    }

    private void legacySchema(Connection c, boolean prototype) throws SQLException {
        sql(c, "CREATE TABLE " + OUTBOX + " (id VARCHAR(36) PRIMARY KEY, "
                + (prototype ? "event_sequence BIGINT NOT NULL AUTO_INCREMENT UNIQUE, " : "")
                + "database_id VARCHAR(36) NOT NULL, table_id VARCHAR(36) NOT NULL, http_method VARCHAR(16) NOT NULL,"
                + " payload LONGTEXT NOT NULL, status VARCHAR(32) NOT NULL, attempts INT NOT NULL DEFAULT 0,"
                + " last_error TEXT, created TIMESTAMP(6) NOT NULL, last_modified TIMESTAMP(6), next_attempt_at TIMESTAMP(6)) ENGINE=InnoDB");
    }

    private void legacy(Connection c, String id, String payload, Long sequence) throws SQLException {
        try (var statement = c.prepareStatement("INSERT INTO " + OUTBOX
                + " (id,database_id,table_id,http_method,payload,status,created"
                + (sequence == null ? "" : ",event_sequence") + ") VALUES (?,?,?,?,?,'PENDING','2026-01-01'"
                + (sequence == null ? "" : ",?") + ")")) {
            statement.setString(1, id);
            statement.setString(2, database.getId().toString());
            statement.setString(3, table.getId().toString());
            statement.setString(4, "POST");
            statement.setString(5, payload);
            if (sequence != null) statement.setLong(6, sequence);
            statement.executeUpdate();
        }
    }

    private static void sql(Connection c, String sql) throws SQLException {
        try (var statement = c.createStatement()) { statement.execute(sql); }
    }

    private static long count(Connection c, String relation) throws SQLException {
        try (var statement = c.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + relation)) {
            assertTrue(rows.next());
            return rows.getLong(1);
        }
    }
}
