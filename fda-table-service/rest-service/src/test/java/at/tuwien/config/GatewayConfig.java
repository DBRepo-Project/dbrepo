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

    @Value("${spring.rabbitmq.username}")
    private String brokerUsername;

    @Value("${spring.rabbitmq.password}")
    private String brokerPassword;

    @Bean("brokerRestTemplate")
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://broker-service:15672"));
        restTemplate.getInterceptors()
                .add(new BasicAuthenticationInterceptor(brokerUsername, brokerPassword));
        return restTemplate;
    }

}