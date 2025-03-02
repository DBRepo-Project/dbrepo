package at.tuwien.config;

import lombok.Getter;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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

    private final String realm = "dbrepo";

    @Bean
    public Keycloak keycloak() {
        return Keycloak.getInstance(keycloakEndpoint, "master", keycloakUsername, keycloakPassword, "admin-cli");
    }
}
