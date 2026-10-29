package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.service.outbox.FileReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.transport.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@Configuration
public class ReplicationTransportConfig {
    @Bean
    public ReplicationTransportRoutes transportRoutes(ObjectMapper mapper, FileReplicationOutboxService outbox,
            @Value("${dbrepo.replication.outbox.path}") String path) throws IOException {
        outbox.initialize();
        return new ReplicationTransportRoutes(mapper, Path.of(path + ".transports.json"));
    }

    @Bean(destroyMethod = "close")
    public ReplicationAmqp replicationAmqp(ObjectMapper mapper,
            @Value("${REPLICATION_AMQP_CONFIG:}") String config,
            @Value("${REPLICATION_AMQP_TIMEOUT_MS:120000}") int timeout,
            @Value("${dbrepo.replication.allowedSites:}") String allowedSites,
            @Value("${dbrepo.endpoints.dataService}") String data,
            @Value("${dbrepo.endpoints.metadataService}") String metadata,
            @Value("${dbrepo.replication.username}") String username,
            @Value("${dbrepo.replication.password}") String password) throws IOException {
        var settings = config.isBlank() ? new ReplicationAmqp.Settings(null, Map.of(), Map.of(), false)
                : mapper.readValue(Path.of(config).toFile(), ReplicationAmqp.Settings.class);
        var peers = new ReplicationPeers(allowedSites);
        java.util.stream.Stream.concat(settings.inbound().keySet().stream(), settings.outbound().keySet().stream())
                .forEach(site -> {
                    if (!site.equals(peers.requireAllowedSite(site))) {
                        throw new IllegalArgumentException("AMQP peer URLs must use their canonical form without a trailing slash");
                    }
                });
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofMillis(timeout));
        var client = new RestTemplate(factory);
        client.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
        return new ReplicationAmqp(mapper, settings, request -> {
            String endpoint = ReplicationAmqpProtocol.service(request.method(), request.path()).equals("data") ? data : metadata;
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            try {
                var response = client.exchange(endpoint + request.path(), HttpMethod.valueOf(request.method()),
                        new HttpEntity<>(request.body(), headers), byte[].class);
                return new ReplicationAmqpProtocol.Reply(response.getStatusCode().value(), response.getBody());
            } catch (RestClientResponseException e) {
                return new ReplicationAmqpProtocol.Reply(e.getStatusCode().value(), e.getResponseBodyAsByteArray());
            }
        }, timeout);
    }

    @Bean
    public ReplicationPeerTransport peerTransport(ReplicationTransportRoutes routes, ReplicationAmqp rabbit) {
        return new ReplicationPeerTransport(routes, rabbit);
    }

    @Bean
    public Receiver receiver(ReplicationAmqp rabbit) { return new Receiver(rabbit); }

    public record Receiver(ReplicationAmqp rabbit) {
        @Scheduled(fixedDelay = 10000)
        public void connect() { rabbit.startReceiver(); }
    }
}
