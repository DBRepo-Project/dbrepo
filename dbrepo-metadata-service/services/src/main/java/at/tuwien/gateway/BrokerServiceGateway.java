package at.tuwien.gateway;

import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;

import java.util.List;

public interface BrokerServiceGateway {

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
     * @throws BrokerVirtualHostCreationException The virtual host could not be created.
     * @throws BrokerRemoteException              The Broker Service did not respond within the 3s timeout.
     */
    void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostCreationException, BrokerRemoteException;

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
     * Create user on the broker service
     *
     * @param username The new username.
     * @throws BrokerRemoteException              The Broker Service did not respond within the 3s timeout.
     * @throws BrokerVirtualHostCreationException The user could not be created.
     */
    void createUser(String username) throws BrokerRemoteException, BrokerVirtualHostCreationException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws BrokerRemoteException           The Broker Service did not respond within the 3s timeout.
     * @throws BrokerVirtualHostGrantException The permissions could not be granted.
     */
    void grantPermission(String username, GrantVirtualHostPermissionsDto data) throws BrokerRemoteException, BrokerVirtualHostGrantException;
}
