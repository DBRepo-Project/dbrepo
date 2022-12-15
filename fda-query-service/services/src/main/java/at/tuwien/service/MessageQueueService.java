package at.tuwien.service;

import at.tuwien.exception.AmqpException;

public interface MessageQueueService {

    void createConsumer(String queueName, Long containerId, Long databaseId, Long tableId) throws AmqpException;
}
