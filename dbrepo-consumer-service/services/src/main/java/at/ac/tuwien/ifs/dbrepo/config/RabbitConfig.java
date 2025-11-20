package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    @Value("${dbrepo.requeueRejected}")
    private Boolean requeueRejected;

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
        container.setMessageListener(message -> {
            final MessageProperties properties = message.getMessageProperties();
            final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
            };
            if (!properties.getReceivedRoutingKey().contains(".")) {
                log.error("Failed to map database and table names from routing key: {}", properties.getReceivedRoutingKey());
                return;
            }
            final String[] parts = properties.getReceivedRoutingKey().split("\\.");
            if (parts.length != 3) {
                log.error("Failed to map database and table names from routing key: is not 3-part");
                return;
            }
            try {
                final TupleDto payload = TupleDto.builder()
                        .data(objectMapper.readValue(message.getBody(), typeRef))
                        .build();
                log.trace("received message of {} bytes with keys: {}", message.getMessageProperties().getContentLength(), payload.getData().keySet());
                dataServiceGateway.insertRawTuple(UUID.fromString(parts[1]), UUID.fromString(parts[2]), payload);
            } catch (IOException e) {
                log.error("Failed to read object: {}", e.getMessage());
            } catch (TableNotFoundException | DataServiceException | RemoteUnavailableException e) {
                /* ignore, handled in the gateway */
            }
        });
        container.setConcurrentConsumers(minConcurrent);
        container.setMaxConcurrentConsumers(maxConcurrent);
        container.setDefaultRequeueRejected(requeueRejected);
        container.setReceiveTimeout(connectionTimeout);
        return container;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        return new RabbitTemplate(factory);
    }

}
