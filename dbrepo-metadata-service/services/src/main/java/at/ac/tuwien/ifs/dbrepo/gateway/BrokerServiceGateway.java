package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.amqp.*;
import at.ac.tuwien.ifs.dbrepo.core.api.user.ExchangeUpdatePermissionsDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

public interface BrokerServiceGateway {

    /**
     * Create topic exchange permissions at the broker service.
     *
     * @param data The topic exchange permissions.
     * @throws BrokerServiceConnectionException
     * @throws BrokerServiceException
     */
    void grantExchangePermission(String username, GrantExchangePermissionsDto data)
            throws BrokerServiceConnectionException, BrokerServiceException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws BrokerServiceConnectionException
     * @throws BrokerServiceException
     */
    void grantTopicPermission(String username, ExchangeUpdatePermissionsDto data)
            throws BrokerServiceConnectionException, BrokerServiceException;

    /**
     * Grants a user permission at a virtual host in the queue service.
     *
     * @param username The username of the user.
     * @param data     The grant data.
     * @throws BrokerServiceConnectionException
     * @throws BrokerServiceException
     */
    void grantVirtualHostPermission(String username, GrantVirtualHostPermissionsDto data)
            throws BrokerServiceConnectionException, BrokerServiceException;
}
