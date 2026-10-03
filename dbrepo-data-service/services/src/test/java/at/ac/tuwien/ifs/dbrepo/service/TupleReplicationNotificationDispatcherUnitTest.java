package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TupleReplicationNotificationDispatcherUnitTest {
    @Mock private RestTemplate http;
    @Mock private MetadataService metadata;
    @Mock private TupleReplicationOutboxService outbox;
    private final Database database = Database.builder().id(UUID.randomUUID()).internalName("test").build();
    private TupleReplicationNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new TupleReplicationNotificationDispatcher(http, metadata, outbox);
        ReflectionTestUtils.setField(dispatcher, "retryDelaySeconds", 30L);
        ReflectionTestUtils.setField(dispatcher, "maxRetryDelaySeconds", 900L);
        ReflectionTestUtils.setField(dispatcher, "maxAttempts", 20);
        ReflectionTestUtils.setField(dispatcher, "batchSize", 2);
        ReflectionTestUtils.setField(dispatcher, "processingTimeoutSeconds", 300L);
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504, 400, 401, 403, 404, 409, 422})
    void responseFailuresAreClassifiedAndFenced(int status) throws Exception {
        final var entry = claimed();
        entry.setAttempts(20);
        when(outbox.claim(database, entry.getId(), Duration.ofMinutes(5))).thenReturn(Optional.of(entry));
        when(http.exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(status).build());
        assertFalse(dispatcher.dispatch(database, entry.getId()));
        verify(outbox).markFailed(eq(database), eq(entry.getId()), eq(entry.getClaimToken()), anyString(),
                eq(Duration.ofSeconds(900)), eq(20), eq(status == 408 || status == 429 || status >= 500));
        verify(outbox, never()).markSucceeded(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({"0,30", "1,60", "4,480", "5,900", "20,900", "2147483647,900"})
    void transportBackoffIsBoundedWithoutAttemptOverflow(int attempts, long seconds) throws Exception {
        final var entry = claimed();
        entry.setAttempts(attempts);
        when(outbox.claim(database, entry.getId(), Duration.ofMinutes(5))).thenReturn(Optional.of(entry));
        when(http.exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("offline"));
        assertFalse(dispatcher.dispatch(database, entry.getId()));
        verify(outbox).markFailed(database, entry.getId(), entry.getClaimToken(), "offline",
                Duration.ofSeconds(seconds), 20, true);
    }

    @Test
    void staleAcknowledgementIsNotReportedAsSuccess() throws Exception {
        final var entry = claimed();
        when(outbox.claim(database, entry.getId(), Duration.ofMinutes(5))).thenReturn(Optional.of(entry));
        accepted();
        assertFalse(dispatcher.dispatch(database, entry.getId()));
        verify(outbox).markSucceeded(database, entry.getId(), entry.getClaimToken());
        verify(outbox, never()).markFailed(any(), any(), any(), any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void failedSuccessPersistenceLeavesLeaseForRecovery() throws Exception {
        final var entry = claimed();
        when(outbox.claim(database, entry.getId(), Duration.ofMinutes(5))).thenReturn(Optional.of(entry));
        when(outbox.markSucceeded(database, entry.getId(), entry.getClaimToken())).thenThrow(new SQLException("offline"));
        accepted();
        assertFalse(dispatcher.dispatch(database, entry.getId()));
        verify(outbox, never()).markFailed(any(), any(), any(), any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void dueBatchClaimsJustBeforeEachSendAndIncludesDatabasesWithoutCurrentReplicaUrls() throws Exception {
        final var first = claimed();
        final var second = claimed();
        when(metadata.getDatabases()).thenReturn(List.of(database));
        when(outbox.claimDue(database, 1, Duration.ofMinutes(5))).thenReturn(List.of(first), List.of(second));
        when(outbox.markSucceeded(eq(database), any(), any())).thenReturn(true);
        accepted();
        assertEquals(2, dispatcher.dispatchDue());
        final var order = inOrder(outbox, http);
        for (var entry : List.of(first, second)) {
            order.verify(outbox).claimDue(database, 1, Duration.ofMinutes(5));
            order.verify(http).exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
            order.verify(outbox).markSucceeded(database, entry.getId(), entry.getClaimToken());
        }
        order.verifyNoMoreInteractions();
    }

    @Test
    void unclaimedWorkIsNeverSent() throws Exception {
        final var id = UUID.randomUUID();
        when(outbox.claim(database, id, Duration.ofMinutes(5))).thenReturn(Optional.empty());
        assertFalse(dispatcher.dispatch(database, id));
        verifyNoInteractions(http);
    }

    private void accepted() {
        when(http.exchange(eq("/api/replication/data"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted().build());
    }

    private TupleReplicationOutboxEntry claimed() {
        return TupleReplicationOutboxEntry.builder().id(UUID.randomUUID()).claimToken(UUID.randomUUID())
                .status(TupleReplicationOutboxStatus.PROCESSING).httpMethod(HttpMethod.POST).payloadJson("{}").build();
    }
}
