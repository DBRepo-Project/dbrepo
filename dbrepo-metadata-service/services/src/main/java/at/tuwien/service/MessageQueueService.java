package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostModificationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;

public interface MessageQueueService {

    /**
     * Create user on the broker service with given username.
     *
     * @param username The username.
     * @throws BrokerRemoteException                  The user could not be created.
     * @throws BrokerVirtualHostModificationException The Broker Service did not respond within the 3s timeout.
     */
    void createUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException;

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
     * @param user The user.
     * @throws BrokerVirtualHostGrantException The Broker Service refused to grant the permissions.
     */
    void updatePermissions(User user) throws BrokerVirtualHostGrantException, BrokerRemoteException;

}
