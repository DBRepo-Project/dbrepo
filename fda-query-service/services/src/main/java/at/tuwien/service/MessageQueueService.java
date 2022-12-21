package at.tuwien.service;

import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.AmqpException;

public interface MessageQueueService {

    /**
     * Creates a consumer on the provided queue with name and container id and database id for table id.
     *
     * @param queueName The queue name.
     * @param table     The table.
     * @throws AmqpException The consumer could not be created.
     */
    void createConsumer(String queueName, Table table) throws AmqpException;
}
