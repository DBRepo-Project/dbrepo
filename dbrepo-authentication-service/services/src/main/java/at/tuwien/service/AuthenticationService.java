package at.tuwien.service;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.TokenRevokedException;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

public interface AuthenticationService {

    /**
     * Authenticates a user with given credentials.
     *
     * @param data The credentials.
     * @return The token, if successful.
     * @throws UserEmailNotVerifiedException The user email is not verified.
     * @throws UserNotFoundException         The user was not found by username.
     */
    JwtResponseDto authenticate(LoginRequestDto data) throws UserEmailNotVerifiedException, UserNotFoundException;

    /**
     * Verifies an authorization header.
     *
     * @param authorization The authorization header.
     * @throws TokenRevokedException The token is not valid anymore and has been revoked.
     */
    void verifyToken(String authorization) throws TokenRevokedException;

    /**
     * Renews a token for a given principal
     *
     * @param principal The principal.
     * @return The token, if successful.
     */
    JwtResponseDto renew(Principal principal);
}
