package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.AmqpException;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import jakarta.annotation.PostConstruct;

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
     * Creates a queue and consumer that re-routes the insert requests to the Query Service. Therefore and due to the
     * dependency this method cannot take any input during startup or seeding phase as it would introduce a deadlock.
     * Seeding is solely performed by the Query Service on startup.
     *
     * @param table The table.
     * @throws AmqpException The broker service did not allow to create a consumer.
     */
    void create(Table table) throws AmqpException;

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
     * @param user The user.
     * @throws BrokerVirtualHostGrantException The Broker Service refused to grant the permissions.
     */
    void updatePermissions(User user) throws BrokerVirtualHostGrantException;

    /**
     * Deletes an exchange for a database.
     *
     * @param database The database.
     * @throws AmqpException Could not delete the exchange.
     */
    void deleteExchange(Database database) throws AmqpException;
