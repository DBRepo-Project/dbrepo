package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostModificationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;

public interface MessageQueueService {

    /**
     * Create user on the broker service with given username and password.
     *
     * @param username The username.
     * @param password The password.
     * @throws BrokerRemoteException                  The user could not be created.
     * @throws BrokerVirtualHostModificationException The Broker Service did not respond within the 3s timeout.
     */
    void createUser(String username, String password) throws BrokerRemoteException, BrokerVirtualHostModificationException;

    /**
     * Delete a user on the broker service with given username.
     *
     * @param username The username.
     * @throws BrokerRemoteException                  The user could not be deleted.
     * @throws BrokerVirtualHostModificationException The Broker Service did not respond within the 3s timeout.
     */
    void deleteUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException;

    /**
     * Updates the virtual host permissions in the Broker Service for a user with given principal.
     *
     * @param username The username.
     * @throws BrokerVirtualHostGrantException The Broker Service refused to grant the permissions.
     */
    void setVirtualHostPermissions(String username) throws BrokerVirtualHostGrantException, BrokerRemoteException;

    void setTopicExchangePermissions(User user) throws BrokerVirtualHostGrantException,
            BrokerRemoteException;
}
