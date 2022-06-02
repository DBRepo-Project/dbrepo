package at.tuwien.gateway;

import at.tuwien.api.auth.JwtResponseDto;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationServiceGateway {

    /**
     * Validates a token
     *
     * @param token The token
     * @return User details on success
     */
    UserDetails validate(String token);

    /**
     * Obtain a new JWT token
     *
     * @return The token, if successful.
     */
    JwtResponseDto obtain();
}
