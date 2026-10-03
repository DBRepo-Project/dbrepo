package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;

@Configuration
public class SubsetReplicationConfig {

    @Bean("subsetReplicationPeers")
    public ReplicationPeers peers(@Value("${dbrepo.replication.allowedSites:${REPLICATION_ALLOWED_SITES:}}") String sites) {
        return new ReplicationPeers(sites);
    }

    @Bean("subsetReplicationRestTemplate")
    public RestTemplate client(@Qualifier("subsetReplicationPeers") ReplicationPeers peers,
                               @Value("${dbrepo.replication.username}") String username,
                               @Value("${dbrepo.replication.password}") String password,
                               @Value("${dbrepo.system.username}") String systemUsername) {
        if (username.isBlank() || password.isBlank() || username.equals(systemUsername)) {
            throw new IllegalStateException("Subset replication requires distinct replication credentials");
        }
        final var factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        final var client = new RestTemplate(factory);
        client.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode status) {
                return status.is3xxRedirection() || super.hasError(status);
            }
        });
        client.getInterceptors().add((request, body, execution) -> {
            peers.requireAllowedRequest(request.getURI());
            return execution.execute(request, body);
        });
        client.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
        return client;
    }
}
