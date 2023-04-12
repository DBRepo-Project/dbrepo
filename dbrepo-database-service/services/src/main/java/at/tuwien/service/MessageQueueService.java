package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import javax.annotation.PostConstruct;
import java.security.Principal;

public interface MessageQueueService {

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
     * Updates the virtual host permissions in the broker service.
     *
     * @param principal Te user.
     * @throws BrokerVirtualHostCreationException Could not update the permissions.
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