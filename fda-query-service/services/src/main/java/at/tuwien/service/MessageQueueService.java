package at.tuwien.service;

import at.tuwien.exception.AmqpException;

public interface MessageQueueService {

    void createConsumer(String routingKey, Long containerId, Long databaseId, Long tableId) throws AmqpException;
}
