package at.tuwien.service.impl;

import at.tuwien.amqp.RabbitMqConsumer;
import at.tuwien.config.AmqpConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Log4j2
@Service
public class RabbitMqServiceImpl implements MessageQueueService {

    private Channel channel;
    private final AmqpConfig amqpConfig;
    private final ObjectMapper objectMapper;
    private final QueryService queryService;

    @Autowired
    public RabbitMqServiceImpl(Channel channel, AmqpConfig amqpConfig, ObjectMapper objectMapper,
                               QueryService queryService) {
        this.channel = channel;
        this.amqpConfig = amqpConfig;
        this.objectMapper = objectMapper;
        this.queryService = queryService;
    }

    @Override
    public void createConsumer(String queueName, Long containerId, Long databaseId, Long tableId) throws AmqpException {
        try {
            if (!this.channel.isOpen()) {
                log.warn("Channel with id {} is closed", this.channel.getChannelNumber());
                final Connection tmp = this.amqpConfig.connectionFactory().newConnection();
                this.channel = tmp.createChannel();
                log.info("Opened channel with id {}", this.channel.getChannelNumber());
            }
            final String consumerTag = this.channel.basicConsume(queueName, true, new RabbitMqConsumer(containerId,
                    databaseId, tableId, objectMapper, queryService));
            log.debug("declared consumer for queue name {} with tag {}", queueName, consumerTag);
        } catch (IOException e) {
            log.error("Failed to create consumer for table with id {}, reason: {}", tableId, e.getMessage());
            throw new AmqpException("Failed to create consumer", e);
        } catch (Exception e) {
            log.error("Failed unknown: {}", e.getMessage());
            /* ignore */
        }
    }

}
