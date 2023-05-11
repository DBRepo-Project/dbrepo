package at.tuwien.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class GatewayConfig {

    @Value("${fda.gateway.endpoint}")
    private String gatewayEndpoint;

    @Value("${spring.rabbitmq.username}")
    private String brokerUsername;

    @Value("${spring.rabbitmq.password}")
    private String brokerPassword;

    @Value("${fda.datacite.url}")
    private String dataCiteUrl;

    @Value("${fda.datacite.prefix}")
    private String dataCitePrefix;

    @Value("${fda.datacite.username}")
    private String dataCiteUsername;

    @Value("${fda.datacite.password}")
    private String dataCitePassword;

    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate =  new RestTemplate();
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

    @Bean("dataCiteRestTemplate")
    public RestTemplate dataciteRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dataCiteUrl));
        restTemplate.getInterceptors()
                .add(new BasicAuthenticationInterceptor(dataCiteUsername, dataCitePassword));
        return restTemplate;
    }

}
