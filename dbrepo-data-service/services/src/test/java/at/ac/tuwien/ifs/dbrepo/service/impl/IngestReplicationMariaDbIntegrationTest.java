package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.classic.Dataset;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
        replication = new ReplicationServiceImpl(outbox, null);
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
        service.importDataset(database, table, input);

        assertEquals(2, count("samples"));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(2, count("(SELECT DISTINCT replication_key FROM samples) AS identities"));
        final var events = outbox.findAll(database);
        assertEquals(2, events.size());
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
    void duplicateLateRowRollsBackEveryRowEventAndHistoryAndRetainsUpload() throws Exception {
        final ImportDto input = csv("sample_value,payload,amount\n1,first,\n2,second,\n1,duplicate,\n");
        assertThrows(MalformedException.class, () -> service.importDataset(database, table, input));
        assertRolledBack(input);
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
        table.setColumns(table.getColumns().stream().filter(c -> !"replication_key".equals(c.getInternalName())).toList());
        try (Connection connection = connection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE samples");
            statement.execute("CREATE TABLE samples (sample_value INT NOT NULL UNIQUE, payload LONGBLOB, "
                    + "amount DECIMAL(38,18)) ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
        service.importDataset(database, table, csv("sample_value,payload,amount\n1,literal,1.25\n"));
        assertEquals(1, count("samples WHERE sample_value = 1 AND amount = 1.25"));
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
        assertEquals(4, count("samples"));
        assertEquals(4, outbox.findAll(database).size());
        assertEquals(2, count("samples WHERE sample_value IN (1,2) AND payload = 'first'"));
        assertEquals(2, count("samples WHERE sample_value IN (3,4) AND payload = 'second'"));
        assertNoStagingTables();
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
                + "' AND table_name NOT IN ('samples', 'tuple_replication_notification_outbox')"));
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
