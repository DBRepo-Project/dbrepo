package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationStatus;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplicationNotificationDispatcherUnitTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ReplicationNotificationOutboxService outboxService;

    private ReplicationNotificationDispatcher dispatcher;

    @BeforeEach
    public void beforeEach() {
        dispatcher = new ReplicationNotificationDispatcher(restTemplate, outboxService);
        ReflectionTestUtils.setField(dispatcher, "retryDelaySeconds", 30L);
        ReflectionTestUtils.setField(dispatcher, "maxRetryDelaySeconds", 900L);
        ReflectionTestUtils.setField(dispatcher, "maxAttempts", 20);
        ReflectionTestUtils.setField(dispatcher, "batchSize", 25);
    }

    @Test
    public void dispatchShouldSendJsonPayloadAndMarkSucceeded() {
        final ReplicationNotificationOutbox entry = entry();
        when(outboxService.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(restTemplate.exchange(eq("/api/replication/database"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(Void.class))).thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).build());

        assertTrue(dispatcher.dispatch(entry.getId()));

        final ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("/api/replication/database"), eq(HttpMethod.POST), captor.capture(),
                eq(Void.class));
        assertEquals("{\"id\":\"" + entry.getAggregateId() + "\"}", captor.getValue().getBody());
        assertEquals(MediaType.APPLICATION_JSON, ((HttpHeaders) captor.getValue().getHeaders()).getContentType());
        verify(outboxService).markSucceeded(entry.getId());
    }

    @Test
    public void dispatchShouldMarkFailedWithRetryDelay() {
        final ReplicationNotificationOutbox entry = entry();
        when(outboxService.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(restTemplate.exchange(eq("/api/replication/database"), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(Void.class))).thenThrow(new ResourceAccessException("connection refused"));

        dispatcher.dispatch(entry.getId());

        verify(outboxService).markFailed(entry.getId(), "connection refused", Duration.ofSeconds(30), 20);
    }

    @Test
    public void dispatchShouldIgnoreAlreadySucceededEntries() {
        final ReplicationNotificationOutbox entry = entry();
        entry.setStatus(ReplicationNotificationStatus.SUCCEEDED);
        when(outboxService.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertTrue(dispatcher.dispatch(entry.getId()));

        verify(outboxService).findById(entry.getId());
        verifyNoMoreInteractions(outboxService, restTemplate);
    }

    private ReplicationNotificationOutbox entry() {
        final UUID aggregateId = UUID.randomUUID();
        return ReplicationNotificationOutbox.builder()
                .id(UUID.randomUUID())
                .notificationType(ReplicationNotificationType.DATABASE_CREATE)
                .status(ReplicationNotificationStatus.PENDING)
                .httpMethod(HttpMethod.POST.name())
                .path("/api/replication/database")
                .aggregateId(aggregateId)
                .payload("{\"id\":\"" + aggregateId + "\"}")
                .attempts(0)
                .created(Instant.now())
                .nextAttemptAt(Instant.now())
                .build();
    }
}
