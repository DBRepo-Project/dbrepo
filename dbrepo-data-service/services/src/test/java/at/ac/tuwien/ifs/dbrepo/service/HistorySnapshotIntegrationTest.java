package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotCodec.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "HISTORY_SNAPSHOT_SQL_TEST_PORT", matches = "13366")
class HistorySnapshotIntegrationTest {
    private static final String SCHEMA = "history_snapshot_test";
    private static final String URL = "jdbc:mariadb://127.0.0.1:13366/";
    private static final String ORIGIN = "https://source.example";
    private final TupleReplicationOutboxServiceMariaDbImpl journal = spy(
            new TupleReplicationOutboxServiceMariaDbImpl(new ObjectMapper().findAndRegisterModules()));
    private final Database database = Database.builder().id(UUID.randomUUID()).internalName(SCHEMA).creationLocation(ORIGIN).build();
    private final Table sourceTable = Table.builder().id(UUID.randomUUID()).internalName("samples").creationLocation(ORIGIN).build();
    private final Table targetTable = Table.builder().id(UUID.randomUUID()).internalName("replica").creationLocation(ORIGIN)
            .replicaUrls(Map.of(ORIGIN, sourceTable.getId())).build();
    private final Database target = Database.builder().id(UUID.randomUUID()).internalName(SCHEMA).creationLocation(ORIGIN)
            .replicaUrls(Map.of(ORIGIN, database.getId())).build();
    private final HistorySnapshotService source = service(ORIGIN);
    private final HistorySnapshotService receiver = service("https://target.example");

    @BeforeEach
    void schema() throws Exception {
        try (Connection c = DriverManager.getConnection(URL, "root", password())) {
            sql(c, "DROP DATABASE IF EXISTS " + SCHEMA);
            sql(c, "CREATE DATABASE " + SCHEMA + " CHARACTER SET utf8mb4 COLLATE utf8mb4_bin");
        }
        try (Connection c = connection()) {
            sql(c, "CREATE TABLE samples (replication_key VARCHAR(36) PRIMARY KEY, value DECIMAL(30,10),"
                    + " unsigned_value BIGINT UNSIGNED, binary_value VARBINARY(32), label VARCHAR(100),"
                    + " measured_at DATETIME(6), elapsed TIME(6)) ENGINE=InnoDB WITH SYSTEM VERSIONING");
            sql(c, "CREATE TABLE replica LIKE samples");
            sql(c, "SET timestamp=1700000000.123456");
            sql(c, "INSERT INTO samples VALUES ('a',1.2345678901,18446744073709551615,X'00FF61','first',"
                    + "'2025-01-02 03:04:05.123456','25:01:02.345678'),('deleted',NULL,NULL,NULL,NULL,NULL,NULL)");
            sql(c, "SET timestamp=1700000010.654321");
            sql(c, "UPDATE samples SET value=9.8765432109,label='updated' WHERE replication_key='a'");
            sql(c, "DELETE FROM samples WHERE replication_key='deleted'");
            sql(c, "SET timestamp=1720000000.111111");
            sql(c, "INSERT INTO replica(replication_key,label) VALUES ('a','local-before')");
            sql(c, "SET timestamp=1720000010.222222");
            sql(c, "UPDATE replica SET label='local-after' WHERE replication_key='a'");
            journal.ensureTableExists(c);
        }
    }

    @Test
    void immutableHistoryRoundtripPreservesTypedPayloadMicrosecondsAndNativeTargetHistory() throws Exception {
        final var artifact = export(null);
        assertEquals(3, artifact.envelope().manifest().rows());
        assertEquals(1, artifact.envelope().manifest().currentKeys());
        final List<Row> rows = artifact.chunks().stream().flatMap(chunk -> uncheckedRows(chunk, artifact.envelope().manifest().columns()).stream()).toList();
        final Row original = rows.stream().filter(row -> "a".equals(row.replicationKey()) && !row.current()).findFirst().orElseThrow();
        assertTrue(original.rowStart().endsWith(".123456"));
        assertTrue(original.rowEnd().endsWith(".654321"));
        final var data = data(artifact.envelope().manifest(), original);
        assertEquals("1.2345678901", data.get("value"));
        assertEquals("18446744073709551615", data.get("unsigned_value"));
        assertArrayEquals(new byte[]{0, (byte) 255, 97}, (byte[]) data.get("binary_value"));
        assertEquals("2025-01-02 03:04:05.123456", data.get("measured_at"));
        assertEquals("25:01:02.345678", data.get("elapsed"));
        final String nativeBefore = nativeTarget();
        releaseSourceArtifact(artifact);
        assertEquals("STAGING", receiver.beginImport(target, targetTable, new Import(targetTable.getId(), artifact.envelope())).status());
        for (Chunk chunk : artifact.chunks()) {
            receiver.putChunk(target, chunk.snapshotId(), chunk);
            receiver.putChunk(target, chunk.snapshotId(), chunk);
        }
        final Receipt verified = receiver.verifyImport(target, targetTable, artifact.id());
        assertEquals("VERIFIED", verified.status());
        assertTrue(verified.historyVerified());
        assertFalse(verified.currentReconciled());
        assertEquals(nativeBefore, nativeTarget());
        try (Connection c = connection()) {
            final var keys = receiver.readCurrentKeys(c, artifact.id(), null, 10);
            assertEquals(List.of("a"), keys.stream().map(CurrentKey::replicationKey).toList());
            assertTrue(receiver.readCurrentKeys(c, artifact.id(), "a", 10).isEmpty());
            assertTrue(receiver.containsCurrentKey(c, artifact.id(), "a"));
            assertFalse(receiver.containsCurrentKey(c, artifact.id(), "deleted"));
            assertFalse(receiver.containsCurrentKey(c, UUID.randomUUID(), "a"));
        }
        assertThrows(SQLException.class, () -> receiver.reconcileImport(target, targetTable, artifact.id(), (c, db, t, envelope) -> {
            assertEquals(artifact.envelope(), envelope);
            sql(c, "UPDATE replica SET label='must-rollback' WHERE replication_key='a'");
            throw new SQLException("injected reconciliation failure");
        }));
        assertFalse(receiver.status(target, artifact.id()).currentReconciled());
        assertEquals(nativeBefore, nativeTarget());
    }

    @Test
    void concurrentCommitAfterSnapshotBoundaryDoesNotLeakIntoHistoryOrCurrentKeys() throws Exception {
        final CountDownLatch captured = new CountDownLatch(1);
        final CountDownLatch written = new CountDownLatch(1);
        doAnswer(invocation -> {
            final var state = invocation.callRealMethod();
            captured.countDown();
            assertTrue(written.await(10, TimeUnit.SECONDS));
            return state;
        }).when(journal).readJournalState(any(Connection.class));
        try (var executor = Executors.newSingleThreadExecutor()) {
            final var future = executor.submit(() -> export(null));
            try {
                assertTrue(captured.await(10, TimeUnit.SECONDS));
                try (Connection c = connection()) {
                    c.setAutoCommit(false);
                    sql(c, "UPDATE samples SET label='after-cut' WHERE replication_key='a'");
                    sql(c, "INSERT INTO samples(replication_key,label) VALUES ('later','after-cut')");
                    journal.enqueue(c, database, sourceTable, HttpMethod.PUT, DataReplicationDto.builder().build());
                    c.commit();
                }
            } finally { written.countDown(); }
            final Artifact artifact = future.get(15, TimeUnit.SECONDS);
            assertEquals(0, artifact.envelope().manifest().boundary());
            assertEquals(3, artifact.envelope().manifest().rows());
            assertEquals(1, artifact.envelope().manifest().currentKeys());
            final String payload = new String(artifact.chunks().getFirst().payload(), java.nio.charset.StandardCharsets.UTF_8);
            assertFalse(payload.contains("after-cut"));
            try (Connection c = connection()) { assertEquals(1, journal.readRange(c, 0, 1, 10).size()); }
        }
    }

    @Test
    void chunkResumeAndMissingOrAlteredContentNeverPublishesCoverage() throws Exception {
        try (Connection c = connection(); PreparedStatement statement = c.prepareStatement(
                "INSERT INTO samples(replication_key,label) VALUES (?,?)")) {
            for (int i = 0; i < 300; i++) {
                statement.setString(1, "key-" + String.format("%04d", i)); statement.setString(2, "data"); statement.addBatch();
            }
            statement.executeBatch();
        }
        final Artifact artifact = export(null);
        assertEquals(2, artifact.chunks().size());
        final var second = source.readChunk(database, artifact.id(), 1);
        assertEquals(artifact.chunks().get(1).sha256(), second.sha256());
        final var same = source.create(database, sourceTable, new Create(artifact.id(), sourceTable.getId(), null));
        assertEquals(artifact.envelope(), same);
        releaseSourceArtifact(artifact);
        receiver.beginImport(target, targetTable, new Import(targetTable.getId(), artifact.envelope()));
        receiver.putChunk(target, artifact.id(), second);
        assertThrows(ResponseStatusException.class, () -> receiver.verifyImport(target, targetTable, artifact.id()));
        assertFalse(receiver.status(target, artifact.id()).historyVerified());
        final Chunk first = artifact.chunks().getFirst();
        final byte[] corrupt = first.payload().clone(); corrupt[2] ^= 1;
        assertThrows(Exception.class, () -> receiver.putChunk(target, artifact.id(), new Chunk(artifact.id(), 0, first.sha256(), corrupt)));
        receiver.putChunk(target, artifact.id(), first);
        assertTrue(receiver.verifyImport(target, targetTable, artifact.id()).historyVerified());
        assertEquals(301, artifact.envelope().manifest().currentKeys());
    }

    @Test
    void selfConsistentTamperingStillFailsAgainstSavedSourceManifest() throws Exception {
        final Artifact artifact = export(null);
        releaseSourceArtifact(artifact);
        receiver.beginImport(target, targetTable, new Import(targetTable.getId(), artifact.envelope()));
        final Chunk original = artifact.chunks().getFirst();
        final byte[] changed = new String(original.payload(), java.nio.charset.StandardCharsets.UTF_8)
                .replace("updated", "altered").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        receiver.putChunk(target, artifact.id(), new Chunk(artifact.id(), 0, sha256(changed), changed));
        assertThrows(ResponseStatusException.class, () -> receiver.verifyImport(target, targetTable, artifact.id()));
        assertFalse(receiver.status(target, artifact.id()).historyVerified());
        try (Connection c = connection()) { assertTrue(receiver.readCurrentKeys(c, artifact.id(), null, 10).isEmpty()); }
    }

    @Test
    void sourcePrimaryAndMappedTargetBindingsAreRequired() throws Exception {
        final Artifact artifact = export(null);
        assertThrows(ResponseStatusException.class, () -> receiver.create(database, sourceTable,
                new Create(UUID.randomUUID(), sourceTable.getId(), null)));
        targetTable.setReplicaUrls(Map.of(ORIGIN, UUID.randomUUID()));
        assertThrows(ResponseStatusException.class, () -> receiver.beginImport(target, targetTable,
                new Import(targetTable.getId(), artifact.envelope())));
        targetTable.setReplicaUrls(Map.of(ORIGIN, sourceTable.getId()));
        target.setCreationLocation("https://unregistered.example");
        assertThrows(ResponseStatusException.class, () -> receiver.beginImport(target, targetTable,
                new Import(targetTable.getId(), artifact.envelope())));
    }

    @Test
    void restoreRegressionAndCheckpointForkFailClosed() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            journal.enqueue(c, database, sourceTable, HttpMethod.POST, DataReplicationDto.builder().build()); c.commit();
        }
        final Artifact artifact = export(null);
        final Manifest m = artifact.envelope().manifest();
        assertEquals(1, m.boundary());
        final Checkpoint base = new Checkpoint(m.epoch(), m.boundary(), m.boundaryEventId());
        assertEquals(base, source.create(database, sourceTable, new Create(UUID.randomUUID(), sourceTable.getId(), base)).manifest().base());
        try (Connection c = connection()) {
            sql(c, "UPDATE tuple_replication_journal_counter SET last_sequence=0");
        }
        assertThrows(ResponseStatusException.class, () -> export(base));
        try (Connection c = connection()) {
            sql(c, "UPDATE tuple_replication_journal_counter SET last_sequence=1");
            sql(c, "UPDATE tuple_replication_notification_outbox SET id='" + UUID.randomUUID() + "' WHERE event_sequence=1");
        }
        assertThrows(ResponseStatusException.class, () -> export(base));
    }

    @Test
    void subsequentTargetImportRequiresItsAcceptedCheckpointAndSameEpoch() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false); journal.enqueue(c, database, sourceTable, HttpMethod.POST, DataReplicationDto.builder().build()); c.commit();
        }
        final Artifact first = export(null);
        releaseSourceArtifact(first);
        receiver.beginImport(target, targetTable, new Import(targetTable.getId(), first.envelope()));
        for (Chunk chunk : first.chunks()) receiver.putChunk(target, first.id(), chunk);
        receiver.verifyImport(target, targetTable, first.id());
        final Checkpoint base = receiver.targetCheckpoint(target, targetTable);
        final Artifact unanchored = export(null);
        releaseSourceArtifact(unanchored);
        assertThrows(ResponseStatusException.class, () -> receiver.beginImport(target, targetTable,
                new Import(targetTable.getId(), unanchored.envelope())));
        try (Connection c = connection()) {
            c.setAutoCommit(false); journal.enqueue(c, database, sourceTable, HttpMethod.POST, DataReplicationDto.builder().build()); c.commit();
        }
        final Artifact next = export(base);
        releaseSourceArtifact(next);
        assertEquals("STAGING", receiver.beginImport(target, targetTable, new Import(targetTable.getId(), next.envelope())).status());
        for (Chunk chunk : next.chunks()) receiver.putChunk(target, next.id(), chunk);
        receiver.verifyImport(target, targetTable, next.id());
        assertThrows(ResponseStatusException.class, () -> receiver.reconcileImport(target, targetTable, first.id(),
                (c, db, table, envelope) -> fail("Older verified artifact must not run its callback")));
        final Checkpoint otherEpoch = new Checkpoint(UUID.randomUUID(), base.boundary(), base.eventId());
        assertThrows(ResponseStatusException.class, () -> export(otherEpoch));
    }

    @Test
    void failedChunkPersistenceLeavesNoReadyManifestAndDoesNotTouchSourceHistory() throws Exception {
        try (Connection c = connection(); PreparedStatement statement = c.prepareStatement("INSERT INTO samples(replication_key) VALUES (?)")) {
            for (int i = 0; i < 300; i++) { statement.setString(1, "retry-" + i); statement.addBatch(); }
            statement.executeBatch();
        }
        final Artifact initial = export(null);
        try (Connection c = connection()) {
            sql(c, "CREATE TRIGGER reject_chunk BEFORE INSERT ON replication_history_chunks FOR EACH ROW "
                    + "BEGIN IF NEW.chunk_index=1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='injected chunk failure'; END IF; END");
        }
        final UUID id = UUID.randomUUID();
        assertThrows(SQLException.class, () -> source.create(database, sourceTable, new Create(id, sourceTable.getId(), null)));
        assertEquals("BUILDING", source.status(database, id).status());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> source.manifest(database, id)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> source.create(database, sourceTable,
                new Create(id, sourceTable.getId(), new Checkpoint(initial.envelope().manifest().epoch(), 0, null))));
        assertEquals(303, initial.envelope().manifest().rows());
        try (Connection c = connection(); var result = c.createStatement().executeQuery("SELECT COUNT(*) FROM samples FOR SYSTEM_TIME ALL")) {
            assertTrue(result.next()); assertEquals(303, result.getLong(1));
        }
        try (Connection c = connection()) {
            assertNotNull(source.readChunk(c, id, 0));
            sql(c, "DROP TRIGGER reject_chunk");
            sql(c, "INSERT INTO samples(replication_key) VALUES ('before-retry')");
        }
        final var recovered = source.create(database, sourceTable, new Create(id, sourceTable.getId(), null));
        assertEquals(id, recovered.manifest().snapshotId());
        assertEquals(304, recovered.manifest().rows());
        try (Connection c = connection()) { sql(c, "INSERT INTO samples(replication_key) VALUES ('after-ready')"); }
        assertEquals(recovered, source.create(database, sourceTable, new Create(id, sourceTable.getId(), null)));
    }

    @Test
    void callbackPublicationIsAtomicAndSuccessfulRetryIsIdempotent() throws Exception {
        final Artifact artifact = export(null);
        releaseSourceArtifact(artifact);
        receiver.beginImport(target, targetTable, new Import(targetTable.getId(), artifact.envelope()));
        for (Chunk chunk : artifact.chunks()) receiver.putChunk(target, artifact.id(), chunk);
        final String before = nativeTarget();
        assertThrows(SQLException.class, () -> receiver.reconcileImport(target, targetTable, artifact.id(), (c, db, table, envelope) -> {
            assertFalse(c.getAutoCommit());
            assertEquals(1, receiver.readCurrentKeys(c, artifact.id(), null, 10).size());
            sql(c, "UPDATE replica SET label='rolled-back' WHERE replication_key='a'");
            throw new SQLException("injected callback failure");
        }));
        assertEquals(before, nativeTarget());
        assertEquals("STAGING", receiver.status(target, artifact.id()).status());
        try (Connection c = connection()) { assertTrue(receiver.readCurrentKeys(c, artifact.id(), null, 10).isEmpty()); }
        final AtomicInteger called = new AtomicInteger();
        final HistorySnapshotService.SnapshotReconciler callback = (c, db, table, envelope) -> {
            called.incrementAndGet();
            final CurrentKey key = receiver.readCurrentKeys(c, artifact.id(), null, 10).getFirst();
            final Row row = rows(receiver.readChunk(c, artifact.id(), key.chunkIndex()), envelope.manifest().columns()).get(key.rowIndex());
            assertEquals("updated", data(envelope.manifest(), row).get("label"));
            sql(c, "UPDATE replica SET label='reconciled' WHERE replication_key='a'");
        };
        assertTrue(receiver.reconcileImport(target, targetTable, artifact.id(), callback).currentReconciled());
        assertTrue(receiver.reconcileImport(target, targetTable, artifact.id(), callback).currentReconciled());
        assertEquals(1, called.get());
        assertTrue(nativeTarget().contains("local-before"));
        assertTrue(nativeTarget().contains("local-after"));
        assertTrue(nativeTarget().contains("reconciled"));
    }

    @Test
    void capturedStreamFrontierAllowsUnknownEpochAndNewerEventsAfterCapture() throws Exception {
        final Artifact first = export(null);
        releaseSourceArtifact(first);
        receiver.beginImport(target, targetTable, new Import(targetTable.getId(), first.envelope()));
        for (Chunk chunk : first.chunks()) receiver.putChunk(target, first.id(), chunk);
        receiver.verifyImport(target, targetTable, first.id());
        try (Connection c = connection()) {
            c.setAutoCommit(false); journal.enqueue(c, database, sourceTable, HttpMethod.POST, DataReplicationDto.builder().build()); c.commit();
        }
        final Checkpoint frontier;
        try (Connection c = connection()) {
            frontier = new Checkpoint(null, 1, journal.readRange(c, 0, 1, 1).getFirst().eventId());
        }
        assertThrows(ResponseStatusException.class, () -> export(new Checkpoint(null, 2, UUID.randomUUID())));
        assertThrows(ResponseStatusException.class, () -> export(new Checkpoint(null, 1, UUID.randomUUID())));
        final Artifact snapshot = export(frontier);
        assertEquals(frontier, snapshot.envelope().manifest().base());
        try (Connection c = connection()) {
            c.setAutoCommit(false); journal.enqueue(c, database, sourceTable, HttpMethod.POST, DataReplicationDto.builder().build()); c.commit();
        }
        releaseSourceArtifact(snapshot);
        receiver.beginImport(target, targetTable, new Import(targetTable.getId(), snapshot.envelope()));
        for (Chunk chunk : snapshot.chunks()) receiver.putChunk(target, snapshot.id(), chunk);
        assertEquals(1, receiver.reconcileImport(target, targetTable, snapshot.id(), (c, db, table, envelope) -> {
            assertEquals(2, journal.readJournalState(c).committedThrough());
            assertEquals(1, envelope.manifest().boundary());
        }).boundary());
    }

    @Test
    void concurrentRetryUsesPerSnapshotLockAndReturnsIdenticalManifest() throws Exception {
        final CountDownLatch captured = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            final var state = invocation.callRealMethod();
            captured.countDown();
            assertTrue(release.await(15, TimeUnit.SECONDS));
            return state;
        }).when(journal).readJournalState(any(Connection.class));
        final Create request = new Create(UUID.randomUUID(), sourceTable.getId(), null);
        try (var executor = Executors.newSingleThreadExecutor()) {
            final var first = executor.submit(() -> source.create(database, sourceTable, request));
            try {
                assertTrue(captured.await(10, TimeUnit.SECONDS));
                assertEquals(503, assertThrows(ResponseStatusException.class,
                        () -> source.create(database, sourceTable, request)).getStatusCode().value());
            } finally { release.countDown(); }
            assertEquals(first.get(15, TimeUnit.SECONDS), source.create(database, sourceTable, request));
        }
    }

    @Test
    void changedEnumSchemaAndNonTransactionalArtifactStorageFailClosed() throws Exception {
        try (Connection c = connection()) {
            sql(c, "SET system_versioning_alter_history=KEEP");
            sql(c, "ALTER TABLE samples ADD category ENUM('one','two')");
            sql(c, "ALTER TABLE replica ADD category ENUM('one','other')");
        }
        final Artifact artifact = export(null);
        assertTrue(artifact.envelope().manifest().columns().stream().anyMatch(column -> "enum('one','two')".equals(column.sqlType())));
        releaseSourceArtifact(artifact);
        assertThrows(ResponseStatusException.class, () -> receiver.beginImport(target, targetTable,
                new Import(targetTable.getId(), artifact.envelope())));
        try (Connection c = connection()) { sql(c, "ALTER TABLE replication_history_chunks ENGINE=MyISAM"); }
        assertThrows(ResponseStatusException.class, () -> source.create(database, sourceTable,
                new Create(UUID.randomUUID(), sourceTable.getId(), null)));
    }

    @Test
    void emptySnapshotHasVerifiableEmptyRoots() throws Exception {
        try (Connection c = connection()) {
            sql(c, "DROP TABLE samples");
            sql(c, "CREATE TABLE samples LIKE replica");
        }
        final Artifact artifact = export(null);
        assertEquals(0, artifact.envelope().manifest().rows());
        assertEquals(0, artifact.envelope().manifest().chunks());
        releaseSourceArtifact(artifact);
        receiver.beginImport(target, targetTable, new Import(targetTable.getId(), artifact.envelope()));
        assertTrue(receiver.verifyImport(target, targetTable, artifact.id()).historyVerified());
    }

    @Test
    void bitBlobAndTextRemainExactAndOversizeRowsFailBeforePublication() throws Exception {
        try (Connection c = connection()) {
            sql(c, "SET system_versioning_alter_history=KEEP");
            sql(c, "ALTER TABLE samples ADD bits BIT(16), ADD bytes LONGBLOB, ADD text_value LONGTEXT, ADD kind ENUM('\u03b1')");
            sql(c, "UPDATE samples SET bits=b'1010101010101010',bytes=X'00FF',text_value='' WHERE replication_key='a'");
            sql(c, "ALTER DATABASE " + SCHEMA + " CHARACTER SET latin1");
        }
        final Artifact artifact = export(null);
        assertEquals(artifact.envelope(), source.manifest(database, artifact.id()));
        assertTrue(artifact.envelope().manifest().columns().stream().anyMatch(column -> "enum('\u03b1')".equals(column.sqlType())));
        final Row current = artifact.chunks().stream().flatMap(chunk -> uncheckedRows(chunk, artifact.envelope().manifest().columns()).stream())
                .filter(Row::current).findFirst().orElseThrow();
        final Map<String, Object> values = data(artifact.envelope().manifest(), current);
        assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0xAA}, (byte[]) values.get("bits"));
        assertArrayEquals(new byte[]{0, (byte) 0xFF}, (byte[]) values.get("bytes"));
        assertEquals("", values.get("text_value"));
        try (Connection c = connection()) {
            sql(c, "UPDATE samples SET text_value=REPEAT('x'," + (MAX_CHUNK_BYTES + 1) + ") WHERE replication_key='a'");
        }
        final UUID id = UUID.randomUUID();
        assertEquals(413, assertThrows(ResponseStatusException.class,
                () -> source.create(database, sourceTable, new Create(id, sourceTable.getId(), null))).getStatusCode().value());
        assertFalse(source.status(database, id).historyVerified());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> source.manifest(database, id)).getStatusCode().value());
    }

    private record Artifact(Envelope envelope, List<Chunk> chunks) {
        UUID id() { return envelope.manifest().snapshotId(); }
    }
    private Artifact export(Checkpoint base) throws Exception {
        final Envelope envelope = source.create(database, sourceTable, new Create(UUID.randomUUID(), sourceTable.getId(), base));
        final List<Chunk> chunks = new ArrayList<>();
        for (long i = 0; i < envelope.manifest().chunks(); i++) chunks.add(source.readChunk(database, envelope.manifest().snapshotId(), i));
        return new Artifact(envelope, chunks);
    }
    // Both simulated sites share the one authorized test schema; only artifact transport storage is moved between them.
    private void releaseSourceArtifact(Artifact artifact) throws Exception {
        try (Connection c = connection()) {
            for (String relation : List.of("replication_history_current_keys", "replication_history_chunks", "replication_history_snapshots")) {
                try (var statement = c.prepareStatement("DELETE FROM " + relation + " WHERE snapshot_id=?")) {
                    statement.setString(1, artifact.id().toString()); statement.executeUpdate();
                }
            }
        }
    }
    private HistorySnapshotService service(String site) {
        return new HistorySnapshotService(journal, site) {
            @Override public ComboPooledDataSource getDataSource(Database ignored) {
                final var pool = new ComboPooledDataSource();
                pool.setJdbcUrl(URL + SCHEMA); pool.setUser("root"); pool.setPassword(password());
                pool.setMinPoolSize(2); pool.setInitialPoolSize(2); pool.setMaxPoolSize(3);
                return pool;
            }
        };
    }
    private String nativeTarget() throws Exception {
        try (Connection c = connection(); var statement = c.createStatement(); var rows = statement.executeQuery(
                "SELECT label,ROW_START,ROW_END FROM replica FOR SYSTEM_TIME ALL ORDER BY ROW_START")) {
            final StringBuilder result = new StringBuilder();
            while (rows.next()) result.append(rows.getString(1)).append('|').append(rows.getString(2)).append('|').append(rows.getString(3)).append(';');
            return result.toString();
        }
    }
    private static List<Row> uncheckedRows(Chunk chunk, List<Column> columns) {
        try { return rows(chunk, columns); } catch (Exception e) { throw new AssertionError(e); }
    }
    private Connection connection() throws SQLException {
        final Connection c = DriverManager.getConnection(URL + SCHEMA, "root", password());
        sql(c, "SET time_zone='+00:00'");
        return c;
    }
    private static String password() { return System.getenv("HISTORY_SNAPSHOT_SQL_TEST_PASSWORD"); }
    private static void sql(Connection c, String sql) throws SQLException {
        try (var statement = c.createStatement()) { statement.execute(sql); }
    }
}
