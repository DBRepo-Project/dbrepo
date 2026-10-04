package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.SubsetCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreInsertException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStorePersistException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryExecutionException;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetServiceMariaDbImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.servlet.http.HttpServletRequest;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** All destructive SQL is confined to this suite's one explicitly named database. */
@EnabledIfEnvironmentVariable(named = "SUBSET_SQL_TEST_PORT", matches = "[0-9]+")
class SubsetReplicationIntegrationTest {
    private static final String A = "https://a.example";
    private static final String B = "https://b.example";
    private static final String C = "https://c.example";
    private static final UUID A_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID B_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID C_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final Instant SELECTED = Instant.parse("2020-02-29T12:34:56.123456Z");
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("SUBSET_SQL_TEST_PORT");
    private final String password = System.getenv("SUBSET_SQL_TEST_PASSWORD");
    private final MariaDbMapper mapper = Mappers.getMapper(MariaDbMapper.class);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final Database database = Database.builder().id(A_ID).internalName("subset_replication_test")
            .creationLocation(A).replicaUrls(new HashMap<>(Map.of(B, B_ID, C, C_ID)))
            .container(Container.builder().host("127.0.0.1").port(Integer.valueOf(System.getenv("SUBSET_SQL_TEST_PORT")))
                    .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build()).build();
    private SubsetReplicationService replication;
    private SubsetServiceMariaDbImpl service;

    @BeforeEach
    void schema() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "root", password);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS subset_replication_test");
            statement.execute("CREATE DATABASE subset_replication_test");
            connection.setCatalog("subset_replication_test");
            statement.execute(mapper.queryStoreCreateTableRawQuery());
        }
        replication = replication(A);
        service = service(replication);
        service.upgradeQueryStore(database);
    }

    @Test
    void localIdentitySelectionFixityAndPersistenceShareDurableOutbox() throws Exception {
        final UUID id = create();
        assertEquals(id, service.storeQuery(database, "SELECT a FROM data", "SELECT 'original' a",
                SELECTED.plusSeconds(10), "another-local-user"));
        final SubsetReplicationDto initial = read(id);
        assertEquals(A, initial.originSite());
        assertEquals(SELECTED, initial.selectedAt());
        assertEquals(1, initial.resultCount());
        assertTrue(initial.resultHash().matches("v2:[0-9a-f]{64}"));
        assertEquals(1, initial.revision());
        assertEquals(2, count("qs_subset_outbox"));
        assertEquals(initial, json.readValue(json.writeValueAsBytes(initial), SubsetReplicationDto.class));

        service.persist(database, id, true);
        service.persist(database, id, true);
        assertEquals(2, read(id).revision());
        assertTrue(read(id).persisted());
        service.persist(database, id, false);
        assertEquals(3, read(id).revision());
        assertEquals(3, number("SELECT MIN(revision) FROM qs_subset_outbox"));
        assertEquals(SELECTED, read(id).selectedAt());
    }

    @Test
    void secondaryOriginIsReplicatedThroughPrimaryWithoutBaseDataWrites() throws Exception {
        // The secondary knows only its primary; the primary relays to C using its own topology.
        database.setId(B_ID);
        database.setReplicaUrls(Map.of(A, A_ID));
        replication = replication(B);
        service = service(replication);
        final UUID id = create();
        service.persist(database, id, true);
        final SubsetReplicationDto sent = read(id);
        assertEquals(B, sent.originSite());
        assertEquals(1, count("qs_subset_outbox"));
        clearCurrentSite();
        database.setId(A_ID);
        database.setReplicaUrls(Map.of(B, B_ID, C, C_ID));
        replication = replication(A);
        replication.receive(database, sent);
        final SubsetReplicationDto received = read(id);
        assertEquals(id, received.queryId());
        assertEquals(B, received.originSite());
        assertEquals(sent.query(), received.query());
        assertEquals(sent.queryNormalized(), received.queryNormalized());
        assertEquals(sent.selectedAt(), received.selectedAt());
        assertEquals(sent.resultHash(), received.resultHash());
        assertEquals(sent.resultCount(), received.resultCount());
        assertTrue(received.persisted());
        assertEquals(1, count("qs_subset_outbox"));
        assertEquals(C, string("SELECT target_site FROM qs_subset_outbox"));
        assertNull(string("SELECT created_by FROM qs_queries"));
        assertThrows(ResponseStatusException.class, () -> service(replication).persist(database, id, false));
    }

    @Test
    void duplicateOutOfOrderAndConflictingDeliveriesCannotChangeCanonicalState() throws Exception {
        final UUID id = create();
        final SubsetReplicationDto first = read(id);
        service.persist(database, id, true);
        final SubsetReplicationDto latest = read(id);
        asReceiver();
        replication.receive(database, latest);
        final long history = count("qs_queries FOR SYSTEM_TIME ALL");
        replication.receive(database, latest);
        replication.receive(database, first);
        assertEquals(history, count("qs_queries FOR SYSTEM_TIME ALL"));
        assertTrue(read(id).persisted());
        assertEquals(2, read(id).revision());
        final var wrongHash = new SubsetReplicationDto(id, A, A, A_ID, first.query(), first.queryNormalized(),
                SELECTED, true, "v2:" + "0".repeat(64), 1L, 3);
        assertThrows(ResponseStatusException.class, () -> replication.receive(database, wrongHash));
        final var conflictingRevision = new SubsetReplicationDto(id, A, A, A_ID, first.query(), first.queryNormalized(),
                SELECTED, false, first.resultHash(), 1L, 2);
        assertThrows(ResponseStatusException.class, () -> replication.receive(database, conflictingRevision));
        assertEquals(history, count("qs_queries FOR SYSTEM_TIME ALL"));
    }

    @Test
    void outboxFailureRollsBackCreatePersistAndReceive() throws Exception {
        failOutbox();
        assertThrows(QueryStoreInsertException.class, this::create);
        assertEquals(0, count("qs_queries"));
        assertEquals(0, count("qs_subset_results"));
        assertEquals(0, count("qs_subset_result_rows"));
        sql("DROP TRIGGER fail_subset_outbox");
        final UUID id = create();
        final SubsetReplicationDto sent = read(id);
        failOutbox();
        assertThrows(QueryStorePersistException.class, () -> service.persist(database, id, true));
        assertFalse(read(id).persisted());
        assertEquals(1, read(id).revision());
        asReceiver();
        assertThrows(SQLException.class, () -> replication.receive(database, sent));
        assertEquals(0, count("qs_queries"));
        assertEquals(0, count("qs_subset_outbox"));
    }

    @Test
    void unknownMappingAndLostResponseSurviveRestartAndAcknowledgementRace() throws Exception {
        database.setReplicaUrls(new HashMap<>(Collections.singletonMap(B, null)));
        final UUID id = create();
        final RestTemplate client = mock(RestTemplate.class);
        var dispatcher = new SubsetReplicationDispatcher(null, replication, client, mapper, new SubsetResultService(mapper, json));
        assertEquals(0, dispatcher.dispatch(database));
        verifyNoInteractions(client);
        assertEquals(1, count("qs_subset_outbox"));
        database.setReplicaUrls(Map.of(B, B_ID));
        sql("UPDATE qs_subset_outbox SET next_attempt = UTC_TIMESTAMP(6)");
        when(client.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("response lost"));
        assertEquals(0, dispatcher.dispatch(database));
        assertEquals(2, number("SELECT attempts FROM qs_subset_outbox"));
        sql("UPDATE qs_subset_outbox SET next_attempt = UTC_TIMESTAMP(6)");
        reset(client);
        publishedArtifact(client);
        when(client.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenAnswer(call -> {
                    service.persist(database, id, true);
                    return ResponseEntity.noContent().build();
                });
        dispatcher = new SubsetReplicationDispatcher(null, replication(A), client, mapper, new SubsetResultService(mapper, json));
        assertEquals(1, dispatcher.dispatch(database));
        assertEquals(1, count("qs_subset_outbox"));
        assertEquals(2, number("SELECT revision FROM qs_subset_outbox"));
        reset(client);
        publishedArtifact(client);
        when(client.exchange(eq(B + "/api/v1/database/" + B_ID + "/subset/replicate"), eq(HttpMethod.PUT),
                any(HttpEntity.class), eq(Void.class))).thenReturn(ResponseEntity.noContent().build());
        assertEquals(1, dispatcher.dispatch(database));
        assertEquals(0, count("qs_subset_outbox"));
    }

    @Test
    void parallelStoreCallsReuseIdentityAndLeaveNoSharedWorkTables() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            final var calls = new ArrayList<Callable<UUID>>();
            for (int i = 0; i < 8; i++) calls.add(this::create);
            final var results = executor.invokeAll(calls);
            final UUID id = results.getFirst().get();
            for (var result : results) assertEquals(id, result.get());
        }
        assertEquals(1, count("qs_queries"));
        assertEquals(2, count("qs_subset_outbox"));
        assertEquals(0, number("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() "
                + "AND table_name LIKE '_dbrepo_query_%'"));
    }

    @Test
    void repeatedUpgradePreservesLegacyRowsWithoutInventingOriginOrFixity() throws Exception {
        sql("DROP TABLE qs_queries");
        sql("CREATE TABLE qs_queries (id VARCHAR(36) PRIMARY KEY, created DATETIME NOT NULL DEFAULT NOW(), "
                + "executed DATETIME NOT NULL DEFAULT NOW(), created_by VARCHAR(36), query TEXT NOT NULL, "
                + "query_normalized TEXT NOT NULL, is_persisted BOOLEAN NOT NULL, query_hash VARCHAR(255) NOT NULL, "
                + "result_hash VARCHAR(255), result_number BIGINT) WITH SYSTEM VERSIONING");
        final UUID id = UUID.randomUUID();
        sql("INSERT INTO qs_queries VALUES ('" + id + "','2020-01-01','2020-01-02','legacy', 'SELECT 1', "
                + "'SELECT 1', TRUE, 'old-query-hash','old-result-hash', 1)");
        service.upgradeQueryStore(database);
        service.upgradeQueryStore(database);
        assertEquals(1, count("qs_queries FOR SYSTEM_TIME ALL"));
        assertEquals("old-result-hash", read(id).resultHash());
        assertNull(read(id).originSite());
        assertEquals(0, read(id).revision());
        assertTrue(read(id).persisted());
        assertEquals(Instant.parse("2020-01-02T00:00:00Z"), read(id).selectedAt());
        assertEquals(0, count("qs_subset_outbox"));
        assertNotNull(create());
    }

    @Test
    void invalidPeerMappingLegacyFixityAndLocalOriginSpoofAreRejected() throws Exception {
        final UUID id = create();
        final SubsetReplicationDto original = read(id);
        final var untrusted = new SubsetReplicationDto(id, A, "https://evil.example", A_ID, original.query(),
                original.queryNormalized(), SELECTED, false, original.resultHash(), 1L, 1);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> replication.receive(database, untrusted))
                .getStatusCode().value());
        asReceiver();
        final var badMapping = new SubsetReplicationDto(id, A, A, UUID.randomUUID(), original.query(),
                original.queryNormalized(), SELECTED, false, original.resultHash(), 1L, 1);
        assertThrows(ResponseStatusException.class, () -> replication.receive(database, badMapping));
        final var legacy = new SubsetReplicationDto(id, A, A, A_ID, original.query(), original.queryNormalized(),
                SELECTED, false, "legacy-hash", 1L, 1);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> replication.receive(database, legacy))
                .getStatusCode().value());
        final var spoof = new SubsetReplicationDto(id, B, A, A_ID, original.query(), original.queryNormalized(),
                SELECTED, false, original.resultHash(), 1L, 1);
        assertThrows(ResponseStatusException.class, () -> replication.receive(database, spoof));
        assertEquals(0, count("qs_queries"));
    }

    @Test
    void remoteReplayFailsClosedAgainstActualLocalFixityBeforeDataOrSchemaIsReturned() throws Exception {
        sql("CREATE TABLE sample_data (a VARCHAR(32))");
        sql("INSERT INTO sample_data VALUES ('original')");
        final UUID id = service.storeQuery(database, "SELECT a FROM sample_data", "SELECT a FROM sample_data", SELECTED, "alice");
        final SubsetReplicationDto sent = read(id);
        asReceiver();
        replication.receive(database, sent);
        sql("UPDATE sample_data SET a = 'different local history'");
        final var reader = service(replication);
        final var metadata = mock(MetadataService.class);
        final var data = mock(DataService.class);
        final var analyse = mock(AnalyseService.class);
        final var request = mock(HttpServletRequest.class);
        database.setIsPublic(true);
        when(metadata.getDatabase(B_ID)).thenReturn(database);
        final var endpoint = new SubsetEndpoint(null, data, mapper, reader, analyse, null, metadata,
                mock(EndpointValidator.class), null, new com.fasterxml.jackson.databind.ObjectMapper());
        for (String method : List.of("GET", "HEAD")) {
            when(request.getMethod()).thenReturn(method);
            assertThrows(QueryExecutionException.class, () -> endpoint.getData(B_ID, id, null, "application/json",
                    request, null, null, null));
        }
        verifyNoInteractions(data, analyse);
        assertEquals(sent.resultHash(), read(id).resultHash());
        assertEquals(A, reader.findById(database, id).getCreationLocation());
        assertEquals(sent.selectedAt(), reader.findById(database, id).getExecution());
    }

    @Test
    void localCreationDoesNotAdoptAnotherOriginsIdentityAndConcurrentDuplicatesAreReadOnly() throws Exception {
        final UUID originId = create();
        final SubsetReplicationDto sent = read(originId);
        asReceiver();
        replication.receive(database, sent);
        final long history = count("qs_queries FOR SYSTEM_TIME ALL");
        try (var executor = Executors.newFixedThreadPool(3)) {
            final var calls = new ArrayList<Callable<Void>>();
            for (int i = 0; i < 6; i++) calls.add(() -> { replication.receive(database, sent); return null; });
            for (var result : executor.invokeAll(calls)) result.get();
        }
        assertEquals(history, count("qs_queries FOR SYSTEM_TIME ALL"));
        service = service(replication);
        final UUID localId = create();
        assertNotEquals(originId, localId);
        assertEquals(A, read(originId).originSite());
        assertEquals(B, read(localId).originSite());
        assertEquals(sent.resultHash(), read(localId).resultHash());
    }

    @Test
    void backfillQueuesOnlyNewTargetAndPreservesHistorySnapshotsAndRetryState() throws Exception {
        database.setReplicaUrls(Map.of(B, B_ID));
        final UUID saved = create();
        service.persist(database, saved, true);
        final UUID unsaved = service.storeQuery(database, "SELECT 'second' a", "SELECT 'second' a", SELECTED, "alice");
        // Legacy rows remain ineligible, just as in the ordinary enqueue path.
        sql("INSERT INTO qs_queries (query,query_normalized,is_persisted,query_hash) VALUES ('SELECT 1','SELECT 1',TRUE,'legacy')");
        sql("DELETE FROM qs_subset_outbox");
        final long history = count("qs_queries FOR SYSTEM_TIME ALL");
        final String payloads = string("SELECT GROUP_CONCAT(HEX(payload) ORDER BY query_id,row_no) FROM qs_subset_result_rows");
        final SubsetReplicationDto before = read(saved);
        database.setReplicaUrls(Map.of(B, B_ID, C, C_ID));
        replication.backfill(database, C);
        assertEquals(2, count("qs_subset_outbox"));
        assertEquals(0, count("qs_subset_outbox WHERE target_site='" + B + "'"));
        assertEquals(2, number("SELECT revision FROM qs_subset_outbox WHERE query_id='" + saved + "'"));
        assertEquals(1, number("SELECT revision FROM qs_subset_outbox WHERE query_id='" + unsaved + "'"));
        sql("UPDATE qs_subset_outbox SET attempts=3,last_error='offline',next_attempt='2099-01-01'");
        replication.backfill(database, "https://C.example:443/");
        assertEquals(2, count("qs_subset_outbox WHERE attempts=3 AND last_error='offline' AND next_attempt='2099-01-01'"));
        assertEquals(before, read(saved));
        assertEquals(history, count("qs_queries FOR SYSTEM_TIME ALL"));
        assertEquals(payloads, string("SELECT GROUP_CONCAT(HEX(payload) ORDER BY query_id,row_no) FROM qs_subset_result_rows"));
        assertEquals(2, count("qs_subset_results WHERE ready=TRUE"));

        final var client = mock(RestTemplate.class);
        publishedArtifact(client);
        when(client.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.noContent().build());
        sql("UPDATE qs_subset_outbox SET next_attempt=UTC_TIMESTAMP(6)");
        assertEquals(2, new SubsetReplicationDispatcher(null, replication, client, mapper,
                new SubsetResultService(mapper, json)).dispatch(database));
        verify(client, times(2)).exchange(eq(C + "/api/v1/database/" + C_ID + "/subset/replicate"),
                eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));
        assertEquals(0, count("qs_subset_outbox"));
        assertEquals(history, count("qs_queries FOR SYSTEM_TIME ALL"));
    }

    @Test
    void backfillAdvancesStaleOutboxWithoutTouchingOtherPeersOrRegressingNewerRevision() throws Exception {
        final UUID id = create();
        service.persist(database, id, true);
        sql("UPDATE qs_subset_outbox SET revision=1,attempts=4,last_error='offline',next_attempt='2099-01-01'");
        replication.backfill(database, C);
        assertEquals(1, count("qs_subset_outbox WHERE target_site='" + C + "' AND revision=2 AND next_attempt<'2099-01-01'"));
        assertEquals(1, count("qs_subset_outbox WHERE target_site='" + B + "' AND revision=1 AND next_attempt='2099-01-01'"));
        sql("UPDATE qs_subset_outbox SET revision=3,next_attempt='2099-01-01' WHERE target_site='" + C + "'");
        replication.backfill(database, C);
        assertEquals(1, count("qs_subset_outbox WHERE target_site='" + C + "' AND revision=3 AND next_attempt='2099-01-01'"));
    }

    @Test
    void backfillFailureIsAtomicAndDoesNotChangeCanonicalState() throws Exception {
        final UUID id = create();
        service.storeQuery(database, "SELECT 'second' a", "SELECT 'second' a", SELECTED, "alice");
        sql("DELETE FROM qs_subset_outbox");
        final long history = count("qs_queries FOR SYSTEM_TIME ALL");
        final var before = read(id);
        sql("CREATE TRIGGER fail_subset_outbox BEFORE INSERT ON qs_subset_outbox FOR EACH ROW "
                + "BEGIN IF EXISTS (SELECT 1 FROM qs_subset_outbox) THEN "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='second insert failure'; END IF; END");
        assertThrows(SQLException.class, () -> replication.backfill(database, C));
        assertEquals(0, count("qs_subset_outbox"));
        assertEquals(before, read(id));
        assertEquals(history, count("qs_queries FOR SYSTEM_TIME ALL"));
        assertEquals(2, count("qs_subset_results WHERE ready=TRUE"));
    }

    private SubsetReplicationService replication(String site) {
        return new SubsetReplicationService(mapper, json, new ReplicationPeers(A + "," + B + "," + C),
                Validation.buildDefaultValidatorFactory().getValidator(), site, new SubsetResultService(mapper, json));
    }

    private void publishedArtifact(RestTemplate client) {
        when(client.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                eq(at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto.Progress.class)))
                .thenReturn(ResponseEntity.ok(new at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto.Progress(true, 1, 0)));
        when(client.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.noContent().build());
    }

    private SubsetServiceMariaDbImpl service(SubsetReplicationService replication) {
        final var service = new SubsetServiceMariaDbImpl(null, Mappers.getMapper(DataMapper.class), mapper, null,
                mock(SubsetCacheRepository.class), null, replication);
        return service;
    }

    private UUID create() throws Exception {
        return service.storeQuery(database, "SELECT a FROM data", "SELECT 'original' a", SELECTED, "alice");
    }

    private void asReceiver() throws Exception {
        clearCurrentSite();
        database.setId(B_ID);
        database.setReplicaUrls(Map.of(A, A_ID, C, C_ID));
        replication = replication(B);
    }

    private void clearCurrentSite() throws Exception {
        sql("DELETE FROM qs_subset_result_rows");
        sql("DELETE FROM qs_subset_results");
        sql("DELETE FROM qs_subset_outbox");
        sql("DELETE FROM qs_queries");
    }

    private void failOutbox() throws Exception {
        sql("CREATE TRIGGER fail_subset_outbox BEFORE INSERT ON qs_subset_outbox FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'injected outbox failure'");
    }

    private SubsetReplicationDto read(UUID id) throws Exception {
        try (Connection connection = connect()) {
            return replication.read(connection, database, id);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url + "/subset_replication_test", "root", password);
    }

    private void sql(String sql) throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String table) throws Exception {
        return number("SELECT COUNT(*) FROM " + table);
    }

    private long number(String sql) throws Exception {
        return Long.parseLong(string(sql));
    }

    private String string(String sql) throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
