package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.auth.InternalRequestInterceptor;
import at.ac.tuwien.ifs.dbrepo.service.CredentialService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private final CredentialService credentialService;

    @Autowired
    public GatewayConfig(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    public String getReplicationEndpoint() { return replicationEndpoint; }

    @Bean
    public RestTemplate internalRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(metadataEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestTemplate replicationRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(replicationEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

    @Bean
    public RestTemplate externalReplicationRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

}
