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

    @Value("${dbrepo.endpoints.authService}")
    private String keycloakEndpoint;

    @Value("${dbrepo.keycloak.username}")
    private String keycloakUsername;

    @Value("${dbrepo.keycloak.password}")
    private String keycloakPassword;

    @Value("${dbrepo.keycloak.client}")
    private String keycloakClient;

    @Value("${dbrepo.keycloak.clientSecret}")
    private String keycloakClientSecret;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("keycloakRestTemplate")
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(keycloakEndpoint));
        restTemplate.getInterceptors()
                .add(new KeycloakInterceptor(restTemplate(), keycloakUsername, keycloakPassword, keycloakEndpoint));
        return restTemplate;
    }
}
