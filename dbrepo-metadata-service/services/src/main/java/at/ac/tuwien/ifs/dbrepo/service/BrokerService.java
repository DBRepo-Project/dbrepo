package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

public interface BrokerService {

    /**
     * Updates the virtual host permissions in the Broker Service for a user with given principal.
     *
     * @param user The user.
     * @throws BrokerServiceException           The broker service responded with an unexpected response code.
     * @throws BrokerServiceConnectionException The connection to the broker service could not be established.
     */
    void setVirtualHostPermissions(User user) throws BrokerServiceException, BrokerServiceConnectionException;

    /**
     * Sets topic exchange permissions for a user.
     *
     * @param user The user.
     * @throws BrokerServiceException           The broker service responded with an unexpected response code.
     * @throws BrokerServiceConnectionException The connection to the broker service could not be established.
     */
    void setTopicExchangePermissions(User user) throws BrokerServiceException, BrokerServiceConnectionException;
}
