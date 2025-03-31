package at.tuwien.config;

import at.tuwien.interceptor.KeycloakInterceptor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Log4j2
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.gatewayService}")
    private String gatewayEndpoint;

    @Value("${dbrepo.endpoints.grafana}")
    private String grafanaEndpoint;

    @Value("${dbrepo.admin.username}")
    private String adminUsername;

    @Value("${dbrepo.admin.password}")
    private String adminPassword;

    @Value("${dbrepo.endpoints.dataService}")
    private String dataEndpoint;

    @Value("${dbrepo.endpoints.metadataService}")
    private String metaDataEndpoint;

    @Value("${dbrepo.grafana.username}")
    private String grafanaUsername;

    @Value("${dbrepo.grafana.password}")
    private String grafanaPassword;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("grafanaTemplate")
    public RestTemplate grafanaTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(grafanaEndpoint));
        restTemplate.getInterceptors().add(grafanaHttpRequestInterceptor());
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestInterceptor clientHttpRequestInterceptor() {
        return (request, body, execution) -> {
            final HttpHeaders headers = request.getHeaders();
            headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            return execution.execute(request, body);
        };
    }

    @Bean
    public ClientHttpRequestInterceptor grafanaHttpRequestInterceptor() {
        return (request, body, execution) -> {
            final HttpHeaders headers = request.getHeaders();
            headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(grafanaUsername, grafanaPassword);
            return execution.execute(request, body);
        };
    }

    @Bean("dataServiceRestTemplate")
    public RestTemplate dataServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dataEndpoint));
        log.debug("add basic authentication for internal data service: username={}, password=(hidden)", adminUsername);

        restTemplate.getInterceptors()
                .addAll(List.of(new BasicAuthenticationInterceptor(adminUsername, adminPassword),
                        clientHttpRequestInterceptor()));

        return restTemplate;
    }

    @Bean("metaDataServiceRestTemplate")
    public RestTemplate metaDataServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(metaDataEndpoint));
        restTemplate.getInterceptors().add(clientHttpRequestInterceptor());
        return restTemplate;
    }

}
