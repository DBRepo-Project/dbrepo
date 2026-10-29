package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.auth.BasicRequestInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Slf4j
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.metadataService}")
    private String metadataEndpoint;

    @Value("${dbrepo.endpoints.analyseService}")
    private String analyseEndpoint;

    @Value("${dbrepo.endpoints.replicationService}")
    private String replicationEndpoint;

    @Value("${dbrepo.system.username}")
    private String systemUsername;

    @Value("${dbrepo.system.password}")
    private String systemPassword;

    @Value("${dbrepo.replication.username}")
    private String replicationUsername;

    @Value("${dbrepo.replication.password}")
    private String replicationPassword;

    @Value("${dbrepo.replication.requestTimeoutMs:270000}")
    private int replicationRequestTimeoutMs = 270_000;

    @PostConstruct
    void validateServiceUsers() {
        if (systemUsername.equals(replicationUsername)) {
            throw new IllegalStateException("Replication username must differ from system username");
        }
    }

    @Bean("metadataServiceRestTemplate")
    public RestTemplate metadataServiceRestTemplate() {
        final RestTemplate restTemplate = timeoutRestTemplate(30_000);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(metadataEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicRequestInterceptor(this));
        return restTemplate;
    }

    @Bean("replicationRestTemplate")
    public RestTemplate replicationRestTemplate() {
        if (replicationRequestTimeoutMs <= 0) throw new IllegalArgumentException("Replication request timeout must be positive");
        final RestTemplate restTemplate = timeoutRestTemplate(replicationRequestTimeoutMs);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(replicationEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicRequestInterceptor(this));
        return restTemplate;
    }

    private RestTemplate timeoutRestTemplate(int readTimeoutMs) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(factory);
    }

}
