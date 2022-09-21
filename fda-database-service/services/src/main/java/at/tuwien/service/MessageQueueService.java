package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;

import java.security.Principal;

public interface MessageQueueService {

    /**
     * Creates an exchange for a database.
     *
     * @param database  The database.
     * @param principal The user.
     * @throws AmqpException Could not create the exchange.
     */
    void createExchange(Database database, Principal principal) throws AmqpException, BrokerVirtualHostCreationException;

    /**
     * Deletes an exchange for a database.
     *
     * @param database The database.
     * @throws AmqpException Could not delete the exchange.
     */
    void deleteExchange(Database database) throws AmqpException;

}