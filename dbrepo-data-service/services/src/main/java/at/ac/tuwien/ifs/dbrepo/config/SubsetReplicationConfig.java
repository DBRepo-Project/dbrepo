package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

@Configuration
public class SubsetReplicationConfig {

    @Bean("subsetReplicationPeers")
    public ReplicationPeers peers(@Value("${dbrepo.replication.allowedSites:${REPLICATION_ALLOWED_SITES:}}") String sites) {
        return new ReplicationPeers(sites);
    }

    @Bean("subsetReplicationRestTemplate")
    public RestTemplate client(@Qualifier("subsetReplicationPeers") ReplicationPeers peers,
                               @Qualifier("replicationRestTemplate") RestTemplate replication) {
        final var client = new RestTemplate();
        client.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode status) {
                return status.is3xxRedirection() || super.hasError(status);
            }
        });
        client.getInterceptors().add((request, body, execution) -> {
            peers.requireAllowedRequest(request.getURI());
            final var reply = replication.postForObject("/api/replication/transports/deliver",
                    Map.of("uri", request.getURI().toString(), "method", request.getMethod().name(), "body", body), Reply.class);
            if (reply == null || reply.status() < 200 || reply.status() > 599) {
                throw new java.io.IOException("Missing replication delivery receipt");
            }
            return new ClientHttpResponse() {
                @Override public HttpStatusCode getStatusCode() { return HttpStatusCode.valueOf(reply.status()); }
                @Override public String getStatusText() { return Integer.toString(reply.status()); }
                @Override public void close() { }
                @Override public InputStream getBody() { return new ByteArrayInputStream(reply.body() == null ? new byte[0] : reply.body()); }
                @Override public HttpHeaders getHeaders() {
                    var headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    return headers;
                }
            };
        });
        return client;
    }

    public record Reply(int status, byte[] body) { }
}
