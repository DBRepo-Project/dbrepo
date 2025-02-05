package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Getter
@Configuration
public class KeycloakConfig {

    @Value("${dbrepo.endpoints.keycloak}")
    private String keycloakEndpoint;

    @Bean
    public Keycloak keycloak() {
        return Keycloak.getInstance(keycloakEndpoint, "master", "admin", "admin", "admin-cli");
    }

}
