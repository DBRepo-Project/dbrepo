package at.tuwien.gateway;

import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;

import java.util.List;

public interface BrokerServiceGateway {

    List<ConsumerDto> findAllConsumers();

    /**
     * Create virtual host at the queue service.
     *
     * @param data The virtual host.
     * @throws BrokerVirtualHostCreationException The queue service did not respond within the 3s timeout.
     */
    void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostCreationException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws BrokerVirtualHostGrantException The queue service did not respond within the 3s timeout.
     */
    void grantPermission(String username, ExchangeUpdatePermissionsDto data)
            throws BrokerVirtualHostGrantException;

    /**
     * Create user on the broker service
     *
     * @param username The new username.
     * @throws BrokerVirtualHostCreationException The user could not be created.
     */
    void createUser(String username) throws BrokerVirtualHostCreationException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws BrokerVirtualHostGrantException The queue service did not respond within the 3s timeout.
     */
    void grantPermission(String username, GrantVirtualHostPermissionsDto data) throws BrokerVirtualHostGrantException;
}
