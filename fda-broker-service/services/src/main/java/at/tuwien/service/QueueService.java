package at.tuwien.service;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.ProcessCompletionException;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public interface QueueService {

    /**
     * Creates a user in the RabbitMQ system
     *
     * @param data The user data.
     * @throws ProcessCompletionException The process failed to complete.
     */
    void createUser(CreateUserDto data) throws ProcessCompletionException;

    /**
     * Updates the user password for a user at the Queue Service.
     *
     * @param username  The username of the user.
     * @param data      The password.
     * @param principal The current user.
     * @throws ProcessCompletionException The process did not complete within the 3s timeout.
     */
    void modifyPassword(String username, UserModifyPasswordDto data, Principal principal)
            throws ProcessCompletionException;

    /**
     * Creates a virtual host
     *
     * @param data The virtual host data.
     * @throws ProcessCompletionException The process did not complete within the 3s timeout.
     */
    void createVirtualHost(CreateVirtualHostDto data) throws ProcessCompletionException;

    /**
     * Grants a user permissions to the virtual host
     *
     * @param username  The username of the user.
     * @param data      The password.
     * @param principal The current user.
     * @throws ProcessCompletionException The process did not complete within the 3s timeout.
     */
    void grantVirtualHost(String username, GrantVirtualHostPermissionsDto data, Principal principal)
            throws ProcessCompletionException;
}
