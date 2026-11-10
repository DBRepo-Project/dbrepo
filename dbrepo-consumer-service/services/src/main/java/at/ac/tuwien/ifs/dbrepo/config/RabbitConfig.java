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
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
        final ConditionalRejectingErrorHandler errors = new ConditionalRejectingErrorHandler() {
            @Override
            protected void log(Throwable error) {
                log.warn("Consumer delivery failed; retry/dead-letter policy determines its disposition");
            }
        };
        errors.setDiscardFatalsWithXDeath(false);
        container.setErrorHandler(errors);
        container.setMessageListener(message -> {
            final MessageProperties properties = message.getMessageProperties();
            final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
            };
            final String publisher = properties.getReceivedUserId();
            if (publisher == null || publisher.isBlank()) {
                throw new AmqpRejectAndDontRequeueException("Missing broker-validated publisher identity");
            }
            if (properties.getReceivedRoutingKey() == null || !properties.getReceivedRoutingKey().contains(".")) {
                throw new AmqpRejectAndDontRequeueException("Missing ingestion routing key");
            }
            final String[] parts = properties.getReceivedRoutingKey().split("\\.", -1);
            if (parts.length != 3 || !"dbrepo".equals(parts[0])) {
                throw new AmqpRejectAndDontRequeueException("Invalid ingestion routing key");
            }
            final TupleDto payload;
            final UUID databaseId, tableId;
            try {
                payload = TupleDto.builder().data(objectMapper.readerFor(typeRef).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(message.getBody())).build();
                if (payload.getData() == null) {
                    throw new IllegalArgumentException("Tuple must be a JSON object");
                }
                databaseId = parseId(parts[1]);
                tableId = parseId(parts[2]);
            } catch (IOException | IllegalArgumentException e) {
                throw new AmqpRejectAndDontRequeueException("Malformed ingestion tuple or target");
            }
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    dataServiceGateway.insertRawTuple(databaseId, tableId, payload, publisher);
                    return;
                } catch (RemoteUnavailableException e) {
                    if (attempt == 3) {
                        throw new AmqpRejectAndDontRequeueException("Ingestion backend unavailable after three attempts", e);
                    }
                    log.warn("Ingestion backend unavailable; retrying attempt {} of 3", attempt + 1);
                    try {
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new ImmediateRequeueAmqpException("Consumer stopped during ingestion retry", interrupted);
                    }
                } catch (TableNotFoundException | DataServiceException e) {
                    throw new AmqpRejectAndDontRequeueException("Ingestion was rejected permanently", e);
                }
            }
        });
        container.setConcurrentConsumers(minConcurrent);
        container.setMaxConcurrentConsumers(maxConcurrent);
        container.setDefaultRequeueRejected(false);
        container.setPrefetchCount(1);
        container.setReceiveTimeout(connectionTimeout);
        return container;
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
