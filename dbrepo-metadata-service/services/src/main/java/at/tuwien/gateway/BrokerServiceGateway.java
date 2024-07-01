package at.tuwien.gateway;

import at.tuwien.api.amqp.*;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.exception.*;

public interface BrokerServiceGateway {

    /**
     * Create topic exchange permissions at the broker service.
     *
     * @param data The topic exchange permissions.
     * @throws ServiceConnectionException
     * @throws ServiceException
     */
    void grantExchangePermission(String username, GrantExchangePermissionsDto data) throws ServiceConnectionException,
            ServiceException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws ServiceConnectionException
     * @throws ServiceException
     */
    void grantTopicPermission(String username, ExchangeUpdatePermissionsDto data) throws ServiceConnectionException,
            ServiceException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws ServiceConnectionException
     * @throws ServiceException
     */
    void grantVirtualHostPermission(String username, GrantVirtualHostPermissionsDto data)
            throws ServiceConnectionException, ServiceException;
}
