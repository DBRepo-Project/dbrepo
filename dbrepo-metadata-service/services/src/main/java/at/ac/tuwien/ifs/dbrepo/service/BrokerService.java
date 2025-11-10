package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceException;

import java.util.List;

public interface BrokerService {

    /**
     * Updates the virtual host permissions in the Broker Service for a user with given principal.
     *
     * @param username The username.
     * @throws BrokerServiceException           The broker service responded with an unexpected response code.
     * @throws BrokerServiceConnectionException The connection to the broker service could not be established.
     */
    void setVirtualHostPermissions(String username) throws BrokerServiceException, BrokerServiceConnectionException;

    /**
     * Sets topic exchange permissions for a user.
     *
     * @param username The username.
     * @param accesses The list of accesses.
     * @throws BrokerServiceException           The broker service responded with an unexpected response code.
     * @throws BrokerServiceConnectionException The connection to the broker service could not be established.
     */
    void setTopicExchangePermissions(String username, List<DatabaseAccess> accesses) throws BrokerServiceException, BrokerServiceConnectionException;
}
