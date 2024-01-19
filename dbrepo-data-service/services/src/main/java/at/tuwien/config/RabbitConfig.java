package at.tuwien.config;

import at.tuwien.listener.DefaultListener;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Getter
@Configuration
public class RabbitConfig {

    @Value("${fda.queueName}")
    private String queueName;

    @Value("${fda.exchangeName}")
    private String exchangeName;

    @Value("${fda.routingKey}")
    private String routingKey;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private Integer port;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${fda.minConcurrent}")
    private Integer minConcurrent;

    @Value("${fda.maxConcurrent}")
    private Integer maxConcurrent;

    @Value("${fda.requeueRejected}")
    private Boolean requeueRejected;

    @Value("${fda.connectionTimeout}")
    private Integer connectionTimeout;

    @Bean
    public SimpleRabbitListenerContainerFactory getSimpleRabbitListenerContainerFactory() {
        log.debug("container factory settings: concurrentConsumers={}, maxConcurrentConsumers={}, acknowledgeMode={}, requeueRejected={}",
                minConcurrent, maxConcurrent, AcknowledgeMode.AUTO, requeueRejected);
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(getConnectionFactory());
        factory.setConcurrentConsumers(minConcurrent);
        factory.setMaxConcurrentConsumers(maxConcurrent);
        factory.setConsecutiveActiveTrigger(1);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(requeueRejected);
        return factory;
    }

    @Bean
    public ConnectionFactory getConnectionFactory() {
        log.debug("rabbitmq endpoint: {}:{} -> {}", host, port, virtualHost);
        final CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setAddresses(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(getConnectionFactory());
    }

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, DefaultListener defaultListener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(defaultListener);
        return container;
    }

}
