package at.tuwien.service.impl;

import at.tuwien.amqp.RabbitMqConsumer;
import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.AmqpException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.QueryService;
import at.tuwien.service.TableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Log4j2
@Service
public class RabbitMqServiceImpl implements MessageQueueService {

    private Channel channel;
    private final AmqpConfig amqpConfig;
    private final ObjectMapper objectMapper;
    private final QueryService queryService;
    private final TableService tableService;
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public RabbitMqServiceImpl(Channel channel, AmqpConfig amqpConfig, ObjectMapper objectMapper,
                               QueryService queryService, TableService tableService,
                               BrokerServiceGateway brokerServiceGateway) {
        this.channel = channel;
        this.amqpConfig = amqpConfig;
        this.objectMapper = objectMapper;
        this.queryService = queryService;
        this.tableService = tableService;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public void createConsumer(String queueName, Long databaseId, Long tableId) throws AmqpException {
        try {
            if (!this.channel.isOpen()) {
                log.warn("Channel with id {} is closed", this.channel.getChannelNumber());
                final Connection tmp = this.amqpConfig.connectionFactory().newConnection();
                this.channel = tmp.createChannel();
                log.info("Opened channel with id {}", this.channel.getChannelNumber());
            }
            final String consumerTag = this.channel.basicConsume(queueName, true, new RabbitMqConsumer(databaseId, tableId, objectMapper, queryService));
            log.debug("declared consumer for queue name {} with tag {}", queueName, consumerTag);
        } catch (IOException e) {
            log.error("Failed to create consumer for table with id {}, reason: {}", tableId, e.getMessage());
            throw new AmqpException("Failed to create consumer", e);
        } catch (Exception e) {
            log.error("Failed unknown: {}", e.getMessage());
            /* ignore */
        }
    }

    @Override
    public void restore() throws AmqpException {
        final List<Table> tables = tableService.findAll();
        final List<ConsumerDto> consumers = brokerServiceGateway.findAllConsumers();
        for (Table table : tables) {
            final long consumerCount = consumers.stream().filter(c -> c.getQueue().getName().equals(table.getQueueName())).count();
            if (consumerCount >= amqpConfig.getAmqpConsumers()) {
                continue;
            }
            for (long i = consumerCount; i < amqpConfig.getAmqpConsumers(); i++) {
                createConsumer(table.getQueueName(), table.getDatabase().getId(), table.getId());
            }
        }
    }

}
