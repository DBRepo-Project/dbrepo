package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
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

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dataEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicAuthenticationInterceptor(systemUsername, systemPassword));
        return restTemplate;
    }

}
