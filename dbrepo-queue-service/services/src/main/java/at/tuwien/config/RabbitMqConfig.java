package at.tuwien.config;

import at.tuwien.listener.Receiver;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

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
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    @Bean
    public ConnectionFactory getConnectionFactory() {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(endpoint);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        return container;
    }

}
