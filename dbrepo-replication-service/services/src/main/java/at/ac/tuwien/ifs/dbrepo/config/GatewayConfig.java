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
    public RestTemplate externalReplicationRestTemplate() {
        final RestTemplate restTemplate = timeoutRestTemplate();
        restTemplate.getInterceptors()
                .add(new BasicRequestInterceptor(replicationUsername, replicationPassword));
        return restTemplate;
    }

    private RestTemplate timeoutRestTemplate() {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }

}
