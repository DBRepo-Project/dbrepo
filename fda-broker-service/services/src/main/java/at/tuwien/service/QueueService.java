package at.tuwien.service;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantComponentDto;
import at.tuwien.api.user.UserModifyPasswordDto;
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

    void modifyPassword(UserModifyPasswordDto data) throws ProcessCompletionException;

    /**
     * Creates a virtual host
     *
     * @param data The virtual host data
     * @throws ProcessCompletionException The process failed to complete.
     */
    void createVirtualHost(CreateVirtualHostDto data) throws ProcessCompletionException;

    /**
     * Grants a user permissions to the virtual host
     *
     * @param data The username and vitual host name
     * @throws ProcessCompletionException The process failed to complete.
     */
    void grantVirtualHost(GrantComponentDto data) throws ProcessCompletionException;
}
