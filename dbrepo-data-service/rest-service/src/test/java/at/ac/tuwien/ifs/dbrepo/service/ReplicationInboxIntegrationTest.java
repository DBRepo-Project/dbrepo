package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.JacksonConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationInboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "REPLICA_SQL_TEST_PORT", matches = "[0-9]+")
class ReplicationInboxIntegrationTest {
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("REPLICA_SQL_TEST_PORT");
    private final String password = System.getenv("REPLICA_SQL_TEST_PASSWORD");
    private final com.fasterxml.jackson.databind.ObjectMapper json = new JacksonConfig().objectMapper();
    private final ReplicationInboxService receiver = new ReplicationInboxService(Mappers.getMapper(MariaDbMapper.class),
            json, "https://replica.example");
    private Database database;
    private Table table;
    private UUID sourceDatabase;
    private UUID sourceTable;

    @BeforeEach
    void setup() throws Exception {
        sourceDatabase = UUID.randomUUID();
        sourceTable = UUID.randomUUID();
        database = Database.builder().id(UUID.randomUUID()).internalName("replication_inbox_test")
                .creationLocation("https://origin.example")
                .container(Container.builder().host("127.0.0.1")
                        .port(Integer.valueOf(System.getenv("REPLICA_SQL_TEST_PORT")))
                        .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build())
                .build();
        table = Table.builder().id(UUID.randomUUID()).internalName("samples")
                .creationLocation("https://origin.example")
                .columns(List.of(Column.builder().internalName("replication_key").columnType(ColumnType.VARCHAR).build(),
                        Column.builder().internalName("sample_value").columnType(ColumnType.DECIMAL).build(),
                        Column.builder().internalName("data_blob").columnType(ColumnType.BLOB).build())).build();
        try (Connection root = DriverManager.getConnection(url, "root", password)) {
            root.createStatement().execute("DROP DATABASE IF EXISTS replication_inbox_test");
            root.createStatement().execute("CREATE DATABASE replication_inbox_test");
            root.createStatement().execute("CREATE TABLE replication_inbox_test.samples "
                    + "(replication_key VARCHAR(36) PRIMARY KEY, sample_value DECIMAL(38,16), data_blob LONGBLOB) "
                    + "WITH SYSTEM VERSIONING");
        }
    }

    @Test
    void lostAcknowledgementReturnsOriginalReceiptWithoutNewVersion() throws Exception {
        final var insert = event(1, "a", "1");
        final var first = apply(insert, HttpMethod.POST);
        apply(event(2, "a", "2"), HttpMethod.PUT);
        insert.getDatabase().setReplicaUrls(Map.of("https://another.example", UUID.randomUUID(),
                "https://replica.example", database.getId()));
        final var duplicate = apply(insert, HttpMethod.POST);
        assertEquals(json.valueToTree(first), json.valueToTree(duplicate));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(2, count("tuple_replication_inbox"));
        assertEquals(1, count("samples WHERE sample_value = 2"));
    }

    @Test
    void nonTransactionalReceiptTableIsRejectedBeforeMutation() throws Exception {
        apply(event(1, "a", "1"), HttpMethod.POST);
        try (Connection root = connection()) {
            root.createStatement().execute("ALTER TABLE tuple_replication_inbox ENGINE=MyISAM");
        }
        assertThrows(SQLException.class, () -> apply(event(2, "a", "2"), HttpMethod.PUT));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("tuple_replication_inbox"));
    }

    @Test
    void reversedDeliveryKeepsNewestVersionAndRetainsOlderPayload() throws Exception {
        apply(event(3, "a", "3"), HttpMethod.PUT);
        final var stale = apply(event(1, "a", "1"), HttpMethod.POST);
        assertFalse(stale.getApplied());
        assertNull(stale.getInsertedAt());
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("samples WHERE sample_value = 3"));
        assertEquals(2, count("tuple_replication_inbox"));
    }

    @Test
    void deleteBeforeInsertCreatesDurableTombstone() throws Exception {
        final var deletion = event(3, "a", "2");
        assertFalse(apply(deletion, HttpMethod.DELETE).getApplied());
        assertFalse(apply(event(1, "a", "1"), HttpMethod.POST).getApplied());
        assertFalse(apply(event(2, "a", "2"), HttpMethod.PUT).getApplied());
        assertFalse(apply(deletion, HttpMethod.DELETE).getApplied());
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(3, count("tuple_replication_inbox"));
        apply(event(4, "a", "4"), HttpMethod.POST);
        assertEquals(1, count("samples WHERE sample_value = 4"));
    }

    @Test
    void snapshotBoundaryAlsoFencesLateEventsForPreviouslyUnknownKeys() throws Exception {
        apply(event(1, "a", "1"), HttpMethod.POST);
        try (var connection = connection()) {
            connection.createStatement().execute("UPDATE tuple_replication_table_heads SET event_sequence=5");
        }
        assertFalse(apply(event(2, "absent-from-baseline", "2"), HttpMethod.POST).getApplied());
        assertFalse(apply(event(5, "another-absent-key", "5"), HttpMethod.PUT).getApplied());
        assertEquals(1, count("samples"));
        assertEquals(3, count("tuple_replication_inbox"));
        assertTrue(apply(event(6, "absent-from-baseline", "6"), HttpMethod.POST).getApplied());
        assertEquals(2, count("samples"));
    }

    @Test
    void deleteReceiptRetainsOriginalMicrosecondIntervalAcrossRetries() throws Exception {
        apply(event(1, "a", "1"), HttpMethod.POST);
        apply(event(2, "a", "2"), HttpMethod.PUT);
        final var deletion = event(3, "a", "2");
        final var receipt = apply(deletion, HttpMethod.DELETE);
        assertTrue(receipt.getApplied());
        assertNotNull(receipt.getDeletedAt());
        assertTrue(receipt.getDeletedAt().isAfter(receipt.getInsertedAt()));
        assertEquals(json.valueToTree(receipt), json.valueToTree(apply(deletion, HttpMethod.DELETE)));
        assertEquals(0, count("samples"));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
    }

    @Test
    void failedReceiptWriteRollsBackMutationAndOrderingFence() throws Exception {
        apply(event(1, "a", "1"), HttpMethod.POST);
        try (Connection connection = connection()) {
            connection.createStatement().execute("CREATE TRIGGER fail_receipt BEFORE INSERT ON tuple_replication_inbox "
                    + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'injected receipt failure'");
        }
        final var update = event(2, "a", "2");
        assertThrows(SQLException.class, () -> apply(update, HttpMethod.PUT));
        assertEquals(1, count("samples WHERE sample_value = 1"));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("tuple_replication_heads WHERE event_sequence = 1"));
        try (Connection connection = connection()) {
            connection.createStatement().execute("DROP TRIGGER fail_receipt");
        }
        assertTrue(apply(update, HttpMethod.PUT).getApplied());
    }

    @Test
    void identityReuseWithDifferentContentIsRejected() throws Exception {
        final var insert = event(1, "a", "1");
        apply(insert, HttpMethod.POST);
        insert.getTuple().getData().put("sample_value", new BigDecimal("2"));
        assertThrows(TableMalformedException.class, () -> apply(insert, HttpMethod.POST));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("samples WHERE sample_value = 1"));
    }

    @Test
    void differentEventWithSameSourceSequenceIsRejected() throws Exception {
        apply(event(1, "a", "1"), HttpMethod.POST);
        assertThrows(TableMalformedException.class, () -> apply(event(1, "a", "2"), HttpMethod.PUT));
        assertThrows(SQLException.class, () -> apply(event(1, "b", "2"), HttpMethod.POST));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("tuple_replication_heads"));
    }

    @Test
    void concurrentDuplicateRequestsCommitOnlyOnce() throws Exception {
        final var event = event(1, "a", "1");
        final var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> { start.await(); return apply(event, HttpMethod.POST); });
            final var second = executor.submit(() -> { start.await(); return apply(event, HttpMethod.POST); });
            start.countDown();
            assertEquals(json.valueToTree(first.get(30, TimeUnit.SECONDS)), json.valueToTree(second.get(30, TimeUnit.SECONDS)));
        }
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("tuple_replication_inbox"));
    }

    @Test
    void decimalAndBinarySurviveWireRoundTripWithoutStorageLookup() throws Exception {
        final var event = event(1, "a", "1234567890123456789012.1234567890123456");
        event.getTuple().getData().put("data_blob", new byte[]{0, 1, -1, 0, 23});
        final var wire = json.readValue(json.writeValueAsString(event), DataReplicationDto.class);
        apply(wire, HttpMethod.POST);
        try (var connection = connection(); var row = connection.createStatement().executeQuery("SELECT * FROM samples")) {
            assertTrue(row.next());
            assertEquals(event.getTuple().getData().get("sample_value"), row.getBigDecimal("sample_value"));
            assertArrayEquals((byte[]) event.getTuple().getData().get("data_blob"), row.getBytes("data_blob"));
        }
    }

    @Test
    void mismatchedTargetAndChangedOriginGenerationAreRejected() throws Exception {
        final var first = event(1, "a", "1");
        first.getTable().setReplicaUrls(Map.of("https://replica.example", UUID.randomUUID()));
        assertThrows(TableMalformedException.class, () -> apply(first, HttpMethod.POST));
        apply(event(1, "a", "1"), HttpMethod.POST);
        final var differentOrigin = event(2, "a", "2");
        differentOrigin.getTable().setId(UUID.randomUUID());
        assertThrows(TableMalformedException.class, () -> apply(differentOrigin, HttpMethod.PUT));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
    }

    @Test
    void legacyAndWrongOriginWritesFailClosed() throws Exception {
        final var event = event(1, "a", "1");
        event.setEventId(null);
        assertThrows(TableMalformedException.class, () -> apply(event, HttpMethod.POST));
        event.setEventId(UUID.randomUUID());
        event.getDatabase().setCreationLocation("https://unrelated.example");
        assertThrows(TableMalformedException.class, () -> apply(event, HttpMethod.POST));
        event.getDatabase().setCreationLocation("https://origin.example");
        database.setCreationLocation("https://replica.example");
        table.setCreationLocation("https://replica.example");
        assertThrows(TableMalformedException.class, () -> apply(event, HttpMethod.POST));
        assertEquals(0, count("samples"));
    }

    private DataReplicationDto event(long sequence, String key, String value) {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("replication_key", key);
        data.put("sample_value", new BigDecimal(value));
        data.put("data_blob", null);
        return DataReplicationDto.builder().eventId(UUID.randomUUID()).eventSequence(sequence)
                .database(DatabaseDto.builder().id(sourceDatabase).creationLocation("https://origin.example")
                        .replicaUrls(Map.of("https://replica.example", database.getId())).build())
                .table(TableDto.builder().id(sourceTable).creationLocation("https://origin.example")
                        .replicaUrls(Map.of("https://replica.example", table.getId())).build())
                .tuple(TupleWithTimestampsDto.builder().replicationKey(key).data(data)
                        .insertedAt(Instant.parse("2026-09-01T10:00:00.123456Z")).build()).build();
    }

    private TupleWithTimestampsDto apply(DataReplicationDto event, HttpMethod method) throws Exception {
        return receiver.apply(database, table, event, method);
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(url + "/replication_inbox_test", "root", password);
    }

    private long count(String relation) throws SQLException {
        try (var connection = connection(); var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM " + relation)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }
}
