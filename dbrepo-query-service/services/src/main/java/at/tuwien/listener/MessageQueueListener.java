package at.tuwien.listener;

import at.tuwien.exception.AmqpException;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

public interface MessageQueueListener {

    /**
     * Restores the consumers up to the configured limit. Initial delay of 5 minutes needed for the gateway service to start.
     *
     * @throws AmqpException The consumer could not be created.
     */
    @Scheduled(fixedDelay = 5, initialDelay = 300, timeUnit = TimeUnit.SECONDS)
    void updateConsumers() throws AmqpException;
}
