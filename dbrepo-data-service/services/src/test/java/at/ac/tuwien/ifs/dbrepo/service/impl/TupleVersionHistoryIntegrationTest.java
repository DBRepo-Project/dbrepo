package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpMethod;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TUPLE_VERSION_SQL_PORT", matches = "[0-9]+")
class TupleVersionHistoryIntegrationTest {
    private final UUID database = UUID.randomUUID();
    private final UUID table = UUID.randomUUID();
    private static final String SITE = "https://a.example";
    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");

    private Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://127.0.0.1:" + System.getenv("TUPLE_VERSION_SQL_PORT")
                + "/tuple_version_test", "root", System.getenv("TUPLE_VERSION_SQL_PASSWORD"));
    }

    @BeforeEach
    void setup() throws Exception {
        try (var c = DriverManager.getConnection("jdbc:mariadb://127.0.0.1:" + System.getenv("TUPLE_VERSION_SQL_PORT"),
                "root", System.getenv("TUPLE_VERSION_SQL_PASSWORD")); var s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS tuple_version_test");
            s.execute("CREATE DATABASE tuple_version_test");
        }
        try (var c = connection()) { TupleVersionHistory.prepare(c, null); }
    }

    @Test
    void insertUpdateDeletePreserveTupleIdentityAndCloseTheRightValuesVersion() throws Exception {
        final UUID firstId = UUID.randomUUID(), nextId = UUID.randomUUID();
        try (var c = connection()) {
            c.setAutoCommit(false);
            final var first = tuple(START, null);
            TupleVersionHistory.record(c, SITE, database, table, first, HttpMethod.POST, firstId);
            final var next = tuple(START.plusSeconds(120), null);
            TupleVersionHistory.record(c, SITE, database, table, next, HttpMethod.PUT, nextId);
            final var deleted = tuple(START.plusSeconds(120), START.plusSeconds(180));
            TupleVersionHistory.record(c, SITE, database, table, deleted, HttpMethod.DELETE, UUID.randomUUID());
            assertEquals(nextId, deleted.getVersionId());
            assertEquals(2L, deleted.getVisibilityStart());
            assertEquals(3L, deleted.getVisibilityEnd());
            c.commit();
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT version_id,visibility_start,visibility_end"
                    + " FROM tuple_replication_timestamps ORDER BY row_start")) {
                assertTrue(r.next()); assertEquals(firstId.toString(), r.getString(1)); assertEquals(1, r.getLong(2)); assertEquals(2, r.getLong(3));
                assertTrue(r.next()); assertEquals(nextId.toString(), r.getString(1)); assertEquals(2, r.getLong(2)); assertEquals(3, r.getLong(3));
                assertFalse(r.next());
            }
            assertEquals(firstId, TupleVersionHistory.findVersion(c, table, "K", START));
        }
    }

    @Test
    void rollbackRemovesVersionIntervalAndVisibilitySequence() throws Exception {
        try (var c = connection()) {
            c.setAutoCommit(false);
            TupleVersionHistory.record(c, SITE, database, table, tuple(START, null), HttpMethod.POST, UUID.randomUUID());
            assertEquals(1, TupleVersionHistory.cut(c));
            c.rollback();
            assertEquals(0, TupleVersionHistory.cut(c));
            assertNull(TupleVersionHistory.findVersion(c, table, "K", START));
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT COUNT(*) FROM tuple_replication_timestamps")) {
                assertTrue(r.next()); assertEquals(0, r.getInt(1));
            }
        }
    }

    @Test
    void timestampRetryPreservesVersionAndRejectsConflictingVersionIdentity() throws Exception {
        final UUID version = UUID.randomUUID();
        try (var c = connection()) {
            c.setAutoCommit(false);
            TupleVersionHistory.record(c, SITE, database, table, tuple(START, null), HttpMethod.POST, version);
            final var timestamp = TupleReplicationTimestampDto.builder().siteUrl(SITE).databaseId(database).tableId(table)
                    .replicationId("K").versionId(version).rowStart(START).build();
            ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, timestamp);
            assertEquals(1, TupleVersionHistory.cut(c));
            timestamp.setVersionId(UUID.randomUUID());
            assertThrows(SQLException.class, () -> ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, timestamp));
            c.rollback();
        }
    }

    private TupleWithTimestampsDto tuple(Instant start, Instant end) {
        return TupleWithTimestampsDto.builder().replicationKey("K").insertedAt(start).deletedAt(end).build();
    }

    @Test void latePredecessorGetsItsKnownSuccessorsBoundaryAndSequence() throws Exception {
        try(var c=connection()) {
            c.setAutoCommit(false);
            final var later=TupleReplicationTimestampDto.builder().siteUrl(SITE).databaseId(database).tableId(table)
                    .replicationId("K").versionId(UUID.randomUUID()).rowStart(START.plusSeconds(60)).visibilityStart(2L).build();
            ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c,later);
            final var earlier=TupleReplicationTimestampDto.builder().siteUrl(SITE).databaseId(database).tableId(table)
                    .replicationId("K").versionId(UUID.randomUUID()).rowStart(START).visibilityStart(1L).build();
            ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c,earlier);
            ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c,earlier);
            try(var s=c.createStatement();var rows=s.executeQuery("SELECT row_end,visibility_end FROM tuple_replication_timestamps ORDER BY row_start")) {
                assertTrue(rows.next());assertEquals(START.plusSeconds(60),rows.getTimestamp(1,TupleVersionHistory.utc()).toInstant());
                assertEquals(2,rows.getLong(2));
            }
            c.commit();
        }
    }
}
