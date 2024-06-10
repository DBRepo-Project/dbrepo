package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.util.List;

@Log4j2
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.brokerService}")
    private String brokerEndpoint;

    @Value("${dbrepo.endpoints.dataService}")
    private String dataEndpoint;

    @Value("${dbrepo.endpoints.analyseService}")
    private String analyseEndpoint;

    @Value("${dbrepo.endpoints.searchService}")
    private String searchEndpoint;

    @Value("${spring.rabbitmq.username}")
    private String brokerUsername;

    @Value("${spring.rabbitmq.password}")
    private String brokerPassword;

    @Value("${dbrepo.admin.username}")
    private String adminUsername;

    @Value("${dbrepo.admin.password}")
    private String adminPassword;

    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("brokerRestTemplate")
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(brokerEndpoint));
        log.debug("add basic authentication for broker service: username={}, password=(hidden)", brokerUsername);
        restTemplate.getInterceptors()
                .addAll(List.of(new BasicAuthenticationInterceptor(brokerUsername, brokerPassword),
                        clientHttpRequestInterceptor()));
        return restTemplate;
    }

    @Bean("dataServiceRestTemplate")
    public RestTemplate dataServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dataEndpoint));
        log.debug("add basic authentication for data service: username={}, password=(hidden)", adminUsername);
        restTemplate.getInterceptors()
                .addAll(List.of(new BasicAuthenticationInterceptor(adminUsername, adminPassword),
                        clientHttpRequestInterceptor()));
        return restTemplate;
    }

    @Bean("analyseServiceRestTemplate")
    public RestTemplate analyseServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(analyseEndpoint));
        log.debug("add basic authentication for analyse service: username={}, password=(hidden)", adminUsername);
        restTemplate.getInterceptors()
                .addAll(List.of(new BasicAuthenticationInterceptor(adminUsername, adminPassword),
                        clientHttpRequestInterceptor()));
        return restTemplate;
    }

    @Bean("searchServiceRestTemplate")
    public RestTemplate searchServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(searchEndpoint));
        log.debug("add basic authentication for search service: username={}, password=(hidden)", adminUsername);
        restTemplate.getInterceptors()
                .addAll(List.of(new BasicAuthenticationInterceptor(adminUsername, adminPassword),
                        clientHttpRequestInterceptor()));
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestInterceptor clientHttpRequestInterceptor() {
        return (request, body, execution) -> {
            final HttpHeaders headers = request.getHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            return execution.execute(request, body);
        };
    }

}
