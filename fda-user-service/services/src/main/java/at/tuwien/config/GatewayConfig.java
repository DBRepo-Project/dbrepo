package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Getter
@Configuration
public class GatewayConfig {

    @Value("${fda.gateway.endpoint}")
    private String gatewayEndpoint;

    @Value("${fda.keycloak.endpoint}")
    private String keycloakEndpoint;

    @Value("${fda.keycloak.username}")
    private String keycloakUsername;

    @Value("${fda.keycloak.password}")
    private String keycloakPassword;

    @Bean
    public RestTemplate gatewayRestTemplate() {
        final RestTemplate restTemplate =  new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(gatewayEndpoint));
        return restTemplate;
    }

    @Bean
    public RestTemplate keycloakRestTemplate() {
        final RestTemplate restTemplate =  new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(keycloakEndpoint));
        return restTemplate;
    }

}
