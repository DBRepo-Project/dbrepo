package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Log4j2
@Configuration
public class RabbitConfig {

    @Value("${dbrepo.exchangeName}")
    private String exchangeName;

    @Value("${dbrepo.queueName}")
    private String queueName;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${dbrepo.endpoints.brokerService}")
    private String brokerEndpoint;

}
