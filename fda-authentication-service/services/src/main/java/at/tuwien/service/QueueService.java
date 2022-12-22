package at.tuwien.service;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerUserCreationException;
import org.springframework.stereotype.Service;


@Service
public interface QueueService {

    UserDetailsDto findUser(String username) throws BrokerUserCreationException;

    /**
     * Creates a user at the Broker Service
     *
     * @param data The user data@throws BrokerUserCreationException The broker did not create the user.
     */
    void createUser(String username, SignupRequestDto data) throws BrokerUserCreationException;

    /**
     * Modify a user password at the Broker Service
     *
     * @param user The user data.
     * @param data The user password..
     * @throws BrokerUserCreationException The broker did not modify the user.
     */
    void modifyUserPassword(User user, CreateUserDto data) throws BrokerUserCreationException;
}
