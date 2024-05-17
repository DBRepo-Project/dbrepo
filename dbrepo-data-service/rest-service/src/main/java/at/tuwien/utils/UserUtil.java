package at.tuwien.utils;

import at.tuwien.api.user.UserDetailsDto;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.UUID;

public class UserUtil {

    public static boolean hasRole(Principal principal, String role) {
        if (principal == null || role == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public static UUID getId(Principal principal) {
        if (principal == null) {
            return null;
        }
        final Authentication authentication = (Authentication) principal;
        final UserDetailsDto user = (UserDetailsDto) authentication.getPrincipal();
        if (user.getId() == null) {
            return null;
        }
        return UUID.fromString(user.getId());
    }

}
