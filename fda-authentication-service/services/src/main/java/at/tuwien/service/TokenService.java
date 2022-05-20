package at.tuwien.service;

import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.TokenInvalidException;

public interface TokenService {

    /**
     * Find token by random string.
     *
     * @param token The random string.
     * @return The token.
     * @throws TokenInvalidException The token was not found or has expired.
     */
    Token find(String token) throws TokenInvalidException;

    /**
     * Create a token with random string.
     *
     * @param user The user.
     * @return The token.
     */
    Token create(User user);

    /**
     * Invalidate a token
     *
     * @param token The token.
     * @throws TokenInvalidException THe token was not found or has expired.
     */
    void invalidate(String token) throws TokenInvalidException;
}
