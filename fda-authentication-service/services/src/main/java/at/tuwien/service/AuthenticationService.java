package at.tuwien.service;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

public interface AuthenticationService {

    /**
     * Authenticates a user with given credentials
     *
     * @param data The credentials.
     * @return The token, if successful
     * @throws UserEmailNotVerifiedException The user email is not verified.
     * @throws UserNotFoundException         The user was not found by username.
     */
    JwtResponseDto authenticate(LoginRequestDto data) throws UserEmailNotVerifiedException, UserNotFoundException;

    /**
     * Authenticate a user with username and a reset token as credentials.
     *
     * @param data The credentials.
     * @return The token if successful.
     * @throws UserEmailNotVerifiedException The user email is not verified.
     * @throws UserNotFoundException         The user was not found by username.
     */
    JwtResponseDto authenticate(UserModifyPasswordDto data) throws UserEmailNotVerifiedException,
            UserNotFoundException;

    /**
     * Renews a token for a given principal
     * TODO limit rate of renewal to 1/hour
     *
     * @param principal The principal.
     * @return The token, if successful
     */
    JwtResponseDto renew(Principal principal);
}
