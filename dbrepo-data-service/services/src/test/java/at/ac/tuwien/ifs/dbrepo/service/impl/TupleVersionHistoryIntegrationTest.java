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
    void oldUuidMappingsArePreservedButNotGuessedDuringRepeatableMigration() throws Exception {
        try (var c = connection(); var s = c.createStatement()) {
            s.execute("DROP TABLE tuple_replication_versions");
            s.execute("CREATE TABLE tuple_replication_versions(table_id VARCHAR(36),replication_id VARCHAR(255),"
                    + "version_id VARCHAR(36) NOT NULL,native_start TIMESTAMP(6),PRIMARY KEY(table_id,version_id),"
                    + "UNIQUE KEY native_version(table_id,replication_id,native_start)) ENGINE=InnoDB");
            try (var insert = c.prepareStatement("INSERT INTO tuple_replication_versions VALUES(?,?,?,?)")) {
                insert.setString(1,table.toString()); insert.setString(2,"K"); insert.setString(3,UUID.randomUUID().toString());
                insert.setTimestamp(4,Timestamp.from(START.plusSeconds(120)),TupleVersionHistory.utc()); insert.executeUpdate();
            }
            TupleVersionHistory.prepare(c,null);
            TupleVersionHistory.prepare(c,null);
            assertNull(TupleVersionHistory.findVersion(c,table,"K",START.plusSeconds(120)));
            c.setAutoCommit(false);
            final var receipt=tuple(START.plusSeconds(120),null);
            receipt.setMasterSiteTs(START);
            TupleVersionHistory.bindNative(c,table,receipt);
            assertEquals(START,TupleVersionHistory.findVersion(c,table,"K",START.plusSeconds(120)));
            assertThrows(SQLException.class,()->TupleVersionHistory.record(c,SITE,database,table,
                    tuple(START.plusSeconds(180),null),HttpMethod.PUT,START));
            c.rollback();
        }
    }

    @Test
    void deleteOfAnUnmappedLegacyReplicaDoesNotInventAMasterTimestamp() throws Exception {
        try(var c=connection()) {
            c.setAutoCommit(false);
            final var deleted=tuple(START,START.plusSeconds(60));
            TupleVersionHistory.record(c,SITE,database,table,deleted,HttpMethod.DELETE,null);
            assertNull(deleted.getMasterSiteTs());
            assertNull(TupleVersionHistory.findVersion(c,table,"K",START));
            assertEquals(1L,deleted.getVisibilityEnd());
            c.rollback();
        }
    }

    @Test
    void sourceTimestampPrecisionIsNotSilentlyRounded() {
        assertThrows(SQLException.class,()->TupleVersionHistory.requireMasterTimestamp(START.plusNanos(1)));
    }

    @Test
    void masterTimestampIsCombinedWithTheStableTupleKey() throws Exception {
        try (var c = connection()) {
            c.setAutoCommit(false);
            final var first = tuple(START, null);
            final var second = tuple(START, null);
            second.setReplicationKey("other");
            TupleVersionHistory.record(c, SITE, database, table, first, HttpMethod.POST, START);
            TupleVersionHistory.record(c, SITE, database, table, second, HttpMethod.POST, START);
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT COUNT(*),COUNT(DISTINCT master_site_ts) FROM tuple_replication_versions")) {
                assertTrue(r.next());
                assertEquals(2, r.getInt(1));
                assertEquals(1, r.getInt(2));
            }
            c.rollback();
        }
    }

    @Test
    void insertUpdateDeletePreserveTupleIdentityAndCloseTheRightValuesVersion() throws Exception {
        final Instant firstId = START, nextId = START.plusSeconds(120);
        try (var c = connection()) {
            c.setAutoCommit(false);
            final var first = tuple(START, null);
            TupleVersionHistory.record(c, SITE, database, table, first, HttpMethod.POST, firstId);
            final var next = tuple(START.plusSeconds(120), null);
            TupleVersionHistory.record(c, SITE, database, table, next, HttpMethod.PUT, nextId);
            final var deleted = tuple(START.plusSeconds(120), START.plusSeconds(180));
            TupleVersionHistory.record(c, SITE, database, table, deleted, HttpMethod.DELETE, null);
            assertEquals(nextId, deleted.getMasterSiteTs());
            assertEquals(2L, deleted.getVisibilityStart());
            assertEquals(3L, deleted.getVisibilityEnd());
            c.commit();
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT master_site_ts,visibility_start,visibility_end"
                    + " FROM tuple_replication_timestamps ORDER BY row_start")) {
                assertTrue(r.next()); assertEquals(firstId, r.getTimestamp(1,TupleVersionHistory.utc()).toInstant()); assertEquals(1, r.getLong(2)); assertEquals(2, r.getLong(3));
                assertTrue(r.next()); assertEquals(nextId, r.getTimestamp(1,TupleVersionHistory.utc()).toInstant()); assertEquals(2, r.getLong(2)); assertEquals(3, r.getLong(3));
                assertFalse(r.next());
            }
            assertEquals(firstId, TupleVersionHistory.findVersion(c, table, "K", START));
        }
    }

    @Test
    void rollbackRemovesVersionIntervalAndVisibilitySequence() throws Exception {
        try (var c = connection()) {
            c.setAutoCommit(false);
            TupleVersionHistory.record(c, SITE, database, table, tuple(START, null), HttpMethod.POST, START);
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
    void timestampRetryPreservesVersionAndRejectsConflictingMasterTimestamp() throws Exception {
        final Instant version = START;
        try (var c = connection()) {
            c.setAutoCommit(false);
            TupleVersionHistory.record(c, SITE, database, table, tuple(START, null), HttpMethod.POST, version);
            final var timestamp = TupleReplicationTimestampDto.builder().siteUrl(SITE).databaseId(database).tableId(table)
                    .replicationId("K").masterSiteTs(version).rowStart(START).build();
            ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, timestamp);
            assertEquals(1, TupleVersionHistory.cut(c));
            timestamp.setMasterSiteTs(START.plusSeconds(1));
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
                    .replicationId("K").masterSiteTs(START.plusSeconds(60)).rowStart(START.plusSeconds(60)).visibilityStart(2L).build();
            ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c,later);
            final var earlier=TupleReplicationTimestampDto.builder().siteUrl(SITE).databaseId(database).tableId(table)
                    .replicationId("K").masterSiteTs(START).rowStart(START).visibilityStart(1L).build();
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
