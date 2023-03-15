package at.tuwien.gateway;

import org.springframework.security.core.userdetails.UserDetails;

import javax.servlet.ServletException;

public interface AuthenticationServiceGateway {

    /**
     * Validates a token
     *
     * @param token The token
     * @return User details on success
     * @throws ServletException The token failed to validate at the Authentication Service.
     */
    UserDetails validate(String token) throws ServletException;
}
