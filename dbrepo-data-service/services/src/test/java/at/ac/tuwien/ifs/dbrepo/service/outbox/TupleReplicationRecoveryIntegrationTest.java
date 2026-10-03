package at.ac.tuwien.ifs.dbrepo.service.outbox;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.TupleReplicationNotificationDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "RECOVERY_TUPLE_SQL_TEST_PORT", matches = "13366")
class TupleReplicationRecoveryIntegrationTest {
    private static final String URL = "jdbc:mariadb://127.0.0.1:13366/";
    private static final String SCHEMA = "recovery_tuple_test";
    private static final Duration LEASE = Duration.ofMinutes(5);
    private TupleReplicationOutboxServiceMariaDbImpl service;
    private Database database;
    private Table table;
    private RestTemplate http;

    @BeforeEach
    void prepare() throws Exception {
        try (var connection = DriverManager.getConnection(URL, "root", password());
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + SCHEMA);
        }
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS tuple_replication_notification_outbox");
            statement.executeUpdate("DROP TABLE IF EXISTS tuple_replication_journal_counter");
        }
        database = Database.builder().id(UUID.randomUUID()).internalName(SCHEMA)
                .container(Container.builder().host("127.0.0.1").port(13366).username("root").password(password())
                        .image(Image.builder().jdbcMethod("mariadb").build()).build()).build();
        table = Table.builder().id(UUID.randomUUID()).internalName("samples").build();
        service = newService();
        http = mock(RestTemplate.class);
    }

    @Test
    void automaticRecoveryAfterTwentyFiveFailuresAndRestartRetainsJournal() throws Exception {
        final var entry = enqueue();
        final var original = journal();
        when(http.exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("offline"));
        var dispatcher = dispatcher();
        for (int attempt = 1; attempt <= 25; attempt++) {
            assertEquals(0, dispatcher.dispatchDue(database));
            final var failed = service.findAll(database).getFirst();
            assertEquals(attempt, failed.getAttempts());
            assertEquals(attempt < 20 ? TupleReplicationOutboxStatus.PENDING : TupleReplicationOutboxStatus.FAILED,
                    failed.getStatus());
            assertEquals(Duration.ofSeconds(Math.min(900, 30L << Math.min(attempt - 1, 10))),
                    Duration.between(failed.getLastModified(), failed.getNextAttemptAt()));
            assertNull(failed.getClaimToken());
            assertNull(failed.getClaimUntil());
            assertTrue(service.claimDue(database, 25, LEASE).isEmpty());
            if (attempt == 20) {
                service = newService();
                dispatcher = dispatcher();
                assertEquals(TupleReplicationOutboxStatus.FAILED, service.findAll(database).getFirst().getStatus());
            }
            makeDue(entry.getId());
        }
        verify(http, times(25)).exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
        doReturn(ResponseEntity.accepted().build()).when(http)
                .exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
        assertEquals(1, dispatcher.dispatchDue(database));
        assertTrue(service.findAll(database).isEmpty());
        assertEquals(original, journal());
        assertEquals(1, journal().size());
    }

    @Test
    void expiredClaimsAreRecoveredAndStaleWorkersCannotCompleteOrFailThem() throws Exception {
        final var entry = enqueue();
        final var original = journal();
        final var old = service.claim(database, entry.getId(), LEASE).orElseThrow();
        assertNotNull(old.getClaimToken());
        assertTrue(old.getClaimUntil().isAfter(old.getLastModified()));
        assertTrue(service.claim(database, entry.getId(), LEASE).isEmpty());
        expire(entry.getId());
        assertFalse(service.markSucceeded(database, entry.getId(), old.getClaimToken()));
        assertFalse(service.markFailed(database, entry.getId(), old.getClaimToken(), "late", Duration.ZERO, 1, true));
        service = newService();
        final var replacement = service.claimDue(database, 1, LEASE).getFirst();
        assertNotEquals(old.getClaimToken(), replacement.getClaimToken());
        assertFalse(service.markSucceeded(database, entry.getId(), old.getClaimToken()));
        assertFalse(service.markFailed(database, entry.getId(), old.getClaimToken(), "late", Duration.ZERO, 1, true));
        assertTrue(service.markFailed(database, entry.getId(), replacement.getClaimToken(), "offline", Duration.ZERO, 1, true));
        assertFalse(service.markFailed(database, entry.getId(), replacement.getClaimToken(), "duplicate", Duration.ZERO, 1, true));
        final var failedClaim = service.claimDue(database, 1, LEASE).getFirst();
        assertEquals(1, failedClaim.getAttempts());
        assertEquals(TupleReplicationOutboxStatus.FAILED, failedClaim.getStatus());
        assertEquals(TupleReplicationOutboxStatus.FAILED, service.findAll(database).getFirst().getStatus());
        assertTrue(service.claimDue(database, 1, LEASE).isEmpty());
        assertTrue(service.claim(database, entry.getId(), LEASE).isEmpty());
        expire(entry.getId());
        final var finalClaim = newService().claimDue(database, 1, LEASE).getFirst();
        assertEquals(TupleReplicationOutboxStatus.FAILED, finalClaim.getStatus());
        assertTrue(service.markSucceeded(database, entry.getId(), finalClaim.getClaimToken()));
        assertFalse(service.markFailed(database, entry.getId(), failedClaim.getClaimToken(), "late", Duration.ZERO, 1, true));
        assertFalse(service.markFailed(database, entry.getId(), finalClaim.getClaimToken(), "late", Duration.ZERO, 1, true));
        assertFalse(service.markSucceeded(database, entry.getId(), failedClaim.getClaimToken()));
        assertTrue(service.claim(database, entry.getId(), LEASE).isEmpty());
        assertTrue(service.findAll(database).isEmpty());
        assertEquals(original, journal());
    }

    @Test
    void concurrentManualAndScheduledClaimsHaveOnlyOneOwner() throws Exception {
        final var entry = enqueue();
        final var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var manual = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return newService().claim(database, entry.getId(), LEASE).stream().toList();
            });
            final var scheduled = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return newService().claimDue(database, 1, LEASE);
            });
            start.countDown();
            final var one = manual.get(20, TimeUnit.SECONDS);
            final var two = scheduled.get(20, TimeUnit.SECONDS);
            assertEquals(1, one.size() + two.size());
            final var winner = one.isEmpty() ? two.getFirst() : one.getFirst();
            assertTrue(service.markSucceeded(database, entry.getId(), winner.getClaimToken()));
        }
    }

    @Test
    void initializedJournalUpgradePreservesHistoryAndRecoversLegacyProcessing() throws Exception {
        final var entry = enqueue();
        final var original = journal();
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE tuple_replication_notification_outbox DROP COLUMN claim_token, DROP COLUMN claim_until");
            statement.executeUpdate("UPDATE tuple_replication_notification_outbox SET status='PROCESSING', last_modified=CURRENT_TIMESTAMP(6)");
        }
        service = newService();
        assertTrue(service.claimDue(database, 1, LEASE).isEmpty());
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE tuple_replication_notification_outbox SET last_modified=TIMESTAMPADD(SECOND,-301,CURRENT_TIMESTAMP(6))");
        }
        final var claim = service.claimDue(database, 1, LEASE).getFirst();
        assertNotNull(claim.getClaimToken());
        assertTrue(service.markSucceeded(database, entry.getId(), claim.getClaimToken()));
        assertEquals(original, journal());
    }

    @Test
    void permanentHttpErrorIsNeverAutomaticallyAcknowledgedOrCancelled() throws Exception {
        final var entry = enqueue();
        when(http.exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.notFound().build());
        final var dispatcher = dispatcher();
        ReflectionTestUtils.setField(dispatcher, "maxAttempts", 1);
        assertEquals(0, dispatcher.dispatchDue(database));
        final var failed = service.findAll(database).getFirst();
        assertEquals(TupleReplicationOutboxStatus.FAILED, failed.getStatus());
        assertNull(failed.getNextAttemptAt());
        assertTrue(service.claimDue(database, 25, LEASE).isEmpty());
        final var manual = service.claim(database, entry.getId(), LEASE).orElseThrow();
        assertEquals(TupleReplicationOutboxStatus.FAILED, manual.getStatus());
        // Even a manual attempt of a terminal failure is recoverable if its worker crashes.
        expire(entry.getId());
        final var recovered = newService().claimDue(database, 1, LEASE).getFirst();
        assertFalse(service.markSucceeded(database, entry.getId(), manual.getClaimToken()));
        assertTrue(service.markSucceeded(database, entry.getId(), recovered.getClaimToken()));
        assertEquals(1, journal().size());
    }

    @Test
    void failureCounterSaturatesAndChangedThresholdCannotClearFailedAlert() throws Exception {
        final var entry = enqueue();
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE tuple_replication_notification_outbox SET status='FAILED', attempts=2147483647");
        }
        final var claim = service.claim(database, entry.getId(), LEASE).orElseThrow();
        assertTrue(service.markFailed(database, entry.getId(), claim.getClaimToken(), "offline", Duration.ofMinutes(15),
                Integer.MAX_VALUE, true));
        final var failed = service.findAll(database).getFirst();
        assertEquals(Integer.MAX_VALUE, failed.getAttempts());
        assertEquals(TupleReplicationOutboxStatus.FAILED, failed.getStatus());
        assertNotNull(failed.getNextAttemptAt());
    }

    @Test
    void remoteSuccessWithFailedLocalAcknowledgementRecoversAfterLeaseExpiry() throws Exception {
        final var entry = enqueue();
        final var original = journal();
        service = spy(service);
        doThrow(new SQLException("injected acknowledgement failure")).when(service).markSucceeded(any(), any(), any());
        when(http.exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted().build());
        assertEquals(0, dispatcher().dispatchDue(database));
        final var pendingAck = service.findAll(database).getFirst();
        assertEquals(TupleReplicationOutboxStatus.PROCESSING, pendingAck.getStatus());
        assertEquals(0, pendingAck.getAttempts());
        assertNotNull(pendingAck.getClaimToken());
        expire(entry.getId());
        service = newService();
        assertEquals(1, dispatcher().dispatchDue(database));
        assertTrue(service.findAll(database).isEmpty());
        assertEquals(original, journal());
    }

    @Test
    void enqueueAndClaimUseOneClockAcrossDifferentSessionTimezones() throws Exception {
        final TupleReplicationOutboxEntry entry;
        try (var connection = connection(); var statement = connection.createStatement()) {
            service.ensureTableExists(connection);
            statement.execute("SET time_zone='+09:30'");
            connection.setAutoCommit(false);
            entry = service.enqueue(connection, database, table, HttpMethod.POST, DataReplicationDto.builder().build());
            connection.commit();
        }
        assertTrue(Duration.between(entry.getNextAttemptAt(), Instant.now()).abs().compareTo(Duration.ofSeconds(10)) < 0);
        final var claim = service.claimDue(database, 1, LEASE).getFirst();
        assertEquals(entry.getId(), claim.getId());
        assertEquals(LEASE, Duration.between(claim.getLastModified(), claim.getClaimUntil()));
        assertTrue(Duration.between(Instant.now(), claim.getClaimUntil()).compareTo(Duration.ofMinutes(4)) > 0);
        assertTrue(service.markSucceeded(database, entry.getId(), claim.getClaimToken()));
    }

    private TupleReplicationOutboxServiceMariaDbImpl newService() {
        return new TupleReplicationOutboxServiceMariaDbImpl(new ObjectMapper().findAndRegisterModules());
    }

    private TupleReplicationNotificationDispatcher dispatcher() {
        final var dispatcher = new TupleReplicationNotificationDispatcher(http, mock(MetadataService.class), service);
        ReflectionTestUtils.setField(dispatcher, "retryDelaySeconds", 30L);
        ReflectionTestUtils.setField(dispatcher, "maxRetryDelaySeconds", 900L);
        ReflectionTestUtils.setField(dispatcher, "maxAttempts", 20);
        ReflectionTestUtils.setField(dispatcher, "batchSize", 25);
        ReflectionTestUtils.setField(dispatcher, "processingTimeoutSeconds", 300L);
        return dispatcher;
    }

    private TupleReplicationOutboxEntry enqueue() throws Exception {
        return service.enqueue(database, table, HttpMethod.POST, DataReplicationDto.builder().build());
    }

    private java.util.List<TupleReplicationOutboxServiceMariaDbImpl.JournalEntry> journal() throws Exception {
        try (var connection = connection()) {
            return service.readRange(connection, 0, service.readJournalState(connection).committedThrough(), 100);
        }
    }

    private void makeDue(UUID id) throws Exception {
        backdate(id, "next_attempt_at");
    }

    private void expire(UUID id) throws Exception {
        backdate(id, "claim_until");
    }

    private void backdate(UUID id, String column) throws Exception {
        try (var connection = connection(); var statement = connection.prepareStatement(
                "UPDATE tuple_replication_notification_outbox SET " + column
                        + "=TIMESTAMPADD(SECOND,-1,CURRENT_TIMESTAMP(6)) WHERE id=?")) {
            statement.setString(1, id.toString());
            assertEquals(1, statement.executeUpdate());
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(URL + SCHEMA, "root", password());
    }

    private String password() {
        return System.getenv("RECOVERY_TUPLE_SQL_TEST_PASSWORD");
    }
}
