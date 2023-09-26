package at.tuwien.listener.impl;

import at.tuwien.exception.AmqpException;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.listener.MessageQueueListener;
import at.tuwien.service.MessageQueueService;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class RabbitMqListenerImpl implements MessageQueueListener {

    private final MessageQueueService messageQueueService;

    public RabbitMqListenerImpl(MessageQueueService messageQueueService) {
        this.messageQueueService = messageQueueService;
    }

    @Override
    @Scheduled(fixedDelay = 5, initialDelay = 300, timeUnit = TimeUnit.SECONDS)
    @Transactional(readOnly = true)
    public void updateConsumers() throws AmqpException, BrokerRemoteException {
        messageQueueService.restore();
    }

}
