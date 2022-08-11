package at.tuwien.service;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;

import java.security.Principal;

public interface AuthenticationService {

    /**
     * Authenticates a user with given credentials
     *
     * @param data The credentials.
     * @return The token, if successful
     */
    JwtResponseDto authenticate(LoginRequestDto data) throws UserEmailNotVerifiedException, UserNotFoundException;

    /**
     * Renews a token for a given principal
     * TODO limit rate of renewal to 1/hour
     *
     * @param principal The principal.
     * @return The token, if successful
     */
    JwtResponseDto renew(Principal principal);
}
