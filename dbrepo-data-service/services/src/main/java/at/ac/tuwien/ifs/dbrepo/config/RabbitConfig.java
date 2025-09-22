package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.listener.DefaultListener;
import at.ac.tuwien.ifs.dbrepo.listener.ReplicationListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

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
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(1);
        container.setExclusive(true);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(DefaultListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }

    // Replication listener configuration
    @Value("${dbrepo.replication.queueName:dbrepo-replication}")
    private String replicationQueueName;
    @Value("${dbrepo.replication.queueNames:}")
    private String replicationQueueNamesCsv;
    @Value("${dbrepo.replication.consumerEnabled:true}")
    private boolean replicationConsumerEnabled;
    @Value("${dbrepo.replication.exchangeName:dbrepo-replication}")
    private String replicationExchangeName;
    @Value("${dbrepo.replication.siteId:}")
    private String siteId;

    @Bean
    public SimpleMessageListenerContainer replicationContainer(ConnectionFactory connectionFactory,
                                                               MessageListenerAdapter replicationListenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        String[] queueNames;
        if (replicationQueueNamesCsv != null && !replicationQueueNamesCsv.isBlank()) {
            queueNames = java.util.Arrays.stream(replicationQueueNamesCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } else {
            queueNames = new String[]{replicationQueueName};
        }
        container.setQueueNames(queueNames);
        container.setMessageListener(replicationListenerAdapter);
        container.setConcurrentConsumers(minConcurrent);
        container.setMaxConcurrentConsumers(maxConcurrent);
        container.setMissingQueuesFatal(false);
        container.setAutoStartup(replicationConsumerEnabled);
        return container;
    }

    @Bean
    public MessageListenerAdapter replicationListenerAdapter(ReplicationListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }

    // Auto-declarations for replication topology (local site)
    @Bean
    @ConditionalOnProperty(name = "dbrepo.replication.autoDeclare", havingValue = "true")
    public Exchange replicationExchange() {
        return ExchangeBuilder.topicExchange(replicationExchangeName).durable(true).build();
    }

    @Bean
    @ConditionalOnProperty(name = "dbrepo.replication.autoDeclare", havingValue = "true")
    public Declarables replicationQueuesAndBindings() {
        final java.util.List<Declarable> declarables = new java.util.ArrayList<>();

        // declare exchange
        declarables.add(replicationExchange());

        // determine queues to declare
        final java.util.List<String> queues = new java.util.ArrayList<>();
        if (replicationQueueNamesCsv != null && !replicationQueueNamesCsv.isBlank()) {
            java.util.Arrays.stream(replicationQueueNamesCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(queues::add);
        } else if (replicationQueueName != null && !replicationQueueName.isBlank()) {
            queues.add(replicationQueueName);
        }

        // declare each queue and bind by siteId if available; otherwise, skip binding and rely on external policy
        for (String q : queues) {
            Queue queue = QueueBuilder.durable(q).singleActiveConsumer().build();
            declarables.add(queue);
            if (siteId != null && !siteId.isBlank()) {
                String bindingKey = "dbrepo." + siteId + ".*.*";
                Binding binding = BindingBuilder.bind(queue).to((TopicExchange) replicationExchange()).with(bindingKey);
                declarables.add(binding);
            }
        }

        return new Declarables(declarables);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

}
