package at.tuwien.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Getter
@Log4j2
@Configuration
public class AmqpConfig {

    @Value("${spring.rabbitmq.host}")
    private String ampqHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int ampqPort;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${spring.rabbitmq.username}")
    private String amqpUsername;

    @Value("${spring.rabbitmq.password}")
    private String amqpPassword;

    @Value("${fda.consumers}")
    private Integer amqpConsumers;

    @Bean
    public ConnectionFactory connectionFactory() {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(ampqHost);
        factory.setPort(ampqPort);
        factory.setVirtualHost(virtualHost);
        factory.setUsername(amqpUsername);
        factory.setPassword(amqpPassword);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(10000) /* attempt recovery every 10 seconds */;
        log.debug("broker service host={}, username={}, password=(hidden)", ampqHost, amqpUsername);
        return factory;
    }

    @Bean
    public Channel getChannel(ConnectionFactory factory) throws IOException, TimeoutException {
        final Connection connection = factory.newConnection();
        return connection.createChannel();
    }

}
