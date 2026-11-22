package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationServiceImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationTimestampServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private ReplicationServiceImpl replication;
    private TupleReplicationNotificationDispatcher dispatcher;

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
        dispatcher = mock(TupleReplicationNotificationDispatcher.class);
        replication = new ReplicationServiceImpl(outbox, dispatcher);
        ReflectionTestUtils.setField(replication, "baseUrl", "https://origin.example");
        service = new TableServiceMariaDbImpl(mock(DataMapper.class), Mappers.getMapper(MariaDbMapper.class),
                mock(SubsetService.class), mock(StorageService.class), mock(DataService.class), replication);
        try (Connection root = DriverManager.getConnection(url + "?sslMode=disable", "root", password)) {
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
        final AtomicInteger committed = new AtomicInteger();
        doAnswer(invocation -> {
            final List<?> ids = invocation.getArgument(1);
            assertEquals(committed.addAndGet(ids.size()), count("tuple_replication_notification_outbox"));
            return null;
        }).when(dispatcher).dispatchAsync(eq(database), anyList());
        service.createTuple(database, table, tuple("a", 1));
        service.createTuple(database, table, tuple("b", 1));
        service.updateTuple(database, table, update(1, 2));
        service.deleteTuple(database, table, TupleDeleteDto.builder().keys(Map.of("sample_value", 2)).build());
        assertEquals(0, count("samples"));
        assertEquals(4, count("samples FOR SYSTEM_TIME ALL"));
        final var events = outbox.findAll(database);
        assertEquals(6, events.size());
        verify(dispatcher, times(4)).dispatchAsync(eq(database), anyList());
        verify(dispatcher, times(2)).dispatchAsync(eq(database), argThat(ids -> ids.size() == 1));
        verify(dispatcher, times(2)).dispatchAsync(eq(database), argThat(ids -> ids.size() == 2));
        assertEquals(2, events.stream().filter(e -> e.getHttpMethod().name().equals("DELETE")).count());
        for (var event : events) {
            final var payload = new ObjectMapper().readTree(event.getPayloadJson());
            assertNotNull(payload.get("tuple").get("replicationKey"));
        }
    }

    @Test
    void retriesWholeMutationsAndDispatchesOnlyCommittedEventIds() throws Exception {
        final var attempted = new HashSet<HttpMethod>();
        final var discarded = new HashSet<UUID>();
        final var dispatched = new ArrayList<UUID>();
        doAnswer(invocation -> {
            final TupleReplicationOutboxEntry event = (TupleReplicationOutboxEntry) invocation.callRealMethod();
            if (attempted.add(event.getHttpMethod())) {
                discarded.add(event.getId());
                throw new SQLException("simulated lock conflict after writing the event", "40001", 1213);
            }
            return event;
        }).when(outbox).enqueue(any(Connection.class), any(), any(), any(), any());
        doAnswer(invocation -> {
            dispatched.addAll(invocation.getArgument(1));
            return null;
        }).when(dispatcher).dispatchAsync(eq(database), anyList());

        service.createTuple(database, table, tuple("a", 1));
        service.updateTuple(database, table, update(1, 2));
        service.deleteTuple(database, table, TupleDeleteDto.builder().keys(Map.of("sample_value", 2)).build());

        final List<UUID> retained = outbox.findAll(database).stream().map(TupleReplicationOutboxEntry::getId).toList();
        assertEquals(3, discarded.size());
        assertEquals(3, dispatched.size());
        assertEquals(3, retained.size());
        assertTrue(java.util.Collections.disjoint(discarded, retained));
        assertEquals(new HashSet<>(retained), new HashSet<>(dispatched));
        assertEquals(0, count("samples"));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL WHERE sample_value=1"));
        assertEquals(1, count("samples FOR SYSTEM_TIME ALL WHERE sample_value=2"));
    }

    @Test
    void exhaustedLockWaitPropagatesAsDatabaseFailureAndLeavesNoEvent() throws Exception {
        final var timeout = new SQLException("simulated lock wait", "HY000", 1205);
        doThrow(timeout).when(outbox).enqueue(any(Connection.class), any(), any(), any(), any());
        assertSame(timeout, assertThrows(SQLException.class, () -> service.createTuple(database, table, tuple("a", 1))));
        verify(outbox, times(3)).enqueue(any(Connection.class), any(), any(), any(), any());
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(0, count("tuple_replication_notification_outbox"));
        verifyNoInteractions(dispatcher);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void primaryWritesAndDelayedAcknowledgementsKeepBothHistories(int primary) throws Exception {
        final List<String> sites = List.of("https://site-a.example", "https://site-b.example", "https://site-c.example");
        final String replica = sites.get((primary + 1) % sites.size());
        database.setCreationLocation(sites.get(primary));
        database.setReplicaUrls(Map.of(replica, UUID.randomUUID(), sites.get((primary + 2) % sites.size()), UUID.randomUUID()));
        ReflectionTestUtils.setField(replication, "baseUrl", sites.get(primary));
        final var acknowledgement = spy(new ReplicationTimestampServiceMariaDbImpl());
        final var pending = TupleReplicationTimestampDto.builder().siteUrl(replica)
                .databaseId(database.getId()).tableId(table.getId()).replicationId("a")
                .rowStart(Instant.parse("2026-01-02T03:04:05Z")).build();
        final var deleteInFlight = new CountDownLatch(1);
        final var acknowledgementInFlight = new CountDownLatch(1);
        doAnswer(invocation -> {
            acknowledgementInFlight.countDown();
            return invocation.callRealMethod();
        }).when(acknowledgement).ensureTableExists(any(Connection.class));
        doAnswer(invocation -> {
            if (HttpMethod.DELETE.equals(invocation.getArgument(3))) {
                deleteInFlight.countDown();
                assertTrue(acknowledgementInFlight.await(10, TimeUnit.SECONDS));
            }
            return invocation.callRealMethod();
        }).when(outbox).enqueue(any(Connection.class), any(), any(), any(), any());

        try (var workers = Executors.newSingleThreadExecutor()) {
            var acknowledgements = workers.submit(() -> {
                assertTrue(deleteInFlight.await(10, TimeUnit.SECONDS));
                for (int i = 0; i < 2; i++) acknowledgement.saveTimestamps(database, List.of(pending));
                return null;
            });
            service.createTuple(database, table, tuple("a", 1));
            service.updateTuple(database, table, update(1, 2));
            service.deleteTuple(database, table, TupleDeleteDto.builder().keys(Map.of("sample_value", 2)).build());
            acknowledgements.get(30, TimeUnit.SECONDS);
        }
        assertEquals(0, count("samples"));
        assertEquals(2, count("samples FOR SYSTEM_TIME ALL"));
        assertEquals(1, count("tuple_replication_timestamps WHERE site_url='" + replica + "'"));
        assertEquals(3, outbox.findAll(database).stream().map(TupleReplicationOutboxEntry::getId).distinct().count());
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
        verifyNoInteractions(dispatcher);
    }

    @Test
    void serializationFailureAfterMutationAlsoRollsBack() throws Exception {
        doThrow(new IllegalArgumentException("injected serialization failure"))
                .when(outbox).enqueue(any(Connection.class), any(), any(), any(), any());
        assertThrows(IllegalArgumentException.class, () -> service.createTuple(database, table, tuple("a", 1)));
        assertEquals(0, count("samples FOR SYSTEM_TIME ALL"));
        verifyNoInteractions(dispatcher);
    }

    @Test
    void failedImmediateSchedulingLeavesCommittedEventForRetry() throws Exception {
        doThrow(new IllegalStateException("executor unavailable"))
                .when(dispatcher).dispatchAsync(eq(database), anyList());
        service.createTuple(database, table, tuple("a", 1));
        assertEquals(1, count("samples"));
        assertEquals(1, count("tuple_replication_notification_outbox"));
    }

    @Test
    void committedChangesAreDeliveredWithoutRunningTheScheduler() throws Exception {
        final RestTemplate http = mock(RestTemplate.class);
        when(http.exchange(eq("/api/replication/data"), any(HttpMethod.class), any(HttpEntity.class),
                eq(Void.class))).thenReturn(ResponseEntity.accepted().build());
        final var sender = new TupleReplicationNotificationDispatcher(http, mock(MetadataService.class), outbox);
        ReflectionTestUtils.setField(sender, "processingTimeoutSeconds", 300L);
        doAnswer(invocation -> {
            sender.dispatchAsync(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(dispatcher).dispatchAsync(eq(database), anyList());

        service.createTuple(database, table, tuple("a", 1));
        service.updateTuple(database, table, update(1, 2));
        service.deleteTuple(database, table, TupleDeleteDto.builder().keys(Map.of("sample_value", 2)).build());

        verify(http, times(3)).exchange(eq("/api/replication/data"), any(HttpMethod.class),
                any(HttpEntity.class), eq(Void.class));
        assertEquals(3, count("tuple_replication_notification_outbox WHERE status = 'SUCCEEDED'"));
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
        verifyNoInteractions(dispatcher);
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

    @Test
    void nonTransactionalSourceIsRejectedBeforeMutation() throws Exception {
        try (Connection root = connection()) {
            root.createStatement().execute("ALTER TABLE samples DROP SYSTEM VERSIONING");
            root.createStatement().execute("ALTER TABLE samples ENGINE=MyISAM");
        }
        assertThrows(QueryMalformedException.class, () -> service.createTuple(database, table, tuple("a", 1)));
        assertEquals(0, count("samples"));
        assertEquals(0, count("tuple_replication_notification_outbox"));
    }

    private TupleDto tuple(String key, int value) {
        return TupleDto.builder().data(new LinkedHashMap<>(Map.of("replication_key", key, "sample_value", value))).build();
    }

    private TupleUpdateDto update(int oldValue, int newValue) {
        return TupleUpdateDto.builder().keys(Map.of("sample_value", oldValue))
                .data(Map.of("sample_value", newValue)).build();
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(url + "/atomic_tuple_test" + "?sslMode=disable", "root", password);
    }

    private long count(String relation) throws Exception {
        try (Connection root = connection(); var result = root.createStatement().executeQuery("SELECT COUNT(*) FROM " + relation)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }
}
