package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationServiceImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "REPLICA_SQL_TEST_PORT", matches = "[0-9]+")
class AtomicTupleOutboxIntegrationTest {
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("REPLICA_SQL_TEST_PORT");
    private final String password = System.getenv("REPLICA_SQL_TEST_PASSWORD");
    private final TupleReplicationOutboxServiceMariaDbImpl outbox =
            spy(new TupleReplicationOutboxServiceMariaDbImpl(new ObjectMapper().findAndRegisterModules()));
    private Database database;
    private Table table;
    private TableServiceMariaDbImpl service;

    @BeforeEach
    void setup() throws Exception {
        database = Database.builder().id(UUID.randomUUID()).internalName("atomic_tuple_test")
                .creationLocation("https://origin.example")
                .replicaUrls(Map.of("https://replica.example", UUID.randomUUID()))
                .container(Container.builder().id(UUID.randomUUID()).internalName("test")
                        .host("127.0.0.1").port(Integer.valueOf(System.getenv("REPLICA_SQL_TEST_PORT")))
                        .username("root").password(password).image(Image.builder().jdbcMethod("mariadb").build()).build())
                .build();
        table = Table.builder().id(UUID.randomUUID()).internalName("samples")
                .columns(List.of(Column.builder().internalName("replication_key").columnType(ColumnType.VARCHAR).build(),
                        Column.builder().internalName("sample_value").columnType(ColumnType.INT).build())).build();
        final ReplicationServiceImpl replication = new ReplicationServiceImpl(outbox,
                mock(TupleReplicationNotificationDispatcher.class));
        ReflectionTestUtils.setField(replication, "baseUrl", "https://origin.example");
        service = new TableServiceMariaDbImpl(mock(DataMapper.class), Mappers.getMapper(MariaDbMapper.class),
                mock(SubsetService.class), mock(StorageService.class), mock(DataService.class), replication);
        try (Connection root = DriverManager.getConnection(url, "root", password)) {
            root.createStatement().execute("DROP DATABASE IF EXISTS atomic_tuple_test");
            root.createStatement().execute("CREATE DATABASE atomic_tuple_test");
            root.createStatement().execute("CREATE TABLE atomic_tuple_test.samples "
                    + "(replication_key VARCHAR(36) PRIMARY KEY, sample_value INT) WITH SYSTEM VERSIONING");
        }
        try (Connection root = connection()) {
            outbox.ensureTableExists(root);
        }
    }

    @Test
    void sourceWritesAndEveryAffectedTupleAreCommittedTogether() throws Exception {
        service.createTuple(database, table, tuple("a", 1));
        service.createTuple(database, table, tuple("b", 1));
        service.updateTuple(database, table, update(1, 2));
        service.deleteTuple(database, table, TupleDeleteDto.builder().keys(Map.of("sample_value", 2)).build());
        assertEquals(0, count("samples"));
        assertEquals(4, count("samples FOR SYSTEM_TIME ALL"));
        final var events = outbox.findAll(database);
        assertEquals(6, events.size());
        assertEquals(2, events.stream().filter(e -> e.getHttpMethod().name().equals("DELETE")).count());
        for (var event : events) {
            final var payload = new ObjectMapper().readTree(event.getPayloadJson());
            assertNotNull(payload.get("tuple").get("replicationKey"));
        }
    }

    @Test
    void failedOutboxInsertRollsBackInsertUpdateDeleteAndHistory() throws Exception {
        try (Connection root = connection()) {
            root.createStatement().execute("INSERT INTO samples VALUES ('a', 1), ('b', 1)");
            root.createStatement().execute("CREATE TRIGGER fail_outbox BEFORE INSERT "
                    + "ON tuple_replication_notification_outbox FOR EACH ROW SIGNAL SQLSTATE '45000' "
                    + "SET MESSAGE_TEXT = 'injected outbox failure'");
        }
        assertThrows(QueryMalformedException.class, () -> service.createTuple(database, table, tuple("c", 1)));
        assertThrows(QueryMalformedException.class, () -> service.updateTuple(database, table, update(1, 2)));
        assertThrows(QueryMalformedException.class, () -> service.deleteTuple(database, table,
                TupleDeleteDto.builder().keys(Map.of("sample_value", 1)).build()));
        assertEquals(2, count("samples WHERE sample_value = 1"));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(0, count("tuple_replication_notification_outbox"));
    }

    @Test
    void serializationFailureAfterMutationAlsoRollsBack() throws Exception {
        doThrow(new IllegalArgumentException("injected serialization failure"))
                .when(outbox).enqueue(any(Connection.class), any(), any(), any(), any());
        assertThrows(IllegalArgumentException.class, () -> service.createTuple(database, table, tuple("a", 1)));
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
    }

    @Test
    void inboundReplicaWritesDoNotFanOutAgain() throws Exception {
        database.setCreationLocation("https://remote.example");
        service.createTupleWithTimestamps(database, table, tuple("a", 1));
        service.updateTupleWithTimestamps(database, table, update(1, 2));
        service.deleteTupleWithTimestamps(database, table,
                TupleDeleteDto.builder().keys(Map.of("sample_value", 2)).build());
        assertEquals(0, count("tuple_replication_notification_outbox"));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
    }

    @Test
    void replicationIdentityCannotBeChanged() throws Exception {
        service.createTuple(database, table, tuple("a", 1));
        assertThrows(QueryMalformedException.class, () -> service.updateTuple(database, table,
                TupleUpdateDto.builder().keys(Map.of("replication_key", "a"))
                        .data(Map.of("replication_key", "b")).build()));
        assertEquals(1, count("samples WHERE replication_key = 'a'"));
        assertEquals(1, count("tuple_replication_notification_outbox"));
    }

    private TupleDto tuple(String key, int value) {
        return TupleDto.builder().data(new LinkedHashMap<>(Map.of("replication_key", key, "sample_value", value))).build();
    }

    private TupleUpdateDto update(int oldValue, int newValue) {
        return TupleUpdateDto.builder().keys(Map.of("sample_value", oldValue))
                .data(Map.of("sample_value", newValue)).build();
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(url + "/atomic_tuple_test", "root", password);
    }

    private long count(String relation) throws Exception {
        try (Connection root = connection(); var result = root.createStatement().executeQuery("SELECT COUNT(*) FROM " + relation)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }
}
