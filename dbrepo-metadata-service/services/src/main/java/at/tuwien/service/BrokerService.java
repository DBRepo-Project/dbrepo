package at.tuwien.service;

import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.amqp.QueueDto;
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

    /**
     * Finds a queue with a given name.
     *
     * @param name The queue name.
     * @return The queue.
     */
    QueueDto findQueue(String name) throws ServiceException, ServiceConnectionException, QueueNotFoundException;

    /**
     * Finds an exchange with given name.
     *
     * @param name The name.
     * @return The exchange.
     */
    ExchangeDto findExchange(String name) throws ServiceException, ServiceConnectionException, ExchangeNotFoundException;
}
