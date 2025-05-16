package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Slf4j
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
