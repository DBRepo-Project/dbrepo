package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.service.TupleReplicationNotificationDispatcher;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.classic.Dataset;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spark CSV parsing and MariaDB transactions; only S3 transport is replaced with local files. */
@EnabledIfEnvironmentVariable(named = "INGEST_SQL_TEST_PORT", matches = "[0-9]+")
class IngestReplicationMariaDbIntegrationTest {
    private static final String DATABASE = "ingest_replication_test";
    private static final String ORIGIN = "https://origin.example";
    private static SparkSession spark;
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("INGEST_SQL_TEST_PORT");
    private final String password = System.getenv("INGEST_SQL_TEST_PASSWORD");
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final TupleReplicationOutboxServiceMariaDbImpl outbox =
            new TupleReplicationOutboxServiceMariaDbImpl(json);
    private final LocalStorage storage = new LocalStorage();
    @TempDir
    Path directory;
    private Database database;
    private Table table;
    private TableServiceMariaDbImpl service;
    private ReplicationServiceImpl replication;
    private TupleReplicationNotificationDispatcher dispatcher;

    @BeforeAll
    static void startSpark() {
        spark = SparkSession.builder().master("local[2]").appName("ingest-replication-test")
                .config("spark.ui.enabled", "false")
                .config("spark.driver.host", "127.0.0.1")
                .config("spark.driver.bindAddress", "127.0.0.1")
                .getOrCreate();
    }

    @AfterAll
    static void stopSpark() {
        if (spark != null) {
            spark.stop();
        }
    }

    @BeforeEach
    void setup() throws Exception {
        database = Database.builder().id(UUID.randomUUID()).internalName(DATABASE).creationLocation(ORIGIN)
                .replicaUrls(Map.of("https://replica.example", UUID.randomUUID()))
                .container(Container.builder().id(UUID.randomUUID()).internalName("ingest-test")
                        .host("127.0.0.1").port(Integer.valueOf(System.getenv("INGEST_SQL_TEST_PORT")))
                        .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build())
                .build();
        table = Table.builder().id(UUID.randomUUID()).internalName("samples")
                .columns(List.of(column("replication_key", ColumnType.VARCHAR), column("sample_value", ColumnType.INT),
                        column("payload", ColumnType.LONGBLOB), column("amount", ColumnType.DECIMAL))).build();
        dispatcher = mock(TupleReplicationNotificationDispatcher.class);
        replication = new ReplicationServiceImpl(outbox, dispatcher);
        ReflectionTestUtils.setField(replication, "baseUrl", ORIGIN);
        service = new TableServiceMariaDbImpl(null, Mappers.getMapper(MariaDbMapper.class), null, storage,
                new LocalCsvService(), replication);
        ReflectionTestUtils.setField(service, "baseUrl", ORIGIN);
        try (Connection root = DriverManager.getConnection(url, "root", password);
             var statement = root.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
            statement.execute("CREATE DATABASE " + DATABASE);
            statement.execute("CREATE TABLE " + DATABASE + ".samples (replication_key VARCHAR(36) PRIMARY KEY, "
                    + "sample_value INT NOT NULL UNIQUE, payload LONGBLOB, amount DECIMAL(38,18)) "
                    + "ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
    }

    @Test
    void importsAllRowsWithGeneratedKeysExactValuesAndOneDurableEventEach() throws Exception {
        final ImportDto input = csv("sample_value,payload,amount\n"
                + "1,\"literal,bytes\",12345678901234567890.123456789012345678\n2,,\n");
        doAnswer(invocation -> {
            assertEquals(2, count("tuple_replication_notification_outbox"));
            return null;
        }).when(dispatcher).dispatchAsync(eq(database), anyList());
        service.importDataset(database, table, input);

        assertEquals(2, count("samples"));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(2, count("(SELECT DISTINCT replication_key FROM samples) AS identities"));
        final var events = outbox.findAll(database);
        assertEquals(2, events.size());
        verify(dispatcher).dispatchAsync(eq(database), argThat(ids -> ids.size() == 2));
        for (var event : events) {
            assertEquals("POST", event.getHttpMethod().name());
            final var tuple = json.readTree(event.getPayloadJson()).get("tuple");
            assertNotNull(UUID.fromString(tuple.get("replicationKey").asText()));
            assertFalse(tuple.get("insertedAt").isNull());
            assertTrue(tuple.get("deletedAt").isNull());
            assertEquals(tuple.get("replicationKey"), tuple.get("data").get("replication_key"));
            if (tuple.get("data").get("sample_value").asInt() == 1) {
                assertArrayEquals("literal,bytes".getBytes(StandardCharsets.UTF_8),
                        tuple.get("data").get("payload").binaryValue());
                assertEquals(new BigDecimal("12345678901234567890.123456789012345678"),
                        tuple.get("data").get("amount").decimalValue());
            } else {
                assertTrue(tuple.get("data").get("payload").isNull());
                assertTrue(tuple.get("data").get("amount").isNull());
            }
        }
        assertFalse(Files.exists(Path.of(input.getLocation())));
        assertEquals(List.of(), storage.reads);
        assertNoStagingTables();
    }

    @Test
    void preservesSuppliedKeysAndGeneratesNullKeysWithoutReusingPreviousRow() throws Exception {
        service.importDataset(database, table, csv("replication_key,sample_value,payload,amount\n"
                + "supplied,1,,\n,2,,\n,3,,\n"));
        assertEquals(1, count("samples WHERE replication_key = 'supplied'"));
        assertEquals(3, count("(SELECT DISTINCT replication_key FROM samples) AS identities"));
        assertEquals(3, outbox.findAll(database).size());
    }

    @Test
    void duplicateCsvRowsUpdateValuesButRetainTheFirstReplicationIdentity() throws Exception {
        final ImportDto input = csv("sample_value,payload,amount\n1,first,\n2,second,\n1,duplicate,\n");
        service.importDataset(database, table, input);
        assertEquals(2, count("samples"));
        assertEquals(1, count("samples WHERE sample_value = 1 AND payload = 'duplicate'"));
        final var events = outbox.findAll(database);
        assertEquals(3, events.size());
        final List<String> duplicateKeys = new ArrayList<>();
        final List<String> payloads = new ArrayList<>();
        for (var event : events) {
            final var tuple = json.readTree(event.getPayloadJson()).get("tuple");
            assertEquals("POST", event.getHttpMethod().name());
            if (tuple.get("data").get("sample_value").asInt() == 1) {
                duplicateKeys.add(tuple.get("replicationKey").asText());
                payloads.add(new String(tuple.get("data").get("payload").binaryValue(), StandardCharsets.UTF_8));
            }
        }
        assertEquals(2, duplicateKeys.size());
        assertEquals(duplicateKeys.getFirst(), duplicateKeys.getLast());
        assertTrue(payloads.containsAll(List.of("first", "duplicate")));
        assertStoredBlob(duplicateKeys.getFirst(), "duplicate".getBytes(StandardCharsets.UTF_8));
        assertFalse(Files.exists(Path.of(input.getLocation())));
    }

    @Test
    void invalidLateRowRollsBackEarlierUpsertsEventsAndHistoryAndRetainsUpload() throws Exception {
        final ImportDto input = csv("sample_value,payload,amount\n1,first,\n1,updated,\n2,second,\n,invalid,\n");
        assertThrows(MalformedException.class, () -> service.importDataset(database, table, input));
        assertRolledBack(input);
        verifyNoInteractions(dispatcher);
    }

    @Test
    void reimportRetainsExistingKeyForPrimaryAndAlternateUniqueKeyMatches() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO samples VALUES ('original', 1, 'before', 1)");
        }
        service.importDataset(database, table, csv("replication_key,sample_value,payload,amount\n"
                + "replacement,1,alternate,12345678901234567890.123456789012345678\n"
                + "original,2,primary,99999999999999999999.123456789012345678\n"));
        assertEquals(1, count("samples"));
        assertEquals(1, count("samples WHERE replication_key = 'original' AND sample_value = 2 "
                + "AND amount = 99999999999999999999.123456789012345678"));
        assertStoredBlob("original", "primary".getBytes(StandardCharsets.UTF_8));
        final var events = outbox.findAll(database);
        assertEquals(2, events.size());
        for (var event : events) {
            final var tuple = json.readTree(event.getPayloadJson()).get("tuple");
            assertEquals("original", tuple.get("replicationKey").asText());
            assertEquals("original", tuple.get("data").get("replication_key").asText());
            final BigDecimal expected = tuple.get("data").get("sample_value").asInt() == 1
                    ? new BigDecimal("12345678901234567890.123456789012345678")
                    : new BigDecimal("99999999999999999999.123456789012345678");
            assertEquals(expected, tuple.get("data").get("amount").decimalValue());
        }
    }

    @Test
    void failedOutboxRollsBackAnExistingRowReplacementAndItsHistory() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO samples VALUES ('original', 1, 'before', 1)");
            outbox.ensureTableExists(connection);
            statement.execute("CREATE TRIGGER fail_ingest_outbox BEFORE INSERT ON tuple_replication_notification_outbox "
                    + "FOR EACH ROW BEGIN IF JSON_VALUE(NEW.payload, '$.tuple.data.sample_value') = 2 THEN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'injected ingest outbox failure'; END IF; END");
        }
        final ImportDto input = csv("sample_value,payload,amount\n1,updated,2\n2,inserted,3\n");
        assertThrows(MalformedException.class, () -> service.importDataset(database, table, input));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("samples WHERE replication_key = 'original' AND sample_value = 1 AND amount = 1"));
        assertStoredBlob("original", "before".getBytes(StandardCharsets.UTF_8));
        assertEquals(0, outbox.findAll(database).size());
        assertTrue(Files.exists(Path.of(input.getLocation())));
    }

    @Test
    void lateOutboxFailureRollsBackEarlierSuccessfulRowsAndEvents() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            outbox.ensureTableExists(connection);
            statement.execute("CREATE TRIGGER fail_ingest_outbox BEFORE INSERT ON tuple_replication_notification_outbox "
                    + "FOR EACH ROW BEGIN IF JSON_VALUE(NEW.payload, '$.tuple.data.sample_value') = 2 THEN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'injected ingest outbox failure'; END IF; END");
        }
        final ImportDto input = csv("sample_value,payload,amount\n1,first,\n2,second,\n");
        assertThrows(MalformedException.class, () -> service.importDataset(database, table, input));
        assertRolledBack(input);
    }

    @Test
    void laterSparkPartitionFailureRollsBackEarlierRowsAndEvents() throws Exception {
        final ImportDto input = csv("sample_value,payload,amount\n1,,\n");
        final var failingCsv = new LocalCsvService() {
            @Override
            public Dataset<Row> getCsv(List<String> columns, String key, String delimiter, Boolean withHeader) {
                return (Dataset<Row>) spark.range(0, 2, 1, 2).selectExpr(
                        "CAST(NULL AS STRING) AS replication_key",
                        "CASE WHEN id = 0 THEN '1' ELSE CAST(raise_error('injected partition failure') AS STRING) END AS sample_value",
                        "CAST(NULL AS STRING) AS payload", "CAST(NULL AS STRING) AS amount");
            }
        };
        service = new TableServiceMariaDbImpl(null, Mappers.getMapper(MariaDbMapper.class), null, storage,
                failingCsv, replication);
        ReflectionTestUtils.setField(service, "baseUrl", ORIGIN);
        assertThrows(MalformedException.class, () -> service.importDataset(database, table, input));
        assertRolledBack(input);
    }

    @Test
    void importsWithoutPeersStillGenerateKnownKeysWithoutAnOutbox() throws Exception {
        database.setReplicaUrls(Map.of());
        service.importDataset(database, table, csv("sample_value,payload,amount\n1,,\n2,,\n"));
        assertEquals(2, count("(SELECT DISTINCT replication_key FROM samples) AS identities"));
        assertEquals(0, count("information_schema.tables WHERE table_schema = '" + DATABASE
                + "' AND table_name = 'tuple_replication_notification_outbox'"));
    }

    @Test
    void nonReplicatedTableWithoutKeyRetainsDatabaseCsvConversions() throws Exception {
        database.setReplicaUrls(Map.of());
        database.setCreationLocation(null);
        table.setColumns(table.getColumns().stream().filter(c -> !"replication_key".equals(c.getInternalName())).toList());
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE samples");
            statement.execute("CREATE TABLE samples (sample_value INT NOT NULL UNIQUE, payload LONGBLOB, "
                    + "amount DECIMAL(38,18)) ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
        service.importDataset(database, table, csv("sample_value,payload,amount\n1,literal,1.25\n1,replaced,2.5\n"));
        assertEquals(1, count("samples WHERE sample_value = 1 AND amount = 2.5 AND payload = 'replaced'"));
    }

    @Test
    void rejectsRemoteDatabaseAndTableBeforeReadingCsv() throws Exception {
        final ImportDto missingUpload = ImportDto.builder().location(directory.resolve("absent.csv").toString())
                .header(true).separator(',').build();
        database.setCreationLocation("https://remote.example");
        assertThrows(QueryMalformedException.class, () -> service.importDataset(database, table, missingUpload));
        database.setCreationLocation(ORIGIN);
        table.setCreationLocation("https://remote.example");
        assertThrows(QueryMalformedException.class, () -> service.importDataset(database, table, missingUpload));
        assertEquals(0, count("samples"));
    }

    @Test
    void replicatedTableMissingIdentityIsRejectedBeforeIngestion() {
        table.setColumns(table.getColumns().stream().filter(c -> !"replication_key".equals(c.getInternalName())).toList());
        assertThrows(TableMalformedException.class, () -> service.importDataset(database, table,
                ImportDto.builder().location("absent.csv").header(true).separator(',').build()));
    }

    @Test
    void malformedColumnCountLeavesTargetAndUploadUntouched() throws Exception {
        final ImportDto input = csv("wrong\n1\n");
        assertThrows(MalformedException.class, () -> service.importDataset(database, table, input));
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
        assertTrue(Files.exists(Path.of(input.getLocation())));
        assertNoStagingTables();
    }

    @Test
    void emptyCsvHasNoEvents() throws Exception {
        final ImportDto input = csv("replication_key,sample_value,payload,amount\n");
        service.importDataset(database, table, input);
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(0, outbox.findAll(database).size());
        assertFalse(Files.exists(Path.of(input.getLocation())));
    }

    @Test
    void concurrentImportsRemainIndependent() throws Exception {
        final ImportDto first = csv("sample_value,payload,amount\n1,first,1\n2,first,2\n");
        final ImportDto second = csv("sample_value,payload,amount\n3,second,3\n4,second,4\n");
        importConcurrently(first, second);
        assertEquals(4, count("samples"));
        assertEquals(4, outbox.findAll(database).size());
        assertEquals(2, count("samples WHERE sample_value IN (1,2) AND payload = 'first'"));
        assertEquals(2, count("samples WHERE sample_value IN (3,4) AND payload = 'second'"));
        assertNoStagingTables();
    }

    @Test
    void concurrentImportsOfTheSameUniqueKeyRetainOneIdentity() throws Exception {
        importConcurrently(csv("sample_value,payload,amount\n1,first,1\n"),
                csv("sample_value,payload,amount\n1,second,2\n"));
        assertEquals(1, count("samples"));
        final var events = outbox.findAll(database);
        assertEquals(2, events.size());
        final String key = json.readTree(events.getFirst().getPayloadJson()).get("tuple").get("replicationKey").asText();
        assertEquals(key, json.readTree(events.getLast().getPayloadJson()).get("tuple").get("replicationKey").asText());
        assertNoStagingTables();
    }

    @Test
    void keyOnlyTableSupportsNoOpDuplicateImports() throws Exception {
        table.setColumns(List.of(column("replication_key", ColumnType.VARCHAR)));
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE samples");
            statement.execute("CREATE TABLE samples (replication_key VARCHAR(36) PRIMARY KEY) "
                    + "ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
        service.importDataset(database, table, csv("replication_key\nidentity\nidentity\n"));
        assertEquals(1, count("samples"));
        assertEquals(2, outbox.findAll(database).size());
    }

    private void importConcurrently(ImportDto first, ImportDto second) throws Exception {
        final CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            final var a = workers.submit(() -> {
                start.await();
                service.importDataset(database, table, first);
                return null;
            });
            final var b = workers.submit(() -> {
                start.await();
                service.importDataset(database, table, second);
                return null;
            });
            start.countDown();
            a.get(60, TimeUnit.SECONDS);
            b.get(60, TimeUnit.SECONDS);
        }
    }

    @Test
    void headerlessCsvCanOmitReplicationKey() throws Exception {
        final ImportDto input = csv("1,first,1.25\n2,second,2.5\n");
        input.setHeader(false);
        service.importDataset(database, table, input);
        assertEquals(2, count("samples"));
        assertEquals(2, outbox.findAll(database).size());
    }

    @ParameterizedTest
    @EnumSource(value = ColumnType.class, names = {"BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB"})
    void tupleObjectKeyIsReadOnceAndBlobAndDecimalRemainExact(ColumnType type) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE samples");
            statement.execute("CREATE TABLE samples (replication_key VARCHAR(36) PRIMARY KEY, "
                    + "sample_value INT NOT NULL UNIQUE, payload " + type.name() + ", amount DECIMAL(38,18)) "
                    + "ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
        table.getColumns().get(2).setColumnType(type);
        final byte[] bytes = new byte[]{0, 1, 2, 127, (byte) 128, (byte) 255, 0};
        final Path object = directory.resolve("source-blob");
        storage.putObject(object.toString(), bytes);
        final BigDecimal amount = new BigDecimal("12345678901234567890.123456789012345678");
        final TupleDto input = tuple("blob", 1, object.toString(), amount.toPlainString());

        final var result = service.createTupleWithTimestamps(database, table, input);

        assertEquals(List.of(object.toString()), storage.reads);
        assertEquals(object.toString(), input.getData().get("payload"));
        assertArrayEquals(bytes, (byte[]) result.getData().get("payload"));
        assertEquals(amount, result.getData().get("amount"));
        final var event = json.readTree(outbox.findAll(database).getFirst().getPayloadJson()).get("tuple");
        assertArrayEquals(bytes, event.get("data").get("payload").binaryValue());
        assertEquals(amount, event.get("data").get("amount").decimalValue());
        assertStoredBlob("blob", bytes);
    }

    @Test
    void byteArraysSurviveSourceCreateUpdateDeleteAndBlobLookupKeys() throws Exception {
        final byte[] before = new byte[]{0, (byte) 255, 1};
        final byte[] after = new byte[]{(byte) 254, 0, 2};
        service.createTuple(database, table, tuple("bytes", 1, before, null));
        service.updateTuple(database, table, TupleUpdateDto.builder().keys(Map.of("payload", before))
                .data(Map.of("payload", after, "amount", "99999999999999999999.123456789012345678")).build());
        assertStoredBlob("bytes", after);
        service.deleteTuple(database, table, TupleDeleteDto.builder().keys(Map.of("payload", after)).build());

        assertEquals(List.of(), storage.reads);
        assertEquals(0, count("samples"));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
        final var events = outbox.findAll(database);
        assertEquals(3, events.size());
        for (var event : events) {
            final var tuple = json.readTree(event.getPayloadJson()).get("tuple");
            final boolean created = event.getHttpMethod().name().equals("POST");
            assertArrayEquals(created ? before : after, tuple.get("data").get("payload").binaryValue());
            if (!created) {
                assertEquals(new BigDecimal("99999999999999999999.123456789012345678"),
                        tuple.get("data").get("amount").decimalValue());
            }
        }
    }

    @Test
    void plainTupleCreateHandlesObjectKeysBytesEmptyAndNullBlobs() throws Exception {
        database.setReplicaUrls(Map.of());
        final byte[] bytes = new byte[]{0, (byte) 255, 2};
        final Path object = directory.resolve("plain-blob");
        storage.putObject(object.toString(), bytes);
        service.createTuple(database, table, tuple("object", 1, object.toString(), null));
        service.createTuple(database, table, tuple("bytes", 2, bytes, null));
        service.createTuple(database, table, tuple("empty", 3, new byte[0], null));
        service.createTuple(database, table, tuple("null", 4, null, null));
        assertEquals(List.of(object.toString()), storage.reads);
        assertStoredBlob("object", bytes);
        assertStoredBlob("bytes", bytes);
        assertStoredBlob("empty", new byte[0]);
        assertStoredBlob("null", null);
    }

    @Test
    void decodedWireBlobsCanBeUpsertedWithoutLocalStorageOrRefanout() throws Exception {
        final byte[] bytes = new byte[]{0, 127, (byte) 128, (byte) 255};
        final BigDecimal amount = new BigDecimal("12345678901234567890.123456789012345678");
        service.createTuple(database, table, tuple("wire", 1, bytes, amount));
        final var event = outbox.findAll(database).getFirst();
        final DataReplicationDto wire = json.readValue(event.getPayloadJson(), DataReplicationDto.class);
        final Map<String, Object> values = wire.getTuple().getData();
        assertInstanceOf(String.class, values.get("payload"));
        // The receiver endpoint must perform this decoding before calling tuple APIs.
        values.put("payload", Base64.getDecoder().decode((String) values.get("payload")));
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE received LIKE samples");
        }
        database.setCreationLocation("https://remote.example");
        table.setInternalName("received");
        service.upsertTupleWithTimestamps(database, table, TupleDto.builder().data(values).build());
        assertStoredBlob("wire", bytes);
        assertEquals(amount, service.getReplicationData(database, table, 0, 10, ORIGIN)
                .getTuples().getFirst().getData().get("amount"));
        values.put("payload", new byte[0]);
        service.upsertTupleWithTimestamps(database, table, TupleDto.builder().data(values).build());
        assertStoredBlob("wire", new byte[0]);
        assertEquals(List.of(), storage.reads);
        assertEquals(1, outbox.findAll(database).size());
    }

    @Test
    void missingBlobObjectAndInvalidDecimalLeaveNoDataOrEvents() throws Exception {
        final String missing = directory.resolve("missing-blob").toString();
        assertThrows(StorageNotFoundException.class, () -> service.createTuple(database, table,
                tuple("missing", 1, missing, null)));
        assertThrows(NumberFormatException.class, () -> service.createTuple(database, table,
                tuple("bad-decimal", 2, new byte[]{0}, "not-a-decimal")));
        assertEquals(List.of(missing), storage.reads);
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(0, outbox.findAll(database).size());
    }

    @Test
    void sourceBootstrapExtractsExactDecimalBlobAndLongTextTypes() throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE samples");
            statement.execute("CREATE TABLE samples (replication_key VARCHAR(36) PRIMARY KEY, "
                    + "sample_value INT NOT NULL UNIQUE, payload LONGBLOB, amount DECIMAL(38,18), "
                    + "large_text LONGTEXT) ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
        table.setColumns(new ArrayList<>(table.getColumns()));
        table.getColumns().add(column("large_text", ColumnType.LONGTEXT));
        final byte[] bytes = new byte[131072];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        final BigDecimal amount = new BigDecimal("-12345678901234567890.123456789012345678");
        final String text = "quoted \"text\", \\ \u0000 \u20ac\n".repeat(8192);
        final TupleDto input = tuple("bootstrap", 1, bytes, amount);
        input.getData().put("large_text", text);
        service.createTuple(database, table, input);

        final Map<String, Object> extracted = service.getReplicationData(database, table, 0, 10, ORIGIN)
                .getTuples().getFirst().getData();
        assertInstanceOf(byte[].class, extracted.get("payload"));
        assertInstanceOf(BigDecimal.class, extracted.get("amount"));
        assertInstanceOf(String.class, extracted.get("large_text"));
        assertArrayEquals(bytes, (byte[]) extracted.get("payload"));
        assertEquals(amount, extracted.get("amount"));
        assertEquals(text, extracted.get("large_text"));
        final var wire = json.readTree(outbox.findAll(database).getFirst().getPayloadJson()).get("tuple").get("data");
        assertArrayEquals(bytes, wire.get("payload").binaryValue());
        assertEquals(amount, wire.get("amount").decimalValue());
        assertEquals(text, wire.get("large_text").asText());
        assertEquals(List.of(), storage.reads);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nullUpdateLookupKeysDoNotConsumeParameters(boolean replicated) throws Exception {
        if (!replicated) {
            database.setReplicaUrls(Map.of());
        }
        service.createTuple(database, table, tuple("null", 1, null, null));
        service.createTuple(database, table, tuple("other", 2, new byte[]{1}, null));
        final Map<String, Object> nullFirst = new LinkedHashMap<>();
        nullFirst.put("payload", null);
        nullFirst.put("sample_value", 1);
        service.updateTuple(database, table, TupleUpdateDto.builder().keys(nullFirst)
                .data(Map.of("amount", "12345678901234567890.123456789012345678")).build());
        assertEquals(1, count("samples WHERE sample_value = 1 AND amount = 12345678901234567890.123456789012345678"));
        final Map<String, Object> nullLast = new LinkedHashMap<>();
        nullLast.put("sample_value", 1);
        nullLast.put("payload", null);
        final Map<String, Object> nullAssignment = new LinkedHashMap<>();
        nullAssignment.put("amount", null);
        service.updateTuple(database, table, TupleUpdateDto.builder().keys(nullLast).data(nullAssignment).build());
        assertEquals(2, count("samples WHERE amount IS NULL"));
        assertEquals(4, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(replicated ? 4 : 0, outbox.findAll(database).size());
        assertEquals(List.of(), storage.reads);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nullableDeleteKeysUseTheSharedExactDecimalBinder(boolean replicated) throws Exception {
        if (!replicated) {
            database.setReplicaUrls(Map.of());
        }
        final BigDecimal amount = new BigDecimal("12345678901234567890.123456789012345678");
        service.createTuple(database, table, tuple("first", 1, null, amount));
        service.createTuple(database, table, tuple("second", 2, new byte[]{1}, null));
        service.createTuple(database, table, tuple("keep", 3, new byte[]{1}, amount));
        final Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("payload", null);
        keys.put("amount", amount.toPlainString());
        service.deleteTuple(database, table, TupleDeleteDto.builder().keys(keys).build());
        keys.clear();
        keys.put("sample_value", 2);
        keys.put("amount", null);
        service.deleteTuple(database, table, TupleDeleteDto.builder().keys(keys).build());

        assertEquals(1, count("samples"));
        assertEquals(1, count("samples WHERE replication_key = 'keep'"));
        assertEquals(3, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(replicated ? 5 : 0, outbox.findAll(database).size());
        assertEquals(List.of(), storage.reads);
    }

    @Test
    void temporalAndBinarySourceEventsRoundTripThroughJsonAndJdbcExactly() throws Exception {
        createPrecisionTable();
        service = serviceInSessionZone("+00:00");
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+00:00'");
            statement.execute("INSERT INTO samples VALUES ('precision', '2026-10-03 12:34:56.123456', "
                    + "'2026-01-02 03:04:05.111111', '-123:45:56.987654', '2026-01-02', X'A5', X'00FF01', X'80FE00')");
        }
        service.updateTuple(database, table, TupleUpdateDto.builder().keys(Map.of("replication_key", "precision"))
                .data(Map.of("local_at", "2026-01-02 03:04:05.654321")).build());
        final String payload = outbox.findAll(database).getFirst().getPayloadJson();
        final JsonNode data = json.readTree(payload).get("tuple").get("data");
        assertPrecision(data);
        assertArrayEquals(new byte[]{(byte) 0xA5}, data.get("flags").binaryValue());
        assertArrayEquals(new byte[]{0, (byte) 0xFF, 1, 0}, data.get("fixed_bytes").binaryValue());
        assertArrayEquals(new byte[]{(byte) 0x80, (byte) 0xFE, 0}, data.get("variable_bytes").binaryValue());

        final var decoded = json.readValue(payload, DataReplicationDto.class).getTuple().getData();
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+00:00'");
            statement.execute("CREATE TABLE received LIKE samples");
            try (var insert = connection.prepareStatement("INSERT INTO received VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                int index = 1;
                for (Column column : table.getColumns()) {
                    final Object value = decoded.get(column.getInternalName());
                    if (List.of(ColumnType.BIT, ColumnType.BINARY, ColumnType.VARBINARY).contains(column.getColumnType())) {
                        insert.setBytes(index++, Base64.getDecoder().decode((String) value));
                    } else {
                        insert.setString(index++, (String) value);
                    }
                }
                insert.executeUpdate();
            }
            try (var rows = statement.executeQuery("SELECT * FROM received")) {
                assertTrue(rows.next());
                for (Column column : table.getColumns()) {
                    final String name = column.getInternalName();
                    if (List.of(ColumnType.BIT, ColumnType.BINARY, ColumnType.VARBINARY).contains(column.getColumnType())) {
                        assertArrayEquals(data.get(name).binaryValue(), rows.getBytes(name));
                    } else {
                        assertEquals(data.get(name).asText(), rows.getString(name));
                    }
                }
            }
        }
        assertEquals(List.of(), storage.reads);
    }

    @Test
    void bootstrapForcesUtcAndPreservesTemporalFractionsAndBinaryTypes() throws Exception {
        createPrecisionTable();
        final String rowStart;
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+00:00'");
            statement.execute("INSERT INTO samples VALUES ('precision', '2026-10-03 12:34:56.123456', "
                    + "'2026-01-02 03:04:05.654321', '-123:45:56.987654', '2026-01-02', X'A5', X'00FF01', X'80FE00')");
            try (var rows = statement.executeQuery("SELECT ROW_START FROM samples")) {
                assertTrue(rows.next());
                rowStart = rows.getString(1);
            }
        }
        service = serviceInSessionZone("+05:45");
        try (var pool = service.getDataSource(database); var connection = pool.getConnection();
             var statement = connection.createStatement(); var row = statement.executeQuery("SELECT @@session.time_zone")) {
            assertTrue(row.next());
            assertEquals("+05:45", row.getString(1));
        }
        final var tuple = service.getReplicationData(database, table, 0, 10, ORIGIN).getTuples().getFirst();
        assertEquals(Instant.parse(rowStart.replace(' ', 'T') + "Z"), tuple.getInsertedAt());
        for (String name : List.of("observed_at", "local_at", "duration", "calendar_day")) {
            assertInstanceOf(String.class, tuple.getData().get(name));
        }
        for (String name : List.of("flags", "fixed_bytes", "variable_bytes")) {
            assertInstanceOf(byte[].class, tuple.getData().get(name));
        }
        final JsonNode wire = json.readTree(json.writeValueAsString(tuple)).get("data");
        assertPrecision(wire);
        assertArrayEquals(new byte[]{(byte) 0xA5}, wire.get("flags").binaryValue());
        assertArrayEquals(new byte[]{0, (byte) 0xFF, 1, 0}, wire.get("fixed_bytes").binaryValue());
        assertArrayEquals(new byte[]{(byte) 0x80, (byte) 0xFE, 0}, wire.get("variable_bytes").binaryValue());
    }

    @Test
    void csvReturningPreservesMicrosecondsAndBitBytes() throws Exception {
        createPrecisionTable();
        service = serviceInSessionZone("+00:00");
        service.importDataset(database, table, csv("replication_key,observed_at,local_at,duration,calendar_day,flags,fixed_bytes,variable_bytes\n"
                + "precision,2026-10-03 12:34:56.123456,2026-01-02 03:04:05.654321,-123:45:56.987654,2026-01-02,A,ab,cd\n"));
        final JsonNode data = json.readTree(outbox.findAll(database).getFirst().getPayloadJson()).get("tuple").get("data");
        assertPrecision(data);
        assertArrayEquals(new byte[]{'A'}, data.get("flags").binaryValue());
        assertArrayEquals(new byte[]{'a', 'b', 0, 0}, data.get("fixed_bytes").binaryValue());
        assertArrayEquals(new byte[]{'c', 'd'}, data.get("variable_bytes").binaryValue());
    }

    private void createPrecisionTable() throws Exception {
        table.setColumns(List.of(column("replication_key", ColumnType.VARCHAR), column("observed_at", ColumnType.TIMESTAMP),
                column("local_at", ColumnType.DATETIME), column("duration", ColumnType.TIME), column("calendar_day", ColumnType.DATE),
                column("flags", ColumnType.BIT), column("fixed_bytes", ColumnType.BINARY), column("variable_bytes", ColumnType.VARBINARY)));
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE samples");
            statement.execute("CREATE TABLE samples (replication_key VARCHAR(36) PRIMARY KEY, observed_at TIMESTAMP(6) NULL, "
                    + "local_at DATETIME(6), duration TIME(6), calendar_day DATE, flags BIT(8), fixed_bytes BINARY(4), "
                    + "variable_bytes VARBINARY(8)) ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
    }

    private void assertPrecision(JsonNode data) {
        assertEquals("2026-10-03 12:34:56.123456", data.get("observed_at").asText());
        assertEquals("2026-01-02 03:04:05.654321", data.get("local_at").asText());
        assertEquals("-123:45:56.987654", data.get("duration").asText());
        assertEquals("2026-01-02", data.get("calendar_day").asText());
    }

    private TableServiceMariaDbImpl serviceInSessionZone(String zone) {
        final var result = new TableServiceMariaDbImpl(null, Mappers.getMapper(MariaDbMapper.class), null, storage,
                new LocalCsvService(), replication) {
            @Override
            public ComboPooledDataSource getDataSource(Database target) {
                final var pool = super.getDataSource(target);
                pool.setJdbcUrl(pool.getJdbcUrl() + "?sessionVariables=time_zone='" + zone + "'");
                return pool;
            }
        };
        ReflectionTestUtils.setField(result, "baseUrl", ORIGIN);
        return result;
    }

    private TupleDto tuple(String key, int sample, Object blob, Object amount) {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("replication_key", key);
        values.put("sample_value", sample);
        values.put("payload", blob);
        values.put("amount", amount);
        return TupleDto.builder().data(values).build();
    }

    private void assertStoredBlob(String key, byte[] expected) throws Exception {
        try (Connection connection = connection(); var statement = connection.prepareStatement(
                "SELECT payload FROM " + table.getInternalName() + " WHERE replication_key = ?")) {
            statement.setString(1, key);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertArrayEquals(expected, rows.getBytes(1));
            }
        }
    }

    private Column column(String name, ColumnType type) {
        return Column.builder().internalName(name).columnType(type).build();
    }

    private ImportDto csv(String contents) throws IOException {
        final Path file = Files.createTempFile(directory, "ingest-", ".csv");
        Files.writeString(file, contents);
        return ImportDto.builder().location(file.toString()).header(true).separator(',').build();
    }

    private void assertRolledBack(ImportDto input) throws Exception {
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(0, outbox.findAll(database).size());
        assertTrue(Files.exists(Path.of(input.getLocation())));
        assertNoStagingTables();
    }

    private void assertNoStagingTables() throws Exception {
        assertEquals(0, count("information_schema.tables WHERE table_schema = '" + DATABASE
                + "' AND table_name NOT IN ('samples', 'tuple_replication_notification_outbox', 'tuple_replication_journal_counter')"));
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(url + "/" + DATABASE, "root", password);
    }

    private long count(String relation) throws Exception {
        try (Connection connection = connection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT COUNT(*) FROM " + relation)) {
            assertTrue(rows.next());
            return rows.getLong(1);
        }
    }

    private static class LocalCsvService extends DataServiceSparkImpl {
        LocalCsvService() {
            super(null, spark);
        }

        @Override
        public Dataset<Row> getCsv(List<String> columns, String key, String delimiter, Boolean withHeader)
                throws MalformedException, StorageUnavailableException {
            try {
                return (Dataset<Row>) spark.read().option("delimiter", delimiter).option("header", withHeader)
                        .csv(key).toDF(columns.toArray(new String[0]));
            } catch (IllegalArgumentException e) {
                throw new MalformedException("Failed to map columns: " + e.getMessage());
            } catch (Exception e) {
                throw new StorageUnavailableException("Failed to connect to storage service: " + e.getMessage());
            }
        }
    }

    private static class LocalStorage implements StorageService {
        private final List<String> reads = new CopyOnWriteArrayList<>();

        @Override
        public void putObject(String key, byte[] content) {
            try {
                Files.write(Path.of(key), content);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public InputStream getObject(String bucket, String key) throws StorageNotFoundException {
            return new ByteArrayInputStream(getBytes(key));
        }

        @Override
        public byte[] getBytes(String key) throws StorageNotFoundException {
            reads.add(key);
            try {
                return Files.readAllBytes(Path.of(key));
            } catch (IOException e) {
                throw new StorageNotFoundException("Missing local ingest object: " + key, e);
            }
        }

        @Override
        public byte[] getBytes(String bucket, String key) throws StorageNotFoundException {
            return getBytes(key);
        }

        @Override
        public void deleteObject(String key) {
            try {
                Files.deleteIfExists(Path.of(key));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
