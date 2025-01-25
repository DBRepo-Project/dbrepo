package at.tuwien.endpoints;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.user.UserDetailsDto;
import org.springframework.security.core.Authentication;

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

    public void removeInternalData(ContainerDto container) {
        container.setPassword(null);
        container.setUsername(null);
        container.setHost(null);
        container.setPort(null);
    }

}
