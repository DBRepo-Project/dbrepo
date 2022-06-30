package at.tuwien.gateway;


import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.BrokerUserCreationException;


public interface BrokerServiceGateway {

    /**
     * Creates a user at the Broker Service.
     *
     * @param data The user data.
     * @throws BrokerUserCreationException The broker did not create a user.
     */
    void createUser(CreateUserDto data) throws BrokerUserCreationException;

    /**
     * Modify a user password for a user at the Queue Service
     *
     * @param username The username.
     * @param data     The user modification data.
     * @throws BrokerUserCreationException The broker did not modify a user.
     */
    void modifyUserPassword(String username, UserModifyPasswordDto data) throws BrokerUserCreationException;
}
