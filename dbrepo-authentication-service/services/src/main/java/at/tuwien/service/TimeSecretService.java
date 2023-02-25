package at.tuwien.service;

import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.User;
import at.tuwien.exception.SecretInvalidException;

public interface TimeSecretService {

    /**
     * Find time secret by its string.
     *
     * @param string The string.
     * @return The time secret.
     * @throws SecretInvalidException The time secret was not found or has expired.
     */
    TimeSecret find(String string) throws SecretInvalidException;

    /**
     * Create a time secret verification with random string.
     *
     * @param user The user.
     * @return The time secret.
     */
    TimeSecret create(User user);

    /**
     * Invalidate a time secret for a given user.
     *
     * @param string The string.
     * @return The user, if successful.
     * @throws SecretInvalidException The time secret was not found or has expired.
     */
    User invalidate(String string) throws SecretInvalidException;
}
