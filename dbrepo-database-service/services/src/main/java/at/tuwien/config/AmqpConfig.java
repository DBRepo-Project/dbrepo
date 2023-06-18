package at.tuwien.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ValueExpression;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Getter
@Log4j2
@Configuration
public class AmqpConfig {

    @Value("${spring.rabbitmq.host}")
    private String amqpHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int amqpPort;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${spring.rabbitmq.username}")
    private String amqpUsername;

    @Value("${spring.rabbitmq.password}")
    private String amqpPassword;

    @Bean
    public Channel getChannel() throws IOException, TimeoutException {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpHost);
        factory.setPort(amqpPort);
        factory.setVirtualHost(virtualHost);
        factory.setUsername(amqpUsername);
        factory.setPassword(amqpPassword);
        final Connection connection = factory.newConnection();
        return connection.createChannel();
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpHost);
        factory.setVirtualHost(virtualHost);
        factory.setUsername(amqpUsername);
        factory.setPassword(amqpPassword);
        log.info("Opened connection to AMQP Broker {}", amqpHost);
        log.debug("amqp virtual host: {}", amqpHost);
        log.debug("amqp username: {}", amqpUsername);
        return new CachingConnectionFactory(factory);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        final Expression userId = new ValueExpression<>(amqpUsername);
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setUserIdExpression(userId);
        return rabbitTemplate;
    }

}
