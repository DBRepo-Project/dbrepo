package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerUserCreationException;
import org.springframework.stereotype.Service;


@Service
public interface QueueService {

    /**
     * Creates a user at the Broker Service
     *
     * @param data The user data.
     * @throws BrokerUserCreationException The broker did not create the user.
     */
    void createUser(SignupRequestDto data) throws BrokerUserCreationException;

    /**
     * Modify a user password at the Broker Service
     *
     * @param user The user data.
     * @param data The user password.
     * @throws BrokerUserCreationException The broker did not modify the user.
     */
    void modifyUserPassword(User user, UserPasswordDto data) throws BrokerUserCreationException;
}
