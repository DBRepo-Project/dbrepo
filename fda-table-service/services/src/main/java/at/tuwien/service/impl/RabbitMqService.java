package at.tuwien.service.impl;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.gateway.AuthenticationServiceGateway;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.service.MessageQueueService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class RabbitMqService implements MessageQueueService {

    private final Channel channel;
    private final ObjectMapper objectMapper;
    private final QueryServiceGateway queryServiceGateway;
    private final ScheduledExecutorService executorService;
    private final AuthenticationServiceGateway authenticationServiceGateway;

    private JwtResponseDto response;

    @Autowired
    public RabbitMqService(Channel channel, ObjectMapper objectMapper, QueryServiceGateway queryServiceGateway,
                           AuthenticationServiceGateway authenticationServiceGateway) {
        this.channel = channel;
        this.objectMapper = objectMapper;
        this.queryServiceGateway = queryServiceGateway;
        this.executorService = Executors.newScheduledThreadPool(1);
        this.authenticationServiceGateway = authenticationServiceGateway;
    }

    @Transactional(readOnly = true)
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        final Runnable tokenRunnable = this::obtainToken;
        this.executorService.schedule(tokenRunnable, 1L, TimeUnit.HOURS);
        this.obtainToken();
    }

    /**
     * Obtains a new JWT token from the authentication service (via the gateway) as the "system" user
     */
    protected void obtainToken() {
        response = authenticationServiceGateway.obtain();
        log.info("Fetched new token from authentication service for username {}", response.getUsername());
        log.debug("fetched new token from authentication service {}", response);
        queryServiceGateway.setToken(response.getToken());
    }

    @PreDestroy
    @Transactional(readOnly = true)
    public void teardown() {
        this.executorService.shutdown();
    }

    @Override
    @Transactional(readOnly = true)
    public void create(Table table) throws AmqpException {
        try {
            channel.queueDeclare(table.getTopic(), true, false, false, null);
            channel.queueBind(table.getTopic(), table.getDatabase().getExchange(), table.getTopic());
        } catch (IOException e) {
            log.error("Failed to create queue and bind for table with id {}", table.getId());
            log.debug("Failed to create queue and bind for table {}", table);
            throw new AmqpException("Failed to create", e);
        }
        log.info("Created queue for table with id {}", table.getId());
        log.debug("created queue for table {}", table);
        try {
            channel.basicConsume(table.getTopic(), true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    final TypeReference<HashMap<String, Object>> payloadReference = new TypeReference<>() {
                    };
                    try {
                        final TableCsvDto data = TableCsvDto.builder()
                                .data(objectMapper.readValue(body, payloadReference))
                                .build();
                        log.debug("received tuple data {}", data);
                        queryServiceGateway.publish(table.getDatabase().getContainer().getId(),
                                table.getDatabase().getId(), table.getId(), data);
                    } catch (IOException e) {
                        log.error("Failed to parse for table with id {}", table.getId());
                        log.debug("Failed to parse for table {} because {}", table, e.getMessage());
                        /* ignore */
                    } catch (HttpClientErrorException.Unauthorized e) {
                        log.error("Failed to authenticate for table with id {}", table.getId());
                        log.debug("Failed to authenticate for table {} because {}", table.getId(), e.getMessage());
                        /* ignore */
                    } catch (HttpClientErrorException.BadRequest e) {
                        log.error("Failed to insert for table with id {}", table.getId());
                        log.debug("Failed to insert for table {} because {}", table.getId(), e.getMessage());
                        /* ignore */
                    }
                }
            });
        } catch (IOException e) {
            log.error("Failed to create consumer for table with id {}", table.getId());
            log.debug("Failed to create basic consumer for table {}", table);
            throw new AmqpException("Failed to create consumer", e);
        } catch (Exception e) {
            log.warn("Failed unknown: {}", e.getMessage());
            /* ignore */
        }
        log.info("Declared consumer for table topic {}", table.getTopic());
        log.debug("declared consumer {}", table);
    }

}
