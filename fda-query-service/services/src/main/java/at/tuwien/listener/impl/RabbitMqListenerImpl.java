package at.tuwien.listener.impl;

import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.AmqpException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.MessageQueueListener;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
public class RabbitMqListenerImpl implements MessageQueueListener {

    private final AmqpConfig amqpConfig;
    private final TableService tableService;
    private final MessageQueueService messageQueueService;
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public RabbitMqListenerImpl(AmqpConfig amqpConfig, TableService tableService,
                                MessageQueueService messageQueueService, BrokerServiceGateway brokerServiceGateway) {
        this.amqpConfig = amqpConfig;
        this.tableService = tableService;
        this.messageQueueService = messageQueueService;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional(readOnly = true)
    public void updateConsumers() throws AmqpException {
        final List<Table> tables = tableService.findAll();
        final List<ConsumerDto> consumers = brokerServiceGateway.findAllConsumers();
        for (Table table : tables) {
            final long consumerCount = consumers.stream().filter(c -> c.getQueue().getName().equals(table.getTopic())).count();
            if (consumerCount >= amqpConfig.getAmqpConsumers()) {
                log.trace("listener table with name {} already has {} consumers (max. {})", table.getName(), consumerCount, amqpConfig.getAmqpConsumers());
                continue;
            }
            log.debug("table with id {} has {} consumers, but needs {} in total", table.getId(), consumerCount, amqpConfig.getAmqpConsumers());
            messageQueueService.createConsumer(table.getTopic(), table.getDatabase().getContainer().getId(),
                    table.getDatabase().getId(), table.getId());
        }
    }

}
