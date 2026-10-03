package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.auth.BasicRequestInterceptor;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.dataService}")
    private String dataServiceEndpoint;

    @Value("${dbrepo.endpoints.metadataService}")
    private String metadataServiceEndpoint;

    @Value("${dbrepo.system.username}")
    private String systemUsername;

    @Value("${dbrepo.system.password}")
    private String systemPassword;

    @Value("${dbrepo.replication.username}")
    private String replicationUsername;

    @Value("${dbrepo.replication.password}")
    private String replicationPassword;

    @PostConstruct
    void validateServiceUsers() {
        if (systemUsername.equals(replicationUsername)) {
            throw new IllegalStateException("Replication username must differ from system username");
        }
    }

    @Bean("metadataServiceRestTemplate")
    public RestTemplate metadataServiceRestTemplate() {
        final RestTemplate restTemplate = timeoutRestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(metadataServiceEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicRequestInterceptor(systemUsername, systemPassword));
        return restTemplate;
    }

    @Bean("dataServiceRestTemplate")
    public RestTemplate dataServiceRestTemplate() {
        final RestTemplate restTemplate = timeoutRestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dataServiceEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicRequestInterceptor(systemUsername, systemPassword));
        return restTemplate;
    }

    @Bean("externalReplicationRestTemplate")
    public RestTemplate externalReplicationRestTemplate(
            @Value("${dbrepo.replication.allowedSites:}") String allowedSites) {
        final ReplicationPeers peers = new ReplicationPeers(allowedSites);
        final RestTemplate restTemplate = timeoutRestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode status) {
                return status.is3xxRedirection() || super.hasError(status);
            }
        });
        // Validate the destination before attaching shared credentials, including on retries.
        restTemplate.getInterceptors().add((request, body, execution) -> {
            peers.requireAllowedRequest(request.getURI());
            return execution.execute(request, body);
        });
        restTemplate.getInterceptors()
                .add(new BasicRequestInterceptor(replicationUsername, replicationPassword));
        return restTemplate;
    }

    private RestTemplate timeoutRestTemplate() {
        final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

}
