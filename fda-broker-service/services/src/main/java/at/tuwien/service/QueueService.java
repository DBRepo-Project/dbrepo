package at.tuwien.service;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.exception.ProcessCompletionException;
import org.springframework.stereotype.Service;

@Service
public interface QueueService {

    /**
     * Creates a user in the RabbitMQ system
     *
     * @param data The user data.
     * @throws ProcessCompletionException The process failed to complete.
     */
    void createUser(CreateUserDto data) throws ProcessCompletionException;
}
