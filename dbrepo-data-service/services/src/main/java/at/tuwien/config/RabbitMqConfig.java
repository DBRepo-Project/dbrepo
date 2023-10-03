package at.tuwien.config;

import at.tuwien.listener.DefaultListener;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class RabbitMqConfig {

    @Value("${fda.queueName}")
    private String queueName;

    @Value("${fda.exchangeName}")
    private String exchangeName;

    @Value("${fda.routingKey}")
    private String routingKey;

    @Value("${fda.brokerService.username}")
    private String username;

    @Value("${fda.brokerService.password}")
    private String password;

    @Value("${fda.brokerService.endpoint}")
    private String endpoint;

    @Value("${fda.brokerService.virtualHost}")
    private String virtualHost;

    @Value("${fda.minConcurrent}")
    private Integer minConcurrent;

    @Value("${fda.maxConcurrent}")
    private Integer maxConcurrent;

    @Bean
    public Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(routingKey);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory getSimpleRabbitListenerContainerFactory() {
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(getConnectionFactory());
        factory.setConcurrentConsumers(minConcurrent);
        factory.setMaxConcurrentConsumers(maxConcurrent);
        factory.setConsecutiveActiveTrigger(1);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    @Bean
    public ConnectionFactory getConnectionFactory() {
        log.debug("rabbitmq endpoint: {} -> {}", endpoint, virtualHost);
        final CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setAddresses(endpoint);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
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
