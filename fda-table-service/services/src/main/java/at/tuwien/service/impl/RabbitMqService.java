package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class RabbitMqService implements MessageQueueService {

    private final Channel channel;
    private final ObjectMapper objectMapper;
    private final QueryServiceGateway queryServiceGateway;

    @Autowired
    public RabbitMqService(Channel channel, ObjectMapper objectMapper, QueryServiceGateway queryServiceGateway) {
        this.channel = channel;
        this.objectMapper = objectMapper;
        this.queryServiceGateway = queryServiceGateway;
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
    }

    @Override
    @Transactional(readOnly = true)
    public void createConsumer(Long containerId, Long databaseId, Table table) throws AmqpException {
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
                        queryServiceGateway.publish(containerId, databaseId, table.getId(), data);
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
