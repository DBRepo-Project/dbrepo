package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.SubsetCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryExecutionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStorePersistException;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetEndpoint;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.SubsetServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "SUBSET_SQL_TEST_PORT", matches = "[0-9]+")
class SubsetResultIntegrationTest {
    private static final String A = "https://a.example", B = "https://b.example";
    private static final UUID A_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID B_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private final String password = System.getenv("SUBSET_SQL_TEST_PASSWORD");
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("SUBSET_SQL_TEST_PORT");
    private final MariaDbMapper mapper = Mappers.getMapper(MariaDbMapper.class);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final SubsetResultService results = new SubsetResultService(mapper, json);
    private final Database database = Database.builder().id(A_ID).internalName("subset_replication_test")
            .isPublic(true).creationLocation(A).replicaUrls(Map.of(B, B_ID))
            .container(Container.builder().host("127.0.0.1").port(Integer.valueOf(System.getenv("SUBSET_SQL_TEST_PORT")))
                    .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build()).build();
    private SubsetReplicationService replication;
    private SubsetServiceMariaDbImpl service;

    @BeforeEach
    void schema() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "root", password); Statement s = connection.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS subset_replication_test");
            s.execute("CREATE DATABASE subset_replication_test CHARACTER SET utf8mb4");
            connection.setCatalog("subset_replication_test");
            s.execute(mapper.queryStoreCreateTableRawQuery());
        }
        site(A);
        service.upgradeQueryStore(database);
    }

    @Test
    void capturesExactTypesNullsDuplicatesAndReadsWithoutBaseData() throws Exception {
        sql("CREATE TABLE source (d DECIMAL(65,30), n INT, text_value LONGTEXT, raw_value BLOB, t DATETIME(6))");
        sql("INSERT INTO source VALUES (12345678901234567890.123456789012345678901234567890,NULL,"
                + "CONVERT(0xC3A9220A USING utf8mb4),0x00FF01,'2020-01-01 12:34:56.123456')");
        sql("INSERT INTO source SELECT * FROM source");
        UUID id = create("SELECT * FROM source");
        Subset before = service.findById(database, id);
        assertNotNull(before.getSnapshotHash());
        sql("DROP TABLE source");
        String body = read(before, 0, 10);
        var rows = json.readTree(body);
        assertEquals(2, rows.size());
        assertEquals(rows.get(0), rows.get(1));
        assertTrue(body.contains("12345678901234567890.123456789012345678901234567890"));
        assertTrue(rows.get(0).get("n").isNull());
        assertEquals("\u00e9\"\n", rows.get(0).get("text_value").textValue());
        assertArrayEquals(new byte[]{0, (byte)255, 1}, rows.get(0).get("raw_value").binaryValue());
        assertEquals("2020-01-01 12:34:56.123456", rows.get(0).get("t").textValue());
        assertEquals(1, json.readTree(read(before, 1, 1)).size());
        assertEquals("[]", read(before, 2, 1));
        try (var reader = results.open(database, before)) {
            var output = new ByteArrayOutputStream();
            reader.csv(output);
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("\"00ff01\""));
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("\u00e9\"\"\n"));
        }
        assertEquals(before.getResultHash(), service.findById(database, id).getResultHash());
    }

    @Test
    void chunksResumeAfterRestartAndPublishOnlyExactOriginalResult() throws Exception {
        UUID id = create("SELECT REPEAT('abcd',40000) AS a, CAST(NULL AS SIGNED) AS n");
        Subset original = service.findById(database, id);
        SubsetResultManifestDto manifest = manifest(id);
        List<Chunk> chunks = chunks(id);
        assertTrue(chunks.size() > 4);
        receiver();
        replication.receive(database, manifest.query());
        assertThrows(QueryExecutionException.class, () -> results.open(database, original));
        assertEquals(0, results.begin(database, manifest).nextOffset());
        assertThrows(ResponseStatusException.class, () -> results.publish(database, id));
        Chunk first = chunks.getFirst();
        append(results, id, first);
        assertEquals(first.bytes().length, append(results, id, first).nextOffset());
        assertThrows(ResponseStatusException.class, () -> append(results, id, chunks.get(2)));
        byte[] bad = first.bytes().clone(); bad[3] = '0';
        assertThrows(ResponseStatusException.class, () -> results.append(database, id, first.row(), first.offset(), first.length(),
                first.hash(), SubsetResultService.sha256(bad), bad));
        var restarted = new SubsetResultService(mapper, json);
        assertEquals(first.bytes().length, restarted.begin(database, manifest).nextOffset());
        for (Chunk chunk : chunks.subList(1, chunks.size())) append(restarted, id, chunk);
        assertThrows(QueryExecutionException.class, () -> restarted.open(database, original));
        restarted.publish(database, id);
        restarted.publish(database, id);
        assertTrue(restarted.begin(database, manifest).ready());
        assertEquals("abcd".repeat(40000), json.readTree(read(original, 0, 1)).get(0).get("a").textValue());
        assertEquals(original.getResultHash(), service.findById(database, id).getResultHash());
        assertEquals(A, service.findById(database, id).getCreationLocation());
        assertEquals(manifest.query().selectedAt(), service.findById(database, id).getExecution());
        var q = manifest.query();
        var replacement = new SubsetReplicationDto(q.queryId(), q.originSite(), q.senderSite(), q.senderDatabaseId(),
                q.query(), q.queryNormalized(), q.selectedAt(), q.persisted(), q.resultHash(), q.resultCount(),
                q.revision() + 1, "0".repeat(64));
        assertThrows(ResponseStatusException.class, () -> replication.receive(database, replacement));
        assertEquals(original.getSnapshotHash(), service.findById(database, id).getSnapshotHash());
    }

    @Test
    void fullArtifactVerificationPrecedesPaginationAndUsesSameReadSnapshot() throws Exception {
        UUID id = create("SELECT 1 AS a UNION ALL SELECT 2");
        Subset subset = service.findById(database, id);
        try (var reader = results.open(database, subset)) {
            sql("UPDATE qs_subset_result_rows SET payload='[\"V39\"]' WHERE row_no=1");
            var output = new ByteArrayOutputStream();
            reader.json(output, 1, 1);
            assertEquals(2, json.readTree(output.toByteArray()).get(0).get("a").intValue());
        }
        assertThrows(QueryExecutionException.class, () -> results.open(database, subset));
        assertEquals(subset.getResultHash(), service.findById(database, id).getResultHash());
    }

    @Test
    void existingHeadAndGetUseOnlyVerifiedArtifactsWithStablePages() throws Exception {
        UUID id = create("SELECT 5 AS a UNION ALL SELECT 6");
        var metadata = mock(MetadataService.class);
        when(metadata.getDatabase(database.getId())).thenReturn(database);
        var endpoint = new SubsetEndpoint(null, null, mapper, service, null, null, metadata, mock(EndpointValidator.class), null, json);
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("HEAD");
        var head = endpoint.getData(database.getId(), id, null, "application/json", request, null, 0L, 1L);
        assertEquals("2", head.getHeaders().getFirst("X-Count"));
        assertEquals("verified", head.getHeaders().getFirst("X-Integrity"));
        assertNull(head.getBody());
        when(request.getMethod()).thenReturn("GET");
        var get = endpoint.getData(database.getId(), id, null, "application/json", request, null, 1L, 1L);
        // Mutate after the handler verifies, before asynchronous output starts.
        sql("DELETE FROM qs_subset_result_rows");
        var output = new ByteArrayOutputStream();
        ((StreamingResponseBody) get.getBody()).writeTo(output);
        assertEquals(6, json.readTree(output.toByteArray()).get(0).get("a").intValue());
        assertThrows(QueryExecutionException.class, () -> endpoint.getData(database.getId(), id, null,
                "application/json", request, null, 0L, 1L));
        when(request.getMethod()).thenReturn("HEAD");
        assertThrows(QueryExecutionException.class, () -> endpoint.getData(database.getId(), id, null,
                "application/json", request, null, 0L, 1L));
    }

    @Test
    void emptyArtifactReplicatesAndPublishesWithoutChunks() throws Exception {
        UUID id = create("SELECT 1 AS a WHERE FALSE");
        Subset subset = service.findById(database, id);
        SubsetResultManifestDto manifest = manifest(id);
        receiver();
        replication.receive(database, manifest.query());
        results.begin(database, manifest);
        results.publish(database, id);
        assertEquals("[]", read(subset, 0, 10));
    }

    @Test
    void wrongOriginalFixityAndManifestNeverPublish() throws Exception {
        UUID id = create("SELECT 1 AS a");
        SubsetResultManifestDto original = manifest(id);
        List<Chunk> chunks = chunks(id);
        receiver();
        var q = original.query();
        String hash = "v2:" + "0".repeat(64);
        String snapshot = SubsetResultService.sha256("dbrepo:subset-artifact:v1:" + original.schemaJson() + ":"
                + original.orderHash() + ":" + hash + ":1");
        var badQuery = new SubsetReplicationDto(q.queryId(), q.originSite(), q.senderSite(), q.senderDatabaseId(),
                q.query(), q.queryNormalized(), q.selectedAt(), q.persisted(), hash, 1L, q.revision(), snapshot);
        replication.receive(database, badQuery);
        var badManifest = new SubsetResultManifestDto(badQuery, original.schemaJson(), original.orderHash());
        results.begin(database, badManifest);
        for (Chunk chunk : chunks) append(results, id, chunk);
        assertThrows(ResponseStatusException.class, () -> results.publish(database, id));
        assertThrows(ResponseStatusException.class, () -> results.begin(database, original));
        assertEquals(hash, service.findById(database, id).getResultHash());
    }

    @Test
    void persistenceRecaptureMustMatchOriginalAndDoesNotReplaceReference() throws Exception {
        sql("CREATE TABLE source(a INT)");
        sql("INSERT INTO source VALUES(1)");
        UUID id = create("SELECT * FROM source");
        Subset original = service.findById(database, id);
        sql("UPDATE qs_queries SET snapshot_hash=NULL");
        sql("DELETE FROM qs_subset_results");
        sql("DELETE FROM qs_subset_result_rows");
        sql("UPDATE source SET a=2");
        assertThrows(QueryStorePersistException.class, () -> service.persist(database, id, true));
        assertNull(service.findById(database, id).getSnapshotHash());
        assertEquals(original.getResultHash(), service.findById(database, id).getResultHash());
        sql("UPDATE source SET a=1");
        service.persist(database, id, true);
        assertEquals(original.getSnapshotHash(), service.findById(database, id).getSnapshotHash());
        assertEquals(original.getExecution(), service.findById(database, id).getExecution());
    }

    @Test
    void dispatcherResumesLostChunkResponseAndKeepsOutboxUntilPublish() throws Exception {
        UUID id = create("SELECT REPEAT('x',80000) AS a");
        var client = mock(org.springframework.web.client.RestTemplate.class);
        var progress = new java.util.concurrent.atomic.AtomicReference<>(new SubsetResultManifestDto.Progress(false, 0, 0));
        var loseChunk = new java.util.concurrent.atomic.AtomicBoolean(true);
        var losePublish = new java.util.concurrent.atomic.AtomicBoolean(true);
        var received = new ByteArrayOutputStream();
        when(client.exchange(anyString(), eq(org.springframework.http.HttpMethod.PUT), any(org.springframework.http.HttpEntity.class), eq(Void.class)))
                .thenReturn(org.springframework.http.ResponseEntity.noContent().build());
        when(client.exchange(anyString(), eq(org.springframework.http.HttpMethod.PUT), any(org.springframework.http.HttpEntity.class),
                eq(SubsetResultManifestDto.Progress.class))).thenAnswer(call -> {
                    String path = call.getArgument(0);
                    if (path.endsWith("/result")) return org.springframework.http.ResponseEntity.ok(progress.get());
                    org.springframework.http.HttpEntity<byte[]> entity = call.getArgument(2);
                    byte[] bytes = entity.getBody();
                    assertTrue(bytes.length <= SubsetResultService.CHUNK_SIZE);
                    assertEquals(SubsetResultService.sha256(bytes), entity.getHeaders().getFirst("X-Chunk-Hash"));
                    assertEquals(A, entity.getHeaders().getFirst("X-Subset-Sender"));
                    assertTrue(path.endsWith("/chunks/" + received.size()));
                    received.write(bytes);
                    long length = Long.parseLong(entity.getHeaders().getFirst("X-Row-Length"));
                    progress.set(new SubsetResultManifestDto.Progress(false, received.size() == length ? 1 : 0,
                            received.size() == length ? 0 : received.size()));
                    if (loseChunk.getAndSet(false)) throw new org.springframework.web.client.ResourceAccessException("lost chunk acknowledgement");
                    return org.springframework.http.ResponseEntity.ok(progress.get());
                });
        when(client.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(Void.class)))
                .thenAnswer(call -> {
                    assertEquals(1, progress.get().nextRow());
                    progress.set(new SubsetResultManifestDto.Progress(true, 1, 0));
                    if (losePublish.getAndSet(false)) throw new org.springframework.web.client.ResourceAccessException("lost publication acknowledgement");
                    return org.springframework.http.ResponseEntity.noContent().build();
                });
        assertEquals(0, new SubsetReplicationDispatcher(null, replication, client, mapper, results).dispatch(database));
        assertEquals(SubsetResultService.CHUNK_SIZE, received.size());
        sql("UPDATE qs_subset_outbox SET next_attempt=UTC_TIMESTAMP(6)");
        assertEquals(0, new SubsetReplicationDispatcher(null, replication, client, mapper, results).dispatch(database));
        try (var c = connect(); var s = c.createStatement(); var rows = s.executeQuery("SELECT COUNT(*) FROM qs_subset_outbox")) {
            assertTrue(rows.next()); assertEquals(1, rows.getInt(1));
        }
        sql("UPDATE qs_subset_outbox SET next_attempt=UTC_TIMESTAMP(6)");
        assertEquals(1, new SubsetReplicationDispatcher(null, replication, client, mapper, results).dispatch(database));
        try (var c = connect(); var s = c.createStatement(); var rows = s.executeQuery("SELECT row_hash FROM qs_subset_result_rows")) {
            assertTrue(rows.next()); assertEquals(rows.getString(1), SubsetResultService.sha256(received.toByteArray()));
        }
        assertNotNull(service.findById(database, id).getSnapshotHash());
    }

    private SubsetResultManifestDto manifest(UUID id) throws Exception {
        try (Connection connection = connect()) { return results.manifest(connection, replication.read(connection, database, id)); }
    }

    private List<Chunk> chunks(UUID id) throws Exception {
        var chunks = new ArrayList<Chunk>();
        try (Connection connection = connect(); PreparedStatement s = connection.prepareStatement(
                "SELECT row_no,row_hash,payload FROM qs_subset_result_rows WHERE query_id=? ORDER BY row_no")) {
            s.setString(1, id.toString());
            try (ResultSet rows = s.executeQuery()) {
                while (rows.next()) {
                    byte[] bytes = rows.getBytes(3);
                    for (int offset = 0; offset < bytes.length; offset += SubsetResultService.CHUNK_SIZE) {
                        chunks.add(new Chunk(rows.getLong(1), offset, bytes.length, rows.getString(2),
                                Arrays.copyOfRange(bytes, offset, Math.min(offset + SubsetResultService.CHUNK_SIZE, bytes.length))));
                    }
                }
            }
        }
        return chunks;
    }

    private SubsetResultManifestDto.Progress append(SubsetResultService target, UUID id, Chunk c) throws Exception {
        assertTrue(c.bytes().length <= SubsetResultService.CHUNK_SIZE);
        return target.append(database, id, c.row(), c.offset(), c.length(), c.hash(), SubsetResultService.sha256(c.bytes()), c.bytes());
    }

    private String read(Subset subset, long offset, long limit) throws Exception {
        try (var reader = results.open(database, subset)) {
            var output = new ByteArrayOutputStream(); reader.json(output, offset, limit); return output.toString(StandardCharsets.UTF_8);
        }
    }

    private void receiver() throws Exception {
        sql("DELETE FROM qs_subset_result_rows"); sql("DELETE FROM qs_subset_results");
        sql("DELETE FROM qs_subset_outbox"); sql("DELETE FROM qs_queries");
        database.setId(B_ID); database.setReplicaUrls(Map.of(A, A_ID)); site(B);
    }

    private void site(String site) {
        replication = new SubsetReplicationService(mapper, json, new ReplicationPeers(A + "," + B),
                Validation.buildDefaultValidatorFactory().getValidator(), site, results);
        service = new SubsetServiceMariaDbImpl(null, Mappers.getMapper(DataMapper.class), mapper, null,
                mock(SubsetCacheRepository.class), null, replication);
    }

    private UUID create(String query) throws Exception {
        return service.storeQuery(database, query, query, Instant.parse("2020-01-01T12:34:56.123456Z"), "alice");
    }

    private Connection connect() throws SQLException { return DriverManager.getConnection(url + "/subset_replication_test", "root", password); }
    private void sql(String sql) throws Exception { try (var c = connect(); var s = c.createStatement()) { s.execute(sql); } }
    private record Chunk(long row, long offset, long length, String hash, byte[] bytes) { }
}
