package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;

@Log4j2
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.gatewayService}")
    private String gatewayEndpoint;

    @Value("${dbrepo.admin.username}")
    private String adminUsername;

    @Value("${dbrepo.admin.password}")
    private String adminPassword;

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(gatewayEndpoint));
        log.debug("add basic authentication for internal gateway: username={}, password=(hidden)", adminUsername);
        restTemplate.getInterceptors()
                .addAll(List.of(new BasicAuthenticationInterceptor(adminUsername, adminPassword),
                        clientHttpRequestInterceptor()));
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

}
