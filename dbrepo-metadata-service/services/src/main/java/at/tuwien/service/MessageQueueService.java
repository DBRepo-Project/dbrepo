package at.tuwien.service;

import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.amqp.QueueDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

public interface MessageQueueService {

    /**
     * Create user on the broker service with given username and password.
     *
     * @param username The username.
     * @param password The password.
     * @throws BrokerRemoteException                  The broker service did not answer.
     * @throws BrokerVirtualHostModificationException The Broker Service did not respond within the 3s timeout.
     */
    void createUser(String username, String password) throws BrokerRemoteException, BrokerVirtualHostModificationException;

    /**
     * Delete a user on the broker service with given username.
     *
     * @param username The username.
     * @throws BrokerRemoteException                  The broker service did not answer.
     * @throws BrokerVirtualHostModificationException The Broker Service did not respond within the 3s timeout.
     */
    void deleteUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException;

    /**
     * Updates the virtual host permissions in the Broker Service for a user with given principal.
     *
     * @param username The username.
     * @throws BrokerVirtualHostGrantException The Broker Service refused to grant the permissions.
     * @throws BrokerRemoteException           The broker service did not answer.
     */
    void setVirtualHostPermissions(String username) throws BrokerVirtualHostGrantException, BrokerRemoteException;

    /**
     * Sets topic exchange permissions for a user.
     *
     * @param user The user.
     * @throws BrokerVirtualHostGrantException The Broker Service refused to grant the permissions.
     * @throws BrokerRemoteException           The broker service did not answer.
     */
    void setTopicExchangePermissions(User user) throws BrokerVirtualHostGrantException,
            BrokerRemoteException;

    /**
     * Finds a queue with a given name.
     *
     * @param name The queue name.
     * @return The queue.
     * @throws QueueNotFoundException The queue could not be found in the broker service.
     * @throws BrokerRemoteException  The broker service did not answer.
     */
    QueueDto findQueue(String name) throws QueueNotFoundException, BrokerRemoteException;

    /**
     * Finds an exchange with given name.
     *
     * @param name The name.
     * @return The exchange.
     * @throws ExchangeNotFoundException The exchange could not be found in the broker service.
     * @throws BrokerRemoteException     The broker service did not answer.
     */
    ExchangeDto findExchange(String name) throws ExchangeNotFoundException, BrokerRemoteException;
}
