package at.tuwien.service.impl;

import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.service.MessageQueueService;
import com.rabbitmq.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Log4j2
@Service
public class RabbitMqService implements MessageQueueService {

    private final Channel channel;
    private final TableRepository tableRepository;

    @Autowired
    public RabbitMqService(Channel channel, TableRepository tableRepository) {
        this.channel = channel;
        this.tableRepository = tableRepository;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void init() throws AmqpException {
        final List<Table> tables = tableRepository.findAll();
        for (Table table : tables) {
            create(table);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void create(Table table) throws AmqpException {
        try {
            channel.queueDeclare(table.getQueueName(), true, false, false, null);
            channel.queueBind(table.getQueueName(), table.getDatabase().getExchangeName(), table.getRoutingKey());
        } catch (IOException e) {
            log.error("Failed to create queue and bind for table with id {}", table.getId());
            throw new AmqpException("Failed to create", e);
        }
        log.info("Created queue for table with id {}", table.getId());
    }

}
