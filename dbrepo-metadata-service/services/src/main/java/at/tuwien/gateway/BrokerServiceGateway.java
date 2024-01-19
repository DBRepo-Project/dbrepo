package at.tuwien.gateway;

import at.tuwien.api.amqp.*;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.exception.*;

import java.util.List;

public interface BrokerServiceGateway {

    /**
     * Create topic exchange permissions at the broker service.
     *
     * @param data The topic exchange permissions.
     * @throws BrokerVirtualHostGrantException The virtual host could not be created.
     * @throws BrokerRemoteException           The Broker Service did not respond within the 3s timeout.
     */
    void grantTopicPermission(String username, GrantExchangePermissionsDto data) throws BrokerRemoteException,
            BrokerVirtualHostGrantException;

    /**
     * Finds all active consumers on the virtual host "dbrepo".
     *
     * @return The list of active consumers.
     * @throws BrokerRemoteException The Broker Service did not respond within the 3s timeout.
     */
    List<ConsumerDto> findAllConsumers() throws BrokerRemoteException;

    /**
     * Create virtual host at the queue service.
     *
     * @param data The virtual host.
     * @throws BrokerVirtualHostModificationException The virtual host could not be created.
     * @throws BrokerRemoteException                  The Broker Service did not respond within the 3s timeout.
     */
    void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostModificationException, BrokerRemoteException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws BrokerVirtualHostGrantException The permissions could not be granted.
     * @throws BrokerRemoteException           The Broker Service did not respond within the 3s timeout.
     */
    void grantPermission(String username, ExchangeUpdatePermissionsDto data) throws BrokerVirtualHostGrantException, BrokerRemoteException;

    /**
     * Create user on the broker service with given username and password.
     *
     * @param username The username.
     * @param password The password.
     * @throws BrokerRemoteException                  The Broker Service did not respond within the 3s timeout.
     * @throws BrokerVirtualHostModificationException The user could not be created.
     */
    void createUser(String username, String password) throws BrokerRemoteException, BrokerVirtualHostModificationException;

    /**
     * Deletes a user on the broker service with given username.
     *
     * @param username The username.
     * @throws BrokerRemoteException                  The Broker Service did not respond within the 3s timeout.
     * @throws BrokerVirtualHostModificationException The user could not be deleted.
     */
    void deleteUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws BrokerRemoteException           The Broker Service did not respond within the 3s timeout.
     * @throws BrokerVirtualHostGrantException The permissions could not be granted.
     */
    void grantPermission(String username, GrantVirtualHostPermissionsDto data) throws BrokerRemoteException, BrokerVirtualHostGrantException;

    /**
     * Finds queue information from the broker service by name.
     *
     * @param name The queue name.
     * @return The queue, if successful.
     * @throws BrokerRemoteException  The Broker Service did not respond within the 3s timeout.
     * @throws QueueNotFoundException The queue could not be found.
     */
    QueueDto findQueue(String name) throws BrokerRemoteException, QueueNotFoundException;

    /**
     * Finds exchange information from the broker service by name.
     *
     * @param name The exchange name.
     * @return The queue, if successful.
     * @throws BrokerRemoteException     The Broker Service did not respond within the 3s timeout.
     * @throws ExchangeNotFoundException The exchange could not be found.
     */
    ExchangeDto findExchange(String name) throws BrokerRemoteException, ExchangeNotFoundException;
}
