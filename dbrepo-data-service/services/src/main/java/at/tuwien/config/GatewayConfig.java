package at.tuwien.config;

import at.tuwien.auth.InternalRequestInterceptor;
import at.tuwien.gateway.KeycloakGateway;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Log4j2
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.metadataService}")
    private String metadataEndpoint;

    @Value("${dbrepo.endpoints.analyseService}")
    private String analyseEndpoint;

    @Value("${dbrepo.system.username}")
    private String systemUsername;

    @Value("${dbrepo.system.password}")
    private String systemPassword;

    private final KeycloakGateway keycloakGateway;

    @Autowired
    public GatewayConfig(KeycloakGateway keycloakGateway) {
        this.keycloakGateway = keycloakGateway;
    }

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(metadataEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(this, keycloakGateway));
        return restTemplate;
    }

}
