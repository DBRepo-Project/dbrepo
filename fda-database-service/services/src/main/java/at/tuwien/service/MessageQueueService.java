package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;

import java.io.IOException;
import java.security.Principal;

public interface MessageQueueService {

    /**
     * In case of server downtime this method restores all exchanges and bindings
     *
     * @throws IOException Exchange or queue was not declarable.
     */
    void init() throws IOException, AmqpException;

    /**
     * Creates an exchange for a database.
     *
     * @param database  The database.
     * @param principal The user.
     * @throws AmqpException Could not create the exchange.
     */
    void createExchange(Database database, Principal principal) throws AmqpException;

    /**
     * Grant permissions on virtual host for a database
     *
     * @param database  The database.
     * @param principal The user that gets permissions.
     * @throws BrokerVirtualHostCreationException The Broker Service failed to create or grant permissions.
     */
    void createVirtualHost(Database database, Principal principal) throws BrokerVirtualHostCreationException;

    /**
     * Deletes an exchange for a database.
     *
     * @param database The database.
     * @throws AmqpException Could not delete the exchange.
     */
    void deleteExchange(Database database) throws AmqpException;

}