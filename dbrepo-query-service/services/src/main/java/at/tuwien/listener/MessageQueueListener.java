package at.tuwien.listener;

import at.tuwien.exception.AmqpException;
import org.springframework.scheduling.annotation.Scheduled;

public interface MessageQueueListener {

    @Scheduled(fixedDelay = 5000)
    void updateConsumers() throws AmqpException;
}
