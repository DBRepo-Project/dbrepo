package at.tuwien.service.impl;

import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.MessageQueueService;
import com.rabbitmq.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Log4j2
@Service
public class RabbitMqService implements MessageQueueService {

    private final Channel channel;

    @Autowired
    public RabbitMqService(Channel channel) {
        this.channel = channel;
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

}
