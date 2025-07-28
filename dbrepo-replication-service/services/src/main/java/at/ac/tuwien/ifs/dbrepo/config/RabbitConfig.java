package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.listener.DefaultListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Slf4j
@Configuration
public class RabbitConfig {

    @Value("${dbrepo.queueName}")
    private String queueName;

    @Value("${dbrepo.exchangeName}")
    private String exchangeName;

    @Value("${dbrepo.routingKey}")
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

    @Value("${dbrepo.minConcurrent}")
    private Integer minConcurrent;

    @Value("${dbrepo.maxConcurrent}")
    private Integer maxConcurrent;

    @Value("${dbrepo.requeueRejected}")
    private Boolean requeueRejected;

    @Value("${dbrepo.connectionTimeout}")
    private Integer connectionTimeout;

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        container.setConcurrentConsumers(minConcurrent);
        container.setMaxConcurrentConsumers(maxConcurrent);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(DefaultListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

}
