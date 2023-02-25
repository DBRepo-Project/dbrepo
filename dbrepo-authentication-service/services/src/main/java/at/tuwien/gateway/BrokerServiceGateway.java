package at.tuwien.gateway;


import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.exception.BrokerUserCreationException;


public interface BrokerServiceGateway {

    /**
     * Creates a user at the Broker Service.
     *
     * @param username The username.
     * @param data     The user data.
     * @throws BrokerUserCreationException The broker did not create a user.
     */
    void createUser(String username, CreateUserDto data) throws BrokerUserCreationException;

    /**
     * Finds a user by username in the Broker Service.
     *
     * @param username The username.
     * @return The user, if successful.
     * @throws BrokerUserCreationException The user could not be found.
     */
    UserDetailsDto findUser(String username) throws BrokerUserCreationException;

    /**
     * Modifies host permissions at the Broker Service for a user with given username.
     *
     * @param username The username.
     * @param data     The user data.
     * @throws BrokerUserCreationException The broker did not modify the user.
     */
    void modifyHostPermissions(String username, GrantVirtualHostPermissionsDto data) throws BrokerUserCreationException;

    /**
     * Modify a user password at the Broker Service for a user with given username.
     *
     * @param username The username.
     * @param data     The user modification data.
     * @throws BrokerUserCreationException The broker did not modify a user.
     */
    void modifyUserPassword(String username, CreateUserDto data) throws BrokerUserCreationException;
}
