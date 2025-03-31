package at.tuwien.config;

import at.tuwien.interceptor.KeycloakInterceptor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;

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

    private final ClientHttpRequestInterceptor clientHttpRequestInterceptor;

    @Autowired
    public KeycloakConfig(ClientHttpRequestInterceptor clientHttpRequestInterceptor) {
        this.clientHttpRequestInterceptor = clientHttpRequestInterceptor;
    }

    @Bean("keycloakRestTemplate")
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(keycloakEndpoint));
        restTemplate.getInterceptors()
                .addAll(List.of(new KeycloakInterceptor(keycloakUsername, keycloakPassword, keycloakEndpoint),
                        clientHttpRequestInterceptor));
        return restTemplate;
    }
}
