package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Log4j2
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
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakEndpoint)
                .realm("master")
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .scope(OAuth2Constants.SCOPE_OPENID)
                .username(keycloakUsername)
                .password(keycloakPassword)
                .build();
    }
}
