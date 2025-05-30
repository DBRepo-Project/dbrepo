package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;

@Getter
@Slf4j
@Configuration
public class RabbitConfig extends BaseTest {

    @Value("${dbrepo.exchangeName}")
    private String exchangeName;

    @Value("${dbrepo.queueName}")
    private String queueName;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${dbrepo.endpoints.brokerService}")
    private String brokerEndpoint;

    @Bean
    @Primary
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(brokerEndpoint));
        restTemplate.getInterceptors()
                .add(new BasicAuthenticationInterceptor(USER_1_USERNAME, USER_1_PASSWORD));
        return restTemplate;
    }

}
