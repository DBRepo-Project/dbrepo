package at.tuwien.config;

import at.tuwien.mapper.AuthenticationMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Getter
@Configuration
public class GatewayConfig {

    @Value("${fda.gateway.endpoint}")
    private String gatewayEndpoint;

    @Value("${spring.rabbitmq.username}")
    private String brokerUsername;

    @Value("${spring.rabbitmq.password}")
    private String brokerPassword;

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

    @Bean("brokerRestTemplate")
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(gatewayEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicAuthenticationInterceptor(brokerUsername, brokerPassword));
        return restTemplate;
    }
}
