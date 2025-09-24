package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.listener.DefaultListener;
import at.ac.tuwien.ifs.dbrepo.listener.ReplicationListener;
import at.ac.tuwien.ifs.dbrepo.listener.ReplicationTimestampListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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

    @Value("${dbrepo.publisher.consumerEnabled:true}")
    private boolean publisherConsumerEnabled;

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
        container.setAutoStartup(publisherConsumerEnabled);
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
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(1);
        container.setMissingQueuesFatal(false);
        container.setExclusive(true);

        container.setAutoStartup(replicationConsumerEnabled);
        return container;
    }

    @Bean
    public MessageListenerAdapter replicationListenerAdapter(ReplicationListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }

    // Auto-declarations for replication topology (local site)
    @Bean
    @ConditionalOnProperty(name = "dbrepo.replication.autoDeclare", havingValue = "true", matchIfMissing = true)
    public Exchange replicationExchange() {
        return ExchangeBuilder.topicExchange(replicationExchangeName).durable(true).build();
    }

    @Bean
    @ConditionalOnProperty(name = "dbrepo.replication.autoDeclare", havingValue = "true", matchIfMissing = true)
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
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // === Replication timestamps publishing topology ===
    @Value("${dbrepo.replication.timestamps.exchangeName:dbrepo-replication-timestamps}")
    private String replicationTimestampsExchangeName;
    @Value("${dbrepo.replication.timestamps.queueName:dbrepo-replication-timestamps}")
    private String replicationTimestampsQueueName;
    @Value("${dbrepo.replication.timestamps.queueNames:}")
    private String replicationTimestampsQueueNamesCsv;
    @Value("${dbrepo.replication.siteId:}")
    private String timestampsSiteId;

    @Bean
    @ConditionalOnProperty(name = "dbrepo.replication.timestamps.autoDeclare", havingValue = "true", matchIfMissing = true)
    public Exchange replicationTimestampsExchange() {
        return ExchangeBuilder.topicExchange(replicationTimestampsExchangeName).durable(true).build();
    }

    @Bean
    @ConditionalOnProperty(name = "dbrepo.replication.timestamps.autoDeclare", havingValue = "true", matchIfMissing = true)
    public Declarables replicationTimestampsQueuesAndBindings() {
        final java.util.List<Declarable> declarables = new java.util.ArrayList<>();

        // declare exchange
        declarables.add(replicationTimestampsExchange());

        // determine queues to declare
        final java.util.List<String> queues = new java.util.ArrayList<>();
        if (replicationTimestampsQueueNamesCsv != null && !replicationTimestampsQueueNamesCsv.isBlank()) {
            java.util.Arrays.stream(replicationTimestampsQueueNamesCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(queues::add);
        } else if (replicationTimestampsQueueName != null && !replicationTimestampsQueueName.isBlank()) {
            queues.add(replicationTimestampsQueueName);
        }

        for (String q : queues) {
            Queue queue = QueueBuilder.durable(q).singleActiveConsumer().build();
            declarables.add(queue);
            if (timestampsSiteId != null && !timestampsSiteId.isBlank()) {
                String bindingKey = "dbrepo." + timestampsSiteId + ".*.*";
                Binding binding = BindingBuilder.bind(queue).to((TopicExchange) replicationTimestampsExchange()).with(bindingKey);
                declarables.add(binding);
            }
        }

        return new Declarables(declarables);
    }

    // Listener container for replication timestamps
    @Value("${dbrepo.replication.timestamps.consumerEnabled:true}")
    private boolean replicationTimestampsConsumerEnabled;

    @Bean
    public SimpleMessageListenerContainer replicationTimestampsContainer(ConnectionFactory connectionFactory,
                                                                         MessageListenerAdapter replicationTimestampsListenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        String[] queueNames;
        if (replicationTimestampsQueueNamesCsv != null && !replicationTimestampsQueueNamesCsv.isBlank()) {
            queueNames = java.util.Arrays.stream(replicationTimestampsQueueNamesCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } else {
            queueNames = new String[]{replicationTimestampsQueueName};
        }
        container.setQueueNames(queueNames);
        container.setMessageListener(replicationTimestampsListenerAdapter);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(1);
        container.setMissingQueuesFatal(false);
        container.setExclusive(true);
        container.setAutoStartup(replicationTimestampsConsumerEnabled);
        return container;
    }

    @Bean
    public MessageListenerAdapter replicationTimestampsListenerAdapter(ReplicationTimestampListener listener) {
        return new MessageListenerAdapter(listener, "onMessage");
    }

}
