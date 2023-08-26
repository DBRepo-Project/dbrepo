package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Getter
@Configuration
public class KeycloakConfig {

    @Value("${fda.keycloak.username}")
    private String username;

    @Value("${fda.keycloak.password}")
    private String password;
}
