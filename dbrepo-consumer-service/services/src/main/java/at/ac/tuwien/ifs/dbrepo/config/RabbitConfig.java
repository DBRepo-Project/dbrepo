package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

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

    @Value("${dbrepo.connectionTimeout}")
    private Integer connectionTimeout;

    @Bean
    @Profile("!test")
    public ConnectionFactory connectionFactory() throws BrokerServiceConnectionException {
        final CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        try {
            factory.createConnection();
        } catch (AmqpException e) {
            log.error("Failed to connect to broker {}:{}", host, port);
            throw new BrokerServiceConnectionException("Failed to connect to broker", e);
        }
        return factory;
    }

    private final ObjectMapper objectMapper;
    private final DataServiceGateway dataServiceGateway;

    @Autowired
    public RabbitConfig(ObjectMapper objectMapper, DataServiceGateway dataServiceGateway) {
        this.objectMapper = objectMapper;
        this.dataServiceGateway = dataServiceGateway;
    }

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory factory) {
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factory);
        container.setQueueNames(queueName);
        container.setMessageListener(this::insertTuple);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryOperations(RetryTemplate.builder()
                        .maxAttempts(3)
                        .exponentialBackoff(1000, 2, 2000)
                        .retryOn(RemoteUnavailableException.class)
                        .traversingCauses()
                        .build())
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        final ConditionalRejectingErrorHandler errorHandler = new ConditionalRejectingErrorHandler();
        // A manually replayed dead letter must not be silently discarded on another failure.
        errorHandler.setDiscardFatalsWithXDeath(false);
        container.setErrorHandler(errorHandler);
        container.setConcurrentConsumers(minConcurrent);
        container.setMaxConcurrentConsumers(maxConcurrent);
        container.setDefaultRequeueRejected(false);
        container.setPrefetchCount(1);
        container.setReceiveTimeout(connectionTimeout);
        return container;
    }

    private void insertTuple(Message message) {
        final String key = message.getMessageProperties().getReceivedRoutingKey();
        final String[] parts = key == null ? new String[0] : key.split("\\.", -1);
        if (parts.length != 3 || !"dbrepo".equals(parts[0])) {
            throw new AmqpRejectAndDontRequeueException("Expected routing key dbrepo.<database UUID>.<table UUID>");
        }
        try {
            final UUID databaseId = parseId(parts[1]);
            final UUID tableId = parseId(parts[2]);
            final HashMap<String, Object> data = objectMapper.readerFor(new TypeReference<HashMap<String, Object>>() {
            }).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(message.getBody());
            if (data == null) {
                throw new IllegalArgumentException("Tuple must be a JSON object");
            }
            dataServiceGateway.insertRawTuple(databaseId, tableId, TupleDto.builder().data(data).build());
        } catch (RemoteUnavailableException e) {
            throw new AmqpException("Data service temporarily unavailable", e);
        } catch (IOException | IllegalArgumentException | TableNotFoundException | DataServiceException e) {
            throw new AmqpRejectAndDontRequeueException("Tuple cannot be inserted", e);
        }
    }

    private UUID parseId(String value) {
        final UUID id = UUID.fromString(value);
        if (!id.toString().equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Routing key must contain canonical UUIDs");
        }
        return id;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        return new RabbitTemplate(factory);
    }

}
