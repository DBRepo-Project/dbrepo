package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.auth.BasicRequestInterceptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Slf4j
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.metadataService}")
    private String metadataEndpoint;

    @Value("${dbrepo.endpoints.analyseService}")
    private String analyseEndpoint;

    @Value("${dbrepo.system.username}")
    private String systemUsername;

    @Value("${dbrepo.system.password}")
    private String systemPassword;

    @Bean("metadataServiceRestTemplate")
    public RestTemplate metadataServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(metadataEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicRequestInterceptor(this));
        return restTemplate;
    }

}
