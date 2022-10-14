package at.tuwien.gateway;


import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.exception.BrokerUserCreationException;


public interface BrokerServiceGateway {

    /**
     * Creates a user at the Broker Service.
     *
     * @param username The user name.
     * @param data     The user data.
     * @throws BrokerUserCreationException The broker did not create a user.
     */
    void createUser(String username, CreateUserDto data) throws BrokerUserCreationException;

    /**
     * Modified host permissions
     *
     * @param username The user name.
     * @param data     The user data.
     * @throws BrokerUserCreationException The broker did not modify the user.
     */
    void modifyHostPermissions(String username, GrantVirtualHostPermissionsDto data) throws BrokerUserCreationException;

    /**
     * Modify a user password for a user at the Queue Service
     *
     * @param username The user name.
     * @param data     The user modification data.
     * @throws BrokerUserCreationException The broker did not modify a user.
     */
    void modifyUserPassword(String username, CreateUserDto data) throws BrokerUserCreationException;
}
