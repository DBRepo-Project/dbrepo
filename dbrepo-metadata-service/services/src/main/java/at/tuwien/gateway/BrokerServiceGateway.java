package at.tuwien.gateway;

import at.tuwien.api.amqp.*;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.exception.*;

public interface BrokerServiceGateway {

    /**
     * Create topic exchange permissions at the broker service.
     *
     * @param data The topic exchange permissions.
     */
    void grantExchangePermission(String username, GrantExchangePermissionsDto data) throws ServiceConnectionException, ServiceException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     */
    void grantTopicPermission(String username, ExchangeUpdatePermissionsDto data) throws ServiceConnectionException, ServiceException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     */
    void grantVirtualHostPermission(String username, GrantVirtualHostPermissionsDto data) throws ServiceConnectionException, ServiceException;

    /**
     * Finds queue information from the broker service by name.
     *
     * @param name The queue name.
     * @return The queue, if successful.
     */
    QueueDto findQueue(String name) throws ServiceConnectionException, ServiceException, QueueNotFoundException;

    /**
     * Finds exchange information from the broker service by name.
     *
     * @param name The exchange name.
     * @return The queue, if successful.
     */
    ExchangeDto findExchange(String name) throws ServiceException, ServiceConnectionException, ExchangeNotFoundException;
}
