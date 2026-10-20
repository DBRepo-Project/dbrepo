package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@EnabledIfEnvironmentVariable(named = "REPLICATION_ACTIVATION_GALERA_SQL_PORTS", matches = "[0-9]+,[0-9]+,[0-9]+")
class ReplicationActivationGaleraIntegrationTest {
    private static final String SCHEMA = "replication_activation_galera_test";
    private final String[] ports = System.getenv("REPLICATION_ACTIVATION_GALERA_SQL_PORTS").split(",");
    private final String password = System.getenv("REPLICATION_ACTIVATION_SQL_TEST_PASSWORD");
    private final ReplicationActivationService activation = new ReplicationActivationService(
            mock(MetadataService.class), mock(RestTemplate.class), mock(TupleReplicationOutboxService.class),
            "https://target.example", "https://source.example");

    @BeforeEach
    void setup() throws Exception {
        try (var c = connect(0); var s = c.createStatement()) {
            s.execute("CREATE DATABASE IF NOT EXISTS " + SCHEMA);
            s.execute("DROP TABLE IF EXISTS " + SCHEMA + ".samples");
            s.execute("USE " + SCHEMA);
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        try (var c = connect(0); var s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + SCHEMA);
        }
    }

    @Test
    void activationKeepsTheSameKeysValuesAndMicrosecondHistoryOnEveryNode() throws Exception {
        create("site INT NOT NULL,id INT NOT NULL,value INT,PRIMARY KEY(site,id)");
        execute(0, "SET timestamp=1700000000.123456; INSERT INTO samples VALUES(1,1,10),(1,2,30);"
                + "SET timestamp=1700000010.654321; UPDATE samples SET value=20 WHERE id=1;"
                + "SET timestamp=1700000020.111111; UPDATE samples SET value=10 WHERE id=1; DELETE FROM samples WHERE id=2");
        final List<String> before = rows(0, false);
        final List<String> settings = settings(0);
        try (var c = connect(0)) {
            activation.prepareTable(c, "samples");
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT @@session.system_versioning_alter_history")) {
                assertTrue(r.next()); assertEquals(settings.getFirst(), r.getString(1));
            }
        }
        final List<String> expected = rows(0, true);
        for (int node = 0; node < 3; node++) {
            assertEquals(before, rows(node, false), "Original values and periods changed on node " + node);
            assertEquals(expected, rows(node, true), "Backfilled keys differ on node " + node);
            assertEquals(settings, settings(node), "History policy leaked out of the ALTER statement");
        }
        try (var c = connect(2)) { activation.prepareTable(c, "samples"); }
        assertEquals(expected, rows(0, true), "Retry changed existing identities");
        final String key;
        try (var c = connect(0); var s = c.createStatement(); var r = s.executeQuery("SELECT replication_key FROM samples WHERE id=1")) {
            assertTrue(r.next()); key = r.getString(1);
            UUID.fromString(key);
        }
        execute(2, "UPDATE samples SET value=40 WHERE id=1; INSERT INTO samples(site,id,value) VALUES(1,3,99)");
        final List<String> live = rows(2, true);
        for (int node = 0; node < 3; node++) assertEquals(live, rows(node, true));
        try (var c = connect(1); var s = c.createStatement(); var r = s.executeQuery("SELECT replication_key FROM samples WHERE id=1")) {
            assertTrue(r.next()); assertEquals(key, r.getString(1), "Update replaced the tuple identity");
        }
    }

    @Test
    void nonNullableUniqueKeyAlsoSupportsTablesWithoutAPrimaryKey() throws Exception {
        create("id INT NOT NULL UNIQUE,value INT");
        execute(0, "INSERT INTO samples VALUES(1,10),(2,20)");
        try (var c = connect(0)) { activation.prepareTable(c, "samples"); }
        for (int node = 1; node < 3; node++) assertEquals(rows(0, true), rows(node, true));
    }

    @Test
    void populatedTablesWithoutAProvenUniqueIdentityAreRejectedBeforeAltering() throws Exception {
        create("id INT NULL UNIQUE,value INT");
        execute(0, "INSERT INTO samples VALUES(NULL,10),(NULL,10)");
        final var before = rows(0, false);
        try (var c = connect(0)) {
            assertThrows(SQLException.class, () -> activation.prepareTable(c, "samples"));
        }
        for (int node = 0; node < 3; node++) {
            assertEquals(before, rows(node, false));
            try (var c = connect(node); var s = c.createStatement(); var r = s.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()"
                            + " AND TABLE_NAME='samples' AND COLUMN_NAME='replication_key'")) {
                assertTrue(r.next()); assertEquals(0, r.getInt(1));
            }
        }
    }

    @Test
    void emptyTablesNeedNoLegacyIdentityAndNewUuidDefaultsReplicateNormally() throws Exception {
        create("id INT,value INT");
        try (var c = connect(0)) { activation.prepareTable(c, "samples"); }
        execute(1, "INSERT INTO samples(id,value) VALUES(1,10),(1,10)");
        for (int node = 0; node < 3; node++) assertEquals(rows(1, true), rows(node, true));
    }

    @Test
    void deletedHistoricalRowsStillRequireAProvenLegacyIdentity() throws Exception {
        create("id INT,value INT");
        execute(0, "INSERT INTO samples VALUES(1,10); DELETE FROM samples");
        final var before = rows(0, false);
        assertFalse(before.isEmpty());
        try (var c = connect(0)) { assertThrows(SQLException.class, () -> activation.prepareTable(c, "samples")); }
        for (int node = 0; node < 3; node++) assertEquals(before, rows(node, false));
    }

    @Test
    void retryAfterAnInterruptedDefaultChangePreservesKeysAndRestoresUuidInserts() throws Exception {
        create("id INT PRIMARY KEY,value INT");
        execute(0, "INSERT INTO samples VALUES(1,10); SET STATEMENT system_versioning_alter_history=KEEP FOR ALTER TABLE samples"
                + " ADD COLUMN replication_key VARCHAR(36) NOT NULL DEFAULT('00000000-0000-4000-8000-000000000001'),"
                + " ADD UNIQUE KEY(replication_key)");
        final var before = rows(0, true);
        try (var c = connect(1)) { activation.prepareTable(c, "samples"); }
        for (int node = 0; node < 3; node++) assertEquals(before, rows(node, true));
        execute(2, "INSERT INTO samples(id,value) VALUES(2,20),(3,30)");
        for (int node = 0; node < 3; node++) assertEquals(rows(2, true), rows(node, true));
    }

    private Connection connect(int node) throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:mariadb://127.0.0.1:" + ports[node] + "/", "root", password);
        try (var s = connection.createStatement()) {
            s.execute("SET time_zone='+00:00'");
            s.execute("SET SESSION wsrep_sync_wait=1");
            s.execute("USE " + SCHEMA);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1049) { connection.close(); throw e; }
        }
        return connection;
    }

    private void create(String columns) throws Exception {
        execute(0, "CREATE TABLE samples(" + columns + ") ENGINE=InnoDB WITH SYSTEM VERSIONING");
    }

    private void execute(int node, String statements) throws Exception {
        try (var c = connect(node); var s = c.createStatement()) {
            for (String sql : statements.split(";")) s.execute(sql);
        }
    }

    private List<String> rows(int node, boolean keys) throws Exception {
        final List<String> result = new ArrayList<>();
        try (var c = connect(node); var s = c.createStatement(); var r = s.executeQuery(
                "SELECT id,value,ROW_START,ROW_END" + (keys ? ",replication_key" : "")
                        + " FROM samples FOR SYSTEM_TIME ALL ORDER BY id,ROW_START,value" + (keys ? ",replication_key" : ""))) {
            while (r.next()) {
                final List<String> values = new ArrayList<>();
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) values.add(r.getString(i));
                result.add(String.join("|", values));
            }
        }
        return result;
    }

    private List<String> settings(int node) throws Exception {
        try (var c = connect(node); var s = c.createStatement(); var r = s.executeQuery(
                "SELECT @@session.system_versioning_alter_history,@@global.system_versioning_alter_history")) {
            assertTrue(r.next()); return List.of(r.getString(1), r.getString(2));
        }
    }
}
