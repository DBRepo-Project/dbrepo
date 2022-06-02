package at.tuwien.gateway;


import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.exception.BrokerUserCreationException;

public interface BrokerServiceGateway {

    /**
     * Creates a user at the Broker Service.
     *
     * @param data The user data.
     * @throws BrokerUserCreationException The broker did not create a user.
     */
    void createUser(CreateUserDto data) throws BrokerUserCreationException;
}
