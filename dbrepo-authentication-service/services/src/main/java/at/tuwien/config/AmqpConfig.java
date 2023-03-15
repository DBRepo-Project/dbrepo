package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Getter
@Configuration
public class AmqpConfig {

    @Value("${spring.rabbitmq.username}")
    private String amqpUsername;

    @Value("${spring.rabbitmq.password}")
    private String amqpPassword;

}
