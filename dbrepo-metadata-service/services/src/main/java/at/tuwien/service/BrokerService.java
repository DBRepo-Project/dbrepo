package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

public interface BrokerService {

    /**
     * Updates the virtual host permissions in the Broker Service for a user with given principal.
     *
     * @param user The user.
     */
    void setVirtualHostPermissions(User user) throws ServiceException, ServiceConnectionException;

    /**
     * Sets topic exchange permissions for a user.
     *
     * @param user The user.
     */
    void setTopicExchangePermissions(User user) throws ServiceException, ServiceConnectionException;
}
