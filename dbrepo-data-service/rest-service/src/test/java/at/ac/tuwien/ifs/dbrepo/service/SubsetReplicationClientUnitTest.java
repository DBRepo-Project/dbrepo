package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.SubsetReplicationConfig;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SubsetReplicationClientUnitTest {
    private final SubsetReplicationConfig config = new SubsetReplicationConfig();

    @Test
    void routesSubsetsThroughTheTransportSelectorAndRejectsUntrustedTargets() throws Exception {
        final var source = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final var sink = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final var credentials = new AtomicReference<String>();
        final var sinkRequests = new AtomicInteger();
        final String sourceSite = "http://127.0.0.1:" + source.getAddress().getPort();
        final String sinkSite = "http://127.0.0.1:" + sink.getAddress().getPort();
        source.createContext("/api/replication/transports/deliver", request -> {
            credentials.set(request.getRequestHeaders().getFirst("Authorization"));
            assertTrue(new String(request.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).contains("https://peer.example/subset"));
            byte[] response = "{\"status\":409,\"body\":\"e30=\"}".getBytes(StandardCharsets.UTF_8);
            request.getResponseHeaders().set("Content-Type", "application/json");
            request.sendResponseHeaders(200, response.length);
            request.getResponseBody().write(response);
            request.close();
        });
        sink.createContext("/stolen", request -> {
            sinkRequests.incrementAndGet();
            request.sendResponseHeaders(204, -1);
            request.close();
        });
        source.start();
        sink.start();
        try {
            var proxy = new org.springframework.web.client.RestTemplate();
            proxy.setUriTemplateHandler(new org.springframework.web.util.DefaultUriBuilderFactory(sourceSite));
            proxy.getInterceptors().add(new org.springframework.http.client.support.BasicAuthenticationInterceptor("internal", "test-secret"));
            final var client = config.client(new ReplicationPeers("https://peer.example"), proxy);
            assertThrows(org.springframework.web.client.HttpClientErrorException.Conflict.class, () -> client.put("https://peer.example/subset", "{}"));
            assertEquals("Basic " + Base64.getEncoder().encodeToString("internal:test-secret".getBytes(StandardCharsets.UTF_8)),
                    credentials.get());
            assertEquals(0, sinkRequests.get());
            assertThrows(IllegalArgumentException.class, () -> client.put(sinkSite + "/stolen", "{}"));
            assertEquals(0, sinkRequests.get());
        } finally {
            source.stop(0);
            sink.stop(0);
        }
    }

    @Test
    void rejectsUnsafeSites() {
        for (String site : new String[]{"http://peer.example", "https://peer.example/path", "https://user:pass@peer.example",
                "https://peer.example?url=other", "https://peer.example#other"}) {
            assertThrows(IllegalArgumentException.class, () -> config.peers(site));
        }
    }
}
