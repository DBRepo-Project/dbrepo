package at.tuwien.config;

import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class GatewayConfig {

    @Bean("brokerRestTemplate")
    public RestTemplate brokerRestTemplate() {
        return new RestTemplate();
    }
}
