package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Log4j2
@Getter
@Configuration
public class GatewayConfig {

    @Value("${fda.gateway.endpoint}")
    private String gatewayEndpoint;

    @Bean("gatewayRestTemplate")
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate =  new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(gatewayEndpoint));
        log.debug("gateway rest template with endpoint={}", gatewayEndpoint);
        return restTemplate;
    }

}
