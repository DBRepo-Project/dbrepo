package at.tuwien.config;

import at.tuwien.interceptor.KeycloakInterceptor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Log4j2
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.keycloak}")
    private String keycloakEndpoint;

    @Value("${dbrepo.keycloak.username}")
    private String keycloakUsername;

    @Value("${dbrepo.keycloak.password}")
    private String keycloakPassword;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("keycloakRestTemplate")
    public RestTemplate keycloakRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(keycloakEndpoint));
        restTemplate.getInterceptors()
                .add(new KeycloakInterceptor(restTemplate(), keycloakUsername, keycloakPassword, keycloakEndpoint));
        return restTemplate;
    }

}
