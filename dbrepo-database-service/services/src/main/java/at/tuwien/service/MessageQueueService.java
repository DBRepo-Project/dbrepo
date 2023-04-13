package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import javax.annotation.PostConstruct;
import java.security.Principal;

public interface MessageQueueService {

    /**
     * Initializes the exchanges on the Broker Service for each database in the metadata database.
     *
     * @throws AmqpException The exchange could not be created.
     */
    @PostConstruct
    void init() throws AmqpException;

    /**
     * Creates an exchange for a database.
     *
     * @param database  The database.
     * @param principal The user.
     * @throws AmqpException Could not create the exchange.
     */
    void createExchange(Database database, Principal principal) throws AmqpException;

    /**
     * Create user on the broker service
     *
     * @param user The new user.
     * @throws BrokerVirtualHostCreationException The user could not be created.
     */
    void createUser(User user) throws BrokerVirtualHostCreationException;

    /**
     * Updates the virtual host permissions in the Broker Service for a user with given principal.
     *
     * @param principal The user principal.
     * @throws BrokerVirtualHostCreationException The Broker Service refused the update of the permissions.
     * @throws BrokerVirtualHostGrantException    The Broker Service refused to grant the permissions.
     */
    void updatePermissions(Principal principal) throws BrokerVirtualHostCreationException, BrokerVirtualHostGrantException;

    /**
     * Deletes an exchange for a database.
     *
     * @param database The database.
     * @throws AmqpException Could not delete the exchange.
     */
    void deleteExchange(Database database) throws AmqpException;

}