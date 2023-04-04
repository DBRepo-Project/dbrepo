package at.tuwien.config;

import at.tuwien.mapper.AuthenticationMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${fda.keycloak.username}")
    private String keycloakUsername;

    @Value("${fda.keycloak.password}")
    private String keycloakPassword;

    private final AuthenticationMapper authenticationMapper;

    @Autowired
    public GatewayConfig(AuthenticationMapper authenticationMapper) {
        this.authenticationMapper = authenticationMapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate =  new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(gatewayEndpoint));
        restTemplate.getMessageConverters().add(authenticationMapper.mappingJackson2HttpMessageConverter());
        return restTemplate;
    }
}
