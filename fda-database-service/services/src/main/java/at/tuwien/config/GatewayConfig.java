package at.tuwien.config;

import lombok.Getter;
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

    @Bean("authenticationRestTemplate")
    public RestTemplate authenticationRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(gatewayEndpoint));
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
