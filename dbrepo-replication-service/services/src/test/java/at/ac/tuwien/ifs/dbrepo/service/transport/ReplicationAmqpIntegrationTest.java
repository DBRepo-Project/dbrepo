package at.ac.tuwien.ifs.dbrepo.service.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Run against an isolated broker/vhost, never a production ingestion queue. */
@EnabledIfEnvironmentVariable(named = "REPLICATION_TEST_AMQP_URI", matches = ".+")
class ReplicationAmqpIntegrationTest {
    @TempDir Path directory;

    public record Probe(String site, String username, String password) { }

    @Test
    @EnabledIfEnvironmentVariable(named = "REPLICATION_TEST_API_PROBES", matches = ".+")
    void returnsAuthenticatedPeerApiReceiptsWithoutMutatingExistingDatabases() throws Exception {
        var mapper = new ObjectMapper();
        var probes = mapper.readValue(Path.of(System.getenv("REPLICATION_TEST_API_PROBES")).toFile(), Probe[].class);
        String uri = System.getenv("REPLICATION_TEST_AMQP_URI");
        String queue = System.getenv("REPLICATION_TEST_AMQP_QUEUE");
        var factory = new ConnectionFactory();
        factory.setUri(uri);
        try (var connection = factory.newConnection(); var admin = connection.createChannel()) {
            admin.exchangeDeclare(queue, "direct", true);
            admin.exchangeDeclare(queue + ".replies", "direct", true);
            admin.queueDeclare(queue, true, false, false, Map.of());
            admin.queueBind(queue, queue, "request");
            try {
                for (var probe : probes) {
                    var api = new RestTemplate();
                    api.getInterceptors().add(new org.springframework.http.client.support.BasicAuthenticationInterceptor(probe.username(), probe.password()));
                    var settings = new ReplicationAmqp.Settings(System.getenv("REPLICATION_TEST_AMQP_RECEIVER_URI"),
                            Map.of("https://source.example", queue), Map.of(probe.site(),
                            new ReplicationAmqp.Peer(System.getenv("REPLICATION_TEST_AMQP_PUBLISHER_URI"), queue)), true);
                    try (var rabbit = new ReplicationAmqp(mapper, settings, request -> {
                        var headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
                        try {
                            var result = api.exchange(probe.site() + request.path(), HttpMethod.valueOf(request.method()),
                                    new HttpEntity<>(request.body(), headers), byte[].class);
                            return new ReplicationAmqpProtocol.Reply(result.getStatusCode().value(), result.getBody());
                        } catch (org.springframework.web.client.RestClientResponseException e) {
                            return new ReplicationAmqpProtocol.Reply(e.getStatusCode().value(), e.getResponseBodyAsByteArray());
                        }
                    }, 120000)) {
                        rabbit.startReceiver();
                        String path = "/api/v1/database/00000000-0000-4000-8000-000000000000/replication/snapshots/table/00000000-0000-4000-8000-000000000000/checkpoint-with-inbox";
                        var receipt = rabbit.send(probe.site(), new ReplicationAmqpProtocol.Request("GET", path, new byte[0]));
                        assertEquals(404, receipt.status(), probe.site());
                        assertEquals("error.database.missing", mapper.readTree(receipt.body()).get("code").asText(), probe.site());
                    }
                }
            } finally {
                admin.queueDelete(queue);
                admin.exchangeDelete(queue);
                admin.exchangeDelete(queue + ".replies");
            }
        }
    }

    @Test
    void realBrokerDeliversBoundedHistoryAndLiveOperationsAndWaitsForApplicationCompletion() throws Exception {
        String uri = System.getenv("REPLICATION_TEST_AMQP_URI");
        var factory = new ConnectionFactory();
        factory.setUri(uri);
        String queue = System.getenv().getOrDefault("REPLICATION_TEST_AMQP_QUEUE", "dbrepo.replication.test_" + UUID.randomUUID().toString().replace("-", ""));
        String publisherUri = System.getenv().getOrDefault("REPLICATION_TEST_AMQP_PUBLISHER_URI", uri);
        String receiverUri = System.getenv().getOrDefault("REPLICATION_TEST_AMQP_RECEIVER_URI", uri);
        var mapper = new ObjectMapper();
        var applied = new AtomicInteger();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        UUID database = UUID.randomUUID(), remote = UUID.randomUUID(), table = UUID.randomUUID();
        try (var connection = factory.newConnection(); var admin = connection.createChannel()) {
            admin.exchangeDeclare(queue, "direct", true);
            admin.exchangeDeclare(queue + ".replies", "direct", true);
            admin.queueDeclare(queue, true, false, false, Map.of());
            admin.queueBind(queue, queue, "request");
            var settings = new ReplicationAmqp.Settings(receiverUri, Map.of("https://a.example", queue),
                    Map.of("https://b.example", new ReplicationAmqp.Peer(publisherUri, queue)), true);
            try (var rabbit = new ReplicationAmqp(mapper, settings, request -> {
                applied.incrementAndGet();
                if (request.path().equals("/api/v1/database/replicate")) {
                    return new ReplicationAmqpProtocol.Reply(201, ("{\"id\":\"" + remote + "\"}").getBytes());
                }
                if (request.method().equals("DELETE")) {
                    entered.countDown();
                    try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new RuntimeException(e); }
                }
                return new ReplicationAmqpProtocol.Reply(200, request.body());
            }, 120000)) {
                rabbit.startReceiver();
                var routes = new ReplicationTransportRoutes(mapper, directory.resolve("routes.json"));
                routes.save(database, "https://b.example", ReplicationTransportRoutes.Transport.RABBITMQ, null);
                var client = new RestTemplate();
                client.getInterceptors().add(new ReplicationPeerTransport(routes, rabbit));
                assertEquals(remote.toString(), client.postForObject("https://b.example/api/v1/database/replicate",
                        Map.of("creationId", database), Map.class).get("id"));
                String row = "https://b.example/api/v1/database/" + remote + "/table/" + table + "/data/replicate";
                for (var method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT}) {
                    assertEquals("{\"value\":20}", client.exchange(row, method,
                            new HttpEntity<>("{\"value\":20}"), String.class).getBody());
                }
                // A full-sized history chunk must survive binary/JSON encoding without truncation.
                byte[] chunk = new byte[4 * 1024 * 1024];
                new java.util.Random(4).nextBytes(chunk);
                String history = "https://b.example/api/v1/database/" + remote + "/replication/snapshots/" + UUID.randomUUID() + "/chunks/0";
                assertArrayEquals(chunk, client.exchange(history, HttpMethod.PUT, new HttpEntity<>(chunk), byte[].class).getBody());
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    var deletion = executor.submit(() -> client.exchange(row, HttpMethod.DELETE, new HttpEntity<>("{}"), String.class));
                    assertTrue(entered.await(5, TimeUnit.SECONDS));
                    assertFalse(deletion.isDone(), "Publisher confirm must not be treated as destination commit");
                    release.countDown();
                    assertEquals(200, deletion.get(5, TimeUnit.SECONDS).getStatusCode().value());
                }
                assertEquals(5, applied.get());
                if (!publisherUri.equals(uri)) {
                    var restrictedFactory = new ConnectionFactory();
                    restrictedFactory.setUri(publisherUri);
                    try (var publisher = restrictedFactory.newConnection()) {
                        var readChannel = publisher.createChannel();
                        assertThrows(java.io.IOException.class, () -> readChannel.basicGet(queue, true));
                        assertFalse(readChannel.isOpen());
                        var replyChannel = publisher.createChannel();
                        replyChannel.confirmSelect();
                            var rejected = assertThrows(com.rabbitmq.client.ShutdownSignalException.class, () -> {
                                replyChannel.basicPublish(queue + ".replies", "forged", null, new byte[0]);
                                replyChannel.waitForConfirmsOrDie(5000);
                            });
                        assertEquals(403, ((com.rabbitmq.client.AMQP.Channel.Close) rejected.getReason()).getReplyCode());
                        assertFalse(replyChannel.isOpen());
                    }
                }
                rabbit.close();
                try (var unavailable = new ReplicationAmqp(mapper,
                        new ReplicationAmqp.Settings(null, Map.of(), settings.outbound(), true), r -> null, 1000)) {
                    var offlineClient = new RestTemplate();
                    offlineClient.getInterceptors().add(new ReplicationPeerTransport(routes, unavailable));
                    assertThrows(org.springframework.web.client.ResourceAccessException.class,
                            () -> offlineClient.exchange(row, HttpMethod.PUT, new HttpEntity<>("{}"), String.class));
                }
            } finally {
                release.countDown();
                admin.queueDelete(queue);
                admin.exchangeDelete(queue);
                admin.exchangeDelete(queue + ".replies");
            }
        }
    }
}
