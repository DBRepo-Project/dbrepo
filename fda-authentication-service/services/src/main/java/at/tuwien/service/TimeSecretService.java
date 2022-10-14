package at.tuwien.service;

import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.User;
import at.tuwien.exception.SecretInvalidException;

public interface TimeSecretService {

    /**
     * Find token by random string.
     *
     * @param token The random string.
     * @return The token.
     * @throws SecretInvalidException The token was not found or has expired.
     */
    TimeSecret find(String token) throws SecretInvalidException;

    /**
     * Create a token with random string.
     *
     * @param user The user.
     * @return The token.
     */
    TimeSecret create(User user);

    /**
     * Invalidate a token for a given user.
     *
     * @param token The token.
     * @return The user, if successful.
     * @throws SecretInvalidException THe token was not found or has expired.
     */
    User invalidate(String token) throws SecretInvalidException;
}
