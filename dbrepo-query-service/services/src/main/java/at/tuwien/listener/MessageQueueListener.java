package at.tuwien.listener;

import at.tuwien.exception.AmqpException;
import org.springframework.scheduling.annotation.Scheduled;

public interface MessageQueueListener {

    /**
     * Restores the consumers up to the configured limit.
     *
     * @throws AmqpException The consumer could not be created.
     */
    @Scheduled(fixedDelay = 5000)
    void updateConsumers() throws AmqpException;
}
