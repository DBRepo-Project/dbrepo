package at.ac.tuwien.ifs.dbrepo.service.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import static at.ac.tuwien.ifs.dbrepo.service.transport.ReplicationAmqpProtocol.*;

@Slf4j
public final class ReplicationAmqp implements AutoCloseable {
    public record Peer(String uri, String queue) { }
    public record Settings(String localUri, Map<String, String> inbound, Map<String, Peer> outbound, boolean allowPlaintext) {
        public Settings {
            inbound = inbound == null ? Map.of() : Map.copyOf(inbound);
            outbound = outbound == null ? Map.of() : Map.copyOf(outbound);
        }
    }

    private final ObjectMapper mapper;
    private final Settings settings;
    private final Function<Request, Reply> handler;
    private final int timeoutMs;
    private final Map<String, Connection> senders = new HashMap<>();
    private Connection receiver;
    private boolean closed;

    public ReplicationAmqp(ObjectMapper mapper, Settings settings, Function<Request, Reply> handler, int timeoutMs) {
        this.mapper = mapper;
        this.settings = settings;
        this.handler = handler;
        this.timeoutMs = timeoutMs;
        if (timeoutMs < 1000 || timeoutMs > 900_000) throw new IllegalArgumentException("Invalid AMQP response timeout");
        if (!settings.inbound().isEmpty()) validateUri(settings.localUri());
        settings.inbound().values().forEach(ReplicationAmqp::validateQueue);
        if (new HashSet<>(settings.inbound().values()).size() != settings.inbound().size()) {
            throw new IllegalArgumentException("Each inbound peer needs its own queue");
        }
        settings.outbound().values().forEach(p -> { validateUri(p.uri()); validateQueue(p.queue()); });
    }

    public Set<String> peers() { return settings.outbound().keySet(); }

    public Reply send(String site, Request request) throws IOException {
        Peer peer = settings.outbound().get(site);
        if (peer == null) throw new IOException("RabbitMQ is not configured for this target site");
        service(request.method(), request.path());
        byte[] bytes = mapper.writeValueAsBytes(request);
        if (bytes.length > MAX_BYTES) throw new IOException("Replication message exceeds the bounded AMQP payload");
        try (Channel channel = connection(site, peer.uri()).createChannel()) {
            channel.confirmSelect();
            String correlation = UUID.randomUUID().toString();
            String replyQueue = peer.queue() + ".reply." + correlation;
            channel.queueDeclare(replyQueue, false, true, true, Map.of("x-expires", timeoutMs * 2));
            channel.queueBind(replyQueue, peer.queue() + ".replies", replyQueue);
            var reply = new CompletableFuture<byte[]>();
            channel.addReturnListener(returned -> reply.completeExceptionally(new IOException("Replication queue is unavailable")));
            channel.addShutdownListener(reason -> reply.completeExceptionally(new IOException("Replication channel closed")));
            channel.basicConsume(replyQueue, true, (tag, delivery) -> {
                if (correlation.equals(delivery.getProperties().getCorrelationId())) reply.complete(delivery.getBody());
            }, tag -> reply.completeExceptionally(new IOException("Replication reply consumer cancelled")));
            var properties = new AMQP.BasicProperties.Builder().contentType("application/json").deliveryMode(2)
                    .correlationId(correlation).replyTo(replyQueue).expiration(Integer.toString(timeoutMs)).build();
            channel.basicPublish(peer.queue(), "request", true, properties, bytes);
            channel.waitForConfirmsOrDie(timeoutMs);
            byte[] response = reply.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (response.length > MAX_BYTES) throw new IOException("Replication reply exceeds size limit");
            Reply result = mapper.readValue(response, Reply.class);
            if (result.status() < 200 || result.status() > 599) throw new IOException("Invalid replication application receipt");
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Replication interrupted; the outbox will retry", e);
        } catch (Exception e) {
            // Do not include connection URIs or credentials in outbox errors.
            throw new IOException("RabbitMQ delivery has no confirmed application result (" + e.getClass().getSimpleName() + ")");
        }
    }

    private synchronized Connection connection(String site, String uri) throws Exception {
        if (closed) throw new IOException("Replication AMQP client is closed");
        Connection existing = senders.get(site);
        if (existing != null && existing.isOpen()) return existing;
        if (existing != null) existing.abort();
        Connection connection = factory(uri).newConnection("dbrepo-replication-publisher");
        senders.put(site, connection);
        return connection;
    }

    /** Called periodically: a broker outage must not prevent REST replication or application startup. */
    public synchronized void startReceiver() {
        if (closed || settings.inbound().isEmpty() || receiver != null && receiver.isOpen()) return;
        if (receiver != null) receiver.abort();
        try {
            receiver = factory(settings.localUri()).newConnection("dbrepo-replication-consumer");
            final Connection consumerConnection = receiver;
            for (var peer : settings.inbound().entrySet()) {
                Channel channel = receiver.createChannel();
                channel.addShutdownListener(reason -> {
                    if (!reason.isInitiatedByApplication()) consumerConnection.abort();
                });
                // Provision queues and their policies administratively; never weaken an existing queue here.
                channel.queueDeclarePassive(peer.getValue());
                channel.basicQos(1);
                channel.confirmSelect();
                channel.basicConsume(peer.getValue(), false,
                        (tag, delivery) -> receive(channel, peer.getValue(), delivery), tag -> consumerConnection.abort());
            }
        } catch (Exception e) {
            if (receiver != null) receiver.abort();
            receiver = null;
            log.warn("Replication AMQP receiver unavailable ({})", e.getClass().getSimpleName());
        }
    }

    private void receive(Channel channel, String queue, Delivery delivery) throws IOException {
        var properties = delivery.getProperties();
        String replyTo = properties.getReplyTo();
        String correlation = properties.getCorrelationId();
        long tag = delivery.getEnvelope().getDeliveryTag();
        Request request;
        try {
            if (delivery.getBody().length > MAX_BYTES || correlation == null
                    || !UUID.fromString(correlation).toString().equals(correlation)
                    || !(queue + ".reply." + correlation).equals(replyTo)) throw new IllegalArgumentException("Invalid envelope");
            request = mapper.readValue(delivery.getBody(), Request.class);
            service(request.method(), request.path());
        } catch (Exception e) {
            log.warn("Rejected invalid replication AMQP envelope on {}", queue);
            channel.basicReject(tag, false);
            return;
        }
        Reply result;
        try {
            result = handler.apply(request);
        } catch (Exception e) {
            result = new Reply(503, "{\"message\":\"Replication destination unavailable\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        try {
            byte[] response = mapper.writeValueAsBytes(result);
            if (response.length > MAX_BYTES) throw new IOException("Replication response exceeds size limit");
            channel.basicPublish(queue + ".replies", replyTo, new AMQP.BasicProperties.Builder().correlationId(correlation)
                    .contentType("application/json").build(), response);
            channel.waitForConfirmsOrDie(timeoutMs);
            // Endpoint completion includes its transaction commit. Lost replies are safe to retry via the existing inbox.
            channel.basicAck(tag, false);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            if (channel.isOpen()) channel.basicNack(tag, false, true);
        }
    }

    private ConnectionFactory factory(String uri) throws Exception {
        var factory = new ConnectionFactory();
        factory.setUri(uri);
        if (URI.create(uri).getScheme().equals("amqps")) {
            factory.useSslProtocol(javax.net.ssl.SSLContext.getDefault());
            factory.enableHostnameVerification();
        }
        factory.setConnectionTimeout(5000);
        factory.setHandshakeTimeout(5000);
        factory.setRequestedHeartbeat(30);
        factory.setChannelRpcTimeout(timeoutMs);
        factory.setMaxInboundMessageBodySize(MAX_BYTES);
        // Recovery is explicit, so a recovered connection cannot silently lose its required consumer channels.
        factory.setAutomaticRecoveryEnabled(false);
        return factory;
    }

    private void validateUri(String value) {
        if (value == null) throw new IllegalArgumentException("AMQP connection is missing");
        URI uri = URI.create(value);
        if (uri.getHost() == null || uri.getRawFragment() != null || uri.getRawQuery() != null
                || !("amqps".equals(uri.getScheme()) || settings.allowPlaintext() && "amqp".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("Replication AMQP requires a valid TLS connection URI");
        }
    }

    private static void validateQueue(String queue) {
        if (queue == null || !queue.matches("dbrepo\\.replication\\.[a-zA-Z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid replication queue name");
        }
    }

    @Override public synchronized void close() {
        closed = true;
        senders.values().forEach(Connection::abort);
        senders.clear();
        if (receiver != null) receiver.abort();
        receiver = null;
    }
}
