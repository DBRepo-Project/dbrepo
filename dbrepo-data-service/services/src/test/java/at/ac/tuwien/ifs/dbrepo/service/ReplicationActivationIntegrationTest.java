package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.AddReplicaDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationServiceImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationInboxService;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.sql.classic.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "REPLICATION_ACTIVATION_SQL_TEST_PORT", matches = "[0-9]+")
class ReplicationActivationIntegrationTest {
    private static final String SCHEMA = "replication_activation_test";
    private static final String ORIGIN = "https://source.example", TARGET = "https://target.example";
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("REPLICATION_ACTIVATION_SQL_TEST_PORT") + "/";
    private final String password = System.getenv("REPLICATION_ACTIVATION_SQL_TEST_PASSWORD");
    private final MetadataService metadata = mock(MetadataService.class);
    private final RestTemplate rest = mock(RestTemplate.class);
    private final DataService csv = mock(DataService.class);
    private final TupleReplicationNotificationDispatcher dispatcher = mock(TupleReplicationNotificationDispatcher.class);
    private final TupleReplicationOutboxServiceMariaDbImpl journal = new TupleReplicationOutboxServiceMariaDbImpl(
            new ObjectMapper().findAndRegisterModules());
    private final AtomicReference<Database> current = new AtomicReference<>();
    private final AtomicReference<Table> currentTable = new AtomicReference<>();
    private Database staleDatabase;
    private Table staleTable;
    private ReplicationActivationService activation;
    private TableServiceMariaDbImpl tables;

    @BeforeEach
    void setup() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "root", password); Statement sql = connection.createStatement()) {
            sql.execute("DROP DATABASE IF EXISTS " + SCHEMA);
            sql.execute("DROP DATABASE IF EXISTS " + SCHEMA + "_target");
            sql.execute("CREATE DATABASE " + SCHEMA);
            sql.execute("USE " + SCHEMA);
            sql.execute("CREATE TABLE samples (sample_value INT NOT NULL UNIQUE, group_key INT) ENGINE=InnoDB WITH SYSTEM VERSIONING");
            sql.execute("SET timestamp=1700000000.123456");
            sql.execute("INSERT INTO samples VALUES (1,1),(2,1),(3,1)");
            sql.execute("SET timestamp=1700000010.654321");
            sql.execute("UPDATE samples SET sample_value=10 WHERE sample_value=1");
            sql.execute("DELETE FROM samples WHERE sample_value=3");
        }
        staleTable = Table.builder().id(UUID.randomUUID()).internalName("samples").creationLocation(ORIGIN)
                .columns(List.of(column("sample_value", ColumnType.INT), column("group_key", ColumnType.INT)))
                .replicaUrls(Map.of()).build();
        staleDatabase = Database.builder().id(UUID.randomUUID()).internalName(SCHEMA).creationLocation(ORIGIN)
                .tables(List.of(staleTable)).replicaUrls(Map.of()).container(Container.builder()
                        .host("127.0.0.1").port(Integer.valueOf(System.getenv("REPLICATION_ACTIVATION_SQL_TEST_PORT")))
                        .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build()).build();
        current.set(staleDatabase);
        currentTable.set(staleTable);
        when(metadata.refreshDatabase(staleDatabase.getId())).thenAnswer(call -> current.get());
        when(metadata.refreshTable(staleDatabase.getId(), staleTable.getId())).thenAnswer(call -> currentTable.get());
        when(rest.postForEntity(anyString(), any(AddReplicaDto.class), eq(Void.class))).thenAnswer(call -> {
            register();
            return org.springframework.http.ResponseEntity.noContent().build();
        });
        activation = new ReplicationActivationService(metadata, rest, journal, TARGET, ORIGIN);
        final var replication = new ReplicationServiceImpl(journal, dispatcher);
        ReflectionTestUtils.setField(replication, "baseUrl", ORIGIN);
        tables = new TableServiceMariaDbImpl(null, Mappers.getMapper(MariaDbMapper.class), null,
                mock(StorageService.class), csv, replication);
        ReflectionTestUtils.setField(tables, "baseUrl", ORIGIN);
        ReflectionTestUtils.setField(tables, "activation", activation);
    }

    @AfterEach
    void cleanup() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "root", password); Statement sql = connection.createStatement()) {
            sql.execute("DROP DATABASE IF EXISTS " + SCHEMA);
            sql.execute("DROP DATABASE IF EXISTS " + SCHEMA + "_target");
        }
    }

    @Test
    void preparationPreservesAllLegacyValuesAndMicrosecondHistoryAndIsIdempotent() throws Exception {
        final var before = history(false);
        activation.activate(staleDatabase.getId(), TARGET);
        assertEquals(before, history(false));
        assertEquals(4, history(true).size());
        final var keys = history(true);
        activation.activate(staleDatabase.getId(), TARGET);
        assertEquals(keys, history(true));
        try (Connection connection = connection(); Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(
                "SELECT COUNT(*) FROM samples FOR SYSTEM_TIME ALL WHERE replication_key IS NULL OR replication_key = ''")) {
            assertTrue(row.next());
            assertEquals(0, row.getInt(1));
        }
        verify(rest, times(2)).postForEntity(contains("preparedTableId=" + staleTable.getId()), any(AddReplicaDto.class), eq(Void.class));
    }

    @Test
    void activationWaitsForInFlightCsvAndStaleCrudRequestsJournalEveryChangedTuple() throws Exception {
        final CountDownLatch parsing = new CountDownLatch(1), finishParsing = new CountDownLatch(1);
        final Dataset<Row> dataset = mock(Dataset.class);
        when(dataset.columns()).thenReturn(new String[]{"sample_value", "group_key"});
        when(dataset.toLocalIterator()).thenReturn(List.of(RowFactory.create(4, 1)).iterator());
        when(csv.getCsv(anyList(), anyString(), anyString(), anyBoolean())).thenAnswer(call -> {
            parsing.countDown();
            assertTrue(finishParsing.await(10, TimeUnit.SECONDS));
            return dataset;
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            final Future<?> imported = executor.submit(() -> {
                tables.importDataset(staleDatabase, staleTable, ImportDto.builder().location("test.csv").header(true).separator(',').build());
                return null;
            });
            assertTrue(parsing.await(10, TimeUnit.SECONDS));
            final Future<?> activated = executor.submit(() -> { activation.activate(staleDatabase.getId(), TARGET); return null; });
            try {
                assertThrows(TimeoutException.class, () -> activated.get(300, TimeUnit.MILLISECONDS));
            } finally { finishParsing.countDown(); }
            imported.get(15, TimeUnit.SECONDS);
            activated.get(15, TimeUnit.SECONDS);
        }
        assertTrue(history(false).stream().anyMatch(value -> value.startsWith("4|")));
        assertTrue(journal.findAll(current.get()).isEmpty());
        doAnswer(call -> {
            assertFalse(journal.findAll(current.get()).isEmpty(), "Dispatch must follow the data and journal commit");
            return null;
        }).when(dispatcher).dispatchAsync(any(Database.class), anyList());
        tables.createTuple(staleDatabase, staleTable, TupleDto.builder()
                .data(new LinkedHashMap<>(Map.of("sample_value", 5, "group_key", 1))).build());
        tables.updateTuple(staleDatabase, staleTable, TupleUpdateDto.builder().keys(Map.of("sample_value", 10))
                .data(Map.of("sample_value", 11)).build());
        tables.deleteTuple(staleDatabase, staleTable, TupleDeleteDto.builder().keys(Map.of("group_key", 1)).build());
        assertEquals(6, journal.findAll(current.get()).size()); // create + update + four deleted current rows
        verify(dispatcher, times(3)).dispatchAsync(any(Database.class), anyList());
    }

    @Test
    void lostRegistrationResponseCannotResumeUnjournaledWritesAndRetryRecovers() throws Exception {
        when(rest.postForEntity(anyString(), any(AddReplicaDto.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("response lost"));
        assertThrows(ResourceAccessException.class, () -> activation.activate(staleDatabase.getId(), TARGET));
        final var before = history(true);
        assertThrows(SQLException.class, () -> tables.createTuple(staleDatabase, staleTable,
                TupleDto.builder().data(new LinkedHashMap<>(Map.of("sample_value", 99))).build()));
        assertEquals(before, history(true));
        when(rest.postForEntity(anyString(), any(AddReplicaDto.class), eq(Void.class))).thenAnswer(call -> {
            register(); return org.springframework.http.ResponseEntity.noContent().build();
        });
        activation.activate(staleDatabase.getId(), TARGET);
        tables.createTuple(staleDatabase, staleTable,
                TupleDto.builder().data(new LinkedHashMap<>(Map.of("sample_value", 99))).build());
        assertEquals(1, journal.findAll(current.get()).size());
    }

    @Test
    void rolledBackMutationAfterActivationLeavesNoHistoryChangeOrOutboxDelivery() throws Exception {
        activation.activate(staleDatabase.getId(), TARGET);
        final var before = history(true);
        assertThrows(at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException.class,
                () -> tables.createTuple(staleDatabase, staleTable,
                        TupleDto.builder().data(new LinkedHashMap<>(Map.of("sample_value", 2))).build()));
        assertEquals(before, history(true));
        assertTrue(journal.findAll(current.get()).isEmpty());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void rejectedRegistrationDoesNotDisableOrdinarySourceWrites() throws Exception {
        when(rest.postForEntity(anyString(), any(AddReplicaDto.class), eq(Void.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));
        assertThrows(org.springframework.web.client.HttpClientErrorException.class,
                () -> activation.activate(staleDatabase.getId(), TARGET));
        tables.createTuple(staleDatabase, staleTable,
                TupleDto.builder().data(new LinkedHashMap<>(Map.of("sample_value", 77))).build());
        assertTrue(history(false).stream().anyMatch(value -> value.startsWith("77|")));
        assertTrue(journal.findAll(staleDatabase).isEmpty());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void untrustedAndSecondaryRequestsCannotPrepareOrRegister() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> activation.activate(staleDatabase.getId(), "https://untrusted.example"));
        current.get().setCreationLocation(TARGET);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> activation.activate(staleDatabase.getId(), TARGET));
        verifyNoInteractions(rest);
        assertEquals(4, history(false).size());
    }

    @Test
    void newlyPreparedHistoryBootstrapsAnEmptyTargetAndThenAcceptsLiveEvents() throws Exception {
        activation.activate(staleDatabase.getId(), TARGET);
        final var source = new HistorySnapshotService(journal, ORIGIN);
        final var receiver = new HistorySnapshotService(journal, TARGET);
        final UUID snapshotId = UUID.randomUUID();
        final var envelope = source.create(current.get(), currentTable.get(),
                new at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.Create(snapshotId, staleTable.getId(), null));
        assertEquals(4, envelope.manifest().rows());
        assertEquals(2, envelope.manifest().currentKeys());
        final Database target = Database.builder().id(UUID.randomUUID()).internalName(SCHEMA + "_target")
                .container(staleDatabase.getContainer()).creationLocation(ORIGIN)
                .replicaUrls(Map.of(ORIGIN, staleDatabase.getId())).build();
        final Table targetTable = Table.builder().id(UUID.randomUUID()).internalName("samples").creationLocation(ORIGIN)
                .columns(currentTable.get().getColumns()).replicaUrls(Map.of(ORIGIN, staleTable.getId())).build();
        current.get().setReplicaUrls(Map.of(TARGET, target.getId()));
        currentTable.get().setReplicaUrls(Map.of(TARGET, targetTable.getId()));
        try (Connection connection = connection(); Statement sql = connection.createStatement()) {
            sql.execute("CREATE DATABASE " + SCHEMA + "_target");
            sql.execute("CREATE TABLE " + SCHEMA + "_target.samples (sample_value INT NOT NULL UNIQUE, group_key INT, "
                    + "replication_key VARCHAR(36) NOT NULL UNIQUE) ENGINE=InnoDB WITH SYSTEM VERSIONING");
        }
        receiver.beginImport(target, targetTable, new at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.Import(targetTable.getId(), envelope));
        for (long index = 0; index < envelope.manifest().chunks(); index++) {
            receiver.putChunk(target, snapshotId, source.readChunk(current.get(), snapshotId, index));
        }
        assertTrue(receiver.verifyImport(target, targetTable, snapshotId).historyVerified());
        final var inbox = new ReplicationInboxService(Mappers.getMapper(MariaDbMapper.class), new ObjectMapper().findAndRegisterModules(), TARGET);
        assertTrue(inbox.reconcile(target, targetTable, snapshotId, receiver).currentReconciled());
        assertTrue(inbox.reconcile(target, targetTable, snapshotId, receiver).currentReconciled());
        tables.createTuple(staleDatabase, staleTable, TupleDto.builder()
                .data(new LinkedHashMap<>(Map.of("sample_value", 42, "group_key", 1))).build());
        final var entry = journal.findAll(current.get()).getFirst();
        final var event = new ObjectMapper().findAndRegisterModules().readValue(entry.getPayloadJson(),
                at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto.class);
        inbox.apply(target, targetTable, event, entry.getHttpMethod());
        inbox.apply(target, targetTable, event, entry.getHttpMethod());
        try (Connection connection = connection(); Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery(
                "SELECT GROUP_CONCAT(sample_value ORDER BY sample_value) FROM " + SCHEMA + "_target.samples")) {
            assertTrue(row.next());
            assertEquals("2,10,42", row.getString(1));
        }
    }

    private void register() {
        final Table table = Table.builder().id(staleTable.getId()).internalName("samples").creationLocation(ORIGIN)
                .columns(List.of(column("sample_value", ColumnType.INT), column("group_key", ColumnType.INT),
                        column("replication_key", ColumnType.VARCHAR)))
                .replicaUrls(Map.of(TARGET, UUID.randomUUID())).build();
        currentTable.set(table);
        current.set(Database.builder().id(staleDatabase.getId()).internalName(SCHEMA).container(staleDatabase.getContainer())
                .creationLocation(ORIGIN).tables(List.of(table)).replicaUrls(Map.of(TARGET, UUID.randomUUID())).build());
    }

    private Column column(String name, ColumnType type) {
        return Column.builder().internalName(name).columnType(type).build();
    }

    private Connection connection() throws SQLException { return DriverManager.getConnection(url + SCHEMA, "root", password); }

    private List<String> history(boolean keys) throws SQLException {
        final List<String> values = new ArrayList<>();
        try (Connection connection = connection(); Statement sql = connection.createStatement(); ResultSet rows = sql.executeQuery(
                "SELECT sample_value, ROW_START, ROW_END" + (keys ? ", replication_key" : "")
                        + " FROM samples FOR SYSTEM_TIME ALL ORDER BY sample_value, ROW_START")) {
            while (rows.next()) values.add(rows.getInt(1) + "|" + rows.getString(2) + "|" + rows.getString(3)
                    + (keys ? "|" + rows.getString(4) : ""));
        }
        return values;
    }
}
