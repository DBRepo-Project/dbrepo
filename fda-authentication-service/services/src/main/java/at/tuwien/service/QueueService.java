package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
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
}
