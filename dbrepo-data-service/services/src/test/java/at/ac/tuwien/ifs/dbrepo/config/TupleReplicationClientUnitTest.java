package at.ac.tuwien.ifs.dbrepo.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TupleReplicationClientUnitTest {
    @Test
    void discoveryAndDeliveryHaveBoundedTransportAndKeepRoutingAndAuthentication() {
        final var config = new GatewayConfig();
        ReflectionTestUtils.setField(config, "replicationEndpoint", "http://replication.example");
        ReflectionTestUtils.setField(config, "metadataEndpoint", "http://metadata.example");
        final var clients = Map.of("http://replication.example", config.replicationRestTemplate(),
                "http://metadata.example", config.metadataServiceRestTemplate());
        clients.forEach((url, client) -> {
            final var factory = assertInstanceOf(SimpleClientHttpRequestFactory.class,
                    ReflectionTestUtils.getField(client, "requestFactory"));
            assertEquals(10_000, ReflectionTestUtils.getField(factory, "connectTimeout"));
            assertEquals(30_000, ReflectionTestUtils.getField(factory, "readTimeout"));
            assertEquals(url + "/api", client.getUriTemplateHandler().expand("/api", Map.of()).toString());
            assertEquals(1, client.getInterceptors().size());
        });
    }
}
