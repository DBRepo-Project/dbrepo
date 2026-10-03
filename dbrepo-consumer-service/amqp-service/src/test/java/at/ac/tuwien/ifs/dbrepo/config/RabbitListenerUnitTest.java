package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RabbitListenerUnitTest {

    private static final UUID DATABASE_ID = UUID.fromString("14904d0e-8ed5-4f41-9084-74cbb3e5b801");
    private static final UUID TABLE_ID = UUID.fromString("e4a2c1ab-4620-431d-a56b-f5b27c047b02");
    private static final String KEY = "dbrepo." + DATABASE_ID + "." + TABLE_ID;
    private final DataServiceGateway gateway = mock(DataServiceGateway.class);
    private final Channel channel = mock(Channel.class);
    private final CompletableFuture<Consumer> consumer = new CompletableFuture<>();
    private SimpleMessageListenerContainer container;

    @BeforeEach
    void setUp() throws Exception {
        final ConnectionFactory factory = mock(ConnectionFactory.class);
        final Connection connection = mock(Connection.class);
        when(factory.createConnection()).thenReturn(connection);
        when(connection.createChannel(false)).thenReturn(channel);
        when(connection.isOpen()).thenReturn(true);
        when(channel.isOpen()).thenReturn(true);
        when(channel.queueDeclarePassive("dbrepo")).thenReturn(mock(AMQP.Queue.DeclareOk.class));
        when(channel.basicConsume(eq("dbrepo"), eq(false), anyString(), anyBoolean(), anyBoolean(), anyMap(), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    final Consumer callback = invocation.getArgument(6);
                    callback.handleConsumeOk("test-consumer");
                    consumer.complete(callback);
                    return "test-consumer";
                });
        doAnswer(invocation -> {
            consumer.join().handleCancelOk("test-consumer");
            return null;
        }).when(channel).basicCancel(anyString());
        final RabbitConfig config = new RabbitConfig(new ObjectMapper(), gateway);
        ReflectionTestUtils.setField(config, "queueName", "dbrepo");
        ReflectionTestUtils.setField(config, "minConcurrent", 1);
        ReflectionTestUtils.setField(config, "maxConcurrent", 1);
        ReflectionTestUtils.setField(config, "connectionTimeout", 10);
        container = config.container(factory);
        container.setAutoDeclare(false);
        container.setShutdownTimeout(1000);
        container.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        container.destroy();
    }

    @Test
    void ackOnlyAfterSuccessfulWrite() throws Exception {
        doAnswer(invocation -> {
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
            return null;
        }).when(gateway).insertRawTuple(any(), any(), any());
        deliver(KEY, "{\"value\":42}");
        verify(channel, timeout(2000)).basicAck(1L, true);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
        verify(gateway).insertRawTuple(DATABASE_ID, TABLE_ID, TupleDto.builder().data(Map.of("value", 42)).build());
    }

    @Test
    void permanentFailureNacksWithoutRetryOrAck() throws Exception {
        doThrow(new DataServiceException("invalid tuple")).when(gateway).insertRawTuple(any(), any(), any());
        deliver(KEY, "{}");
        assertRejected(1);
    }

    @Test
    void missingTableNacksWithoutRetryOrAck() throws Exception {
        doThrow(new TableNotFoundException("missing table")).when(gateway).insertRawTuple(any(), any(), any());
        deliver(KEY, "{}");
        assertRejected(1);
    }

    @Test
    void unexpectedFailureNacksWithoutRetryOrAck() throws Exception {
        doThrow(new IllegalStateException("unexpected")).when(gateway).insertRawTuple(any(), any(), any());
        deliver(KEY, "{}");
        assertRejected(1);
    }

    @Test
    void transientFailureBacksOffThenNacksAfterThreeAttempts() throws Exception {
        doThrow(new RemoteUnavailableException("unavailable")).when(gateway).insertRawTuple(any(), any(), any());
        final long start = System.nanoTime();
        deliver(KEY, "{}");
        verify(gateway, timeout(1000)).insertRawTuple(any(), any(), any());
        verify(gateway, after(200).times(1)).insertRawTuple(any(), any(), any());
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        assertRejected(3);
        assertTrue(System.nanoTime() - start >= TimeUnit.SECONDS.toNanos(3));
    }

    @Test
    void transientFailureThenSuccessAcksOnce() throws Exception {
        doThrow(new RemoteUnavailableException("unavailable")).doNothing()
                .when(gateway).insertRawTuple(any(), any(), any());
        deliver(KEY, "{}");
        verify(channel, timeout(4000)).basicAck(1L, true);
        verify(gateway, times(2)).insertRawTuple(any(), any(), any());
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void malformedMessageIsNackedByContainer() throws Exception {
        deliver(KEY, "not json");
        assertRejected(0);
    }

    @Test
    void wrongRouteIsNackedByContainer() throws Exception {
        deliver("other." + DATABASE_ID + "." + TABLE_ID, "{}");
        assertRejected(0);
    }

    @Test
    void failedReplayWithDeathHeaderIsNotAcknowledged() throws Exception {
        container.start();
        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .headers(Map.of("x-death", List.of(Map.of("queue", "dbrepo", "reason", "rejected", "count", 1L))))
                .build();
        consumer.get(5, TimeUnit.SECONDS).handleDelivery("test-consumer", new Envelope(1, false, "dbrepo", KEY),
                properties, "null".getBytes(StandardCharsets.UTF_8));
        assertRejected(0);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"dbrepo", "dbrepo.a.b", "other.14904d0e-8ed5-4f41-9084-74cbb3e5b801.e4a2c1ab-4620-431d-a56b-f5b27c047b02",
            "dbrepo.1-1-1-1-1.e4a2c1ab-4620-431d-a56b-f5b27c047b02",
            "dbrepo.14904d0e-8ed5-4f41-9084-74cbb3e5b801.1-1-1-1-1",
            "dbrepo.14904d0e-8ed5-4f41-9084-74cbb3e5b801.",
            "dbrepo.14904d0e-8ed5-4f41-9084-74cbb3e5b801.e4a2c1ab-4620-431d-a56b-f5b27c047b02."})
    void invalidRoutingCannotReturnSuccess(String key) {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> container.getMessageListener().onMessage(message(key, "{}")));
        verifyNoInteractions(gateway);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "42", "\"text\"", "", "{", "{} {}"})
    void invalidPayloadCannotReturnSuccess(String body) {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> container.getMessageListener().onMessage(message(KEY, body)));
        verifyNoInteractions(gateway);
    }

    private void deliver(String key, String body) throws Exception {
        container.start();
        consumer.get(5, TimeUnit.SECONDS).handleDelivery("test-consumer", new Envelope(1, false, "dbrepo", key),
                new AMQP.BasicProperties(), body.getBytes(StandardCharsets.UTF_8));
    }

    private void assertRejected(int attempts) throws Exception {
        verify(channel, timeout(5000)).basicNack(eq(1L), anyBoolean(), eq(false));
        container.stop();
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), eq(true));
        verify(gateway, times(attempts)).insertRawTuple(any(), any(), any());
    }

    private Message message(String key, String body) {
        final MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(key);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
