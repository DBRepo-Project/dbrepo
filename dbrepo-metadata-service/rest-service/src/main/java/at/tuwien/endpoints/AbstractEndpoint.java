package at.tuwien.endpoints;

import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import java.security.Principal;
import java.util.UUID;

public abstract class AbstractEndpoint {

    public boolean hasRole(Principal principal, String role) {
        if (principal == null || role == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean isSystem(Principal principal) {
        if (principal == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("system"));
    }

    public UUID getId(Principal principal) {
        if (principal == null) {
            return null;
        }
        final Authentication authentication = (Authentication) principal;
        if (authentication.getPrincipal() instanceof UserDetailsDto user) {
            if (user.getId() == null) {
                throw new IllegalArgumentException("Principal has no id");
            }
            return UUID.fromString(user.getId());
        }
        throw new IllegalArgumentException("Unknown principal instance: " + authentication.getPrincipal().getClass());
    }

}
