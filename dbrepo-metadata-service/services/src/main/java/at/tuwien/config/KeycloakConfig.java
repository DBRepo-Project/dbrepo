package at.tuwien.config;

import at.tuwien.interceptor.KeycloakInterceptor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Getter
@Configuration
public class KeycloakConfig {

    @Value("${fda.keycloak.endpoint}")
    private String keycloakEndpoint;

    @Value("${fda.keycloak.username}")
    private String keycloakUsername;

    @Value("${fda.keycloak.password}")
    private String keycloakPassword;

    @Bean("keycloakRestTemplate")
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(keycloakEndpoint));
        restTemplate.getInterceptors()
                .add(new KeycloakInterceptor(keycloakUsername, keycloakPassword, keycloakEndpoint));
        return restTemplate;
    }
}
