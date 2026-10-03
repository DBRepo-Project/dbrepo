package at.ac.tuwien.ifs.dbrepo.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ExternalReplicationClientUnitTest {

    private RestTemplate client(String sites) {
        final GatewayConfig config = new GatewayConfig();
        ReflectionTestUtils.setField(config, "replicationUsername", "replication");
        ReflectionTestUtils.setField(config, "replicationPassword", "secret");
        return config.externalReplicationRestTemplate(sites);
    }

    @Test
    void untrustedDestinationReceivesNoRequest() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicInteger requests = new AtomicInteger();
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            assertThrows(IllegalArgumentException.class, () -> client("https://trusted.example")
                    .getForEntity("http://127.0.0.1:" + server.getAddress().getPort() + "/api", String.class));
            assertEquals(0, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303, 307, 308})
    void trustedDestinationReceivesCredentialsButRedirectsAreNeverFollowed(int status) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicReference<String> authorization = new AtomicReference<>();
        final AtomicInteger redirectedRequests = new AtomicInteger();
        server.createContext("/api", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            exchange.getResponseHeaders().set(HttpHeaders.LOCATION, "/capture");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.createContext("/capture", exchange -> {
            redirectedRequests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            final String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            assertThrows(RestClientException.class, () -> client(origin).getForEntity(origin + "/api", String.class));
            assertEquals("Basic cmVwbGljYXRpb246c2VjcmV0", authorization.get());
            assertEquals(0, redirectedRequests.get());
        } finally {
            server.stop(0);
        }
    }
}
