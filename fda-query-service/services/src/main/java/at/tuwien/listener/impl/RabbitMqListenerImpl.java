package at.tuwien.listener.impl;

import at.tuwien.api.amqp.ConsumerDto;
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

    private final TableService tableService;
    private final MessageQueueService messageQueueService;
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public RabbitMqListenerImpl(TableService tableService, MessageQueueService messageQueueService,
                                BrokerServiceGateway brokerServiceGateway) {
        this.tableService = tableService;
        this.messageQueueService = messageQueueService;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional(readOnly = true)
    public void updateConsumers() throws AmqpException {
        final List<Table> tables = tableService.findAll();
        final List<ConsumerDto> consumers = brokerServiceGateway.findAllConsumers();
        for (Table table : tables) {
            if (consumers.stream().anyMatch(c -> c.getQueue().getName().equals(table.getTopic()))) {
                log.trace("table {} already has a consumer, skip.", table);
                continue;
            }
            messageQueueService.createConsumer(table.getTopic(), table.getDatabase().getContainer().getId(),
                    table.getDatabase().getId(), table.getId());
        }
    }

}
