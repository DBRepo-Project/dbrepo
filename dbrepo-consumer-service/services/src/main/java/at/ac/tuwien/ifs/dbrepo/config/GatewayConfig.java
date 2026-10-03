package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Slf4j
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.dataService}")
    private String dataEndpoint;

    @Value("${dbrepo.system.username}")
    private String systemUsername;

    @Value("${dbrepo.system.password}")
    private String systemPassword;

    @Value("${dbrepo.dataConnectTimeout:5000}")
    private int dataConnectTimeout;

    @Value("${dbrepo.dataReadTimeout:30000}")
    private int dataReadTimeout;

    @Bean
    public RestTemplate restTemplate() {
        if (dataConnectTimeout <= 0 || dataReadTimeout <= 0) {
            throw new IllegalArgumentException("Data service timeouts must be positive");
        }
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(dataConnectTimeout);
        requestFactory.setReadTimeout(dataReadTimeout);
        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dataEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicAuthenticationInterceptor(systemUsername, systemPassword));
        return restTemplate;
    }

}
