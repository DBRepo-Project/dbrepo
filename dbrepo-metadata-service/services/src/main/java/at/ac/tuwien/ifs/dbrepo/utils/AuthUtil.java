package at.ac.tuwien.ifs.dbrepo.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.Principal;

public class AuthUtil {

    public static Boolean hasRole(Principal principal, String role) {
        if (principal == null || role == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public static Boolean isSystem(Principal principal) {
        return hasRole(principal, "system");
    }

    public static String getUsername(Principal principal) {
        if (principal == null) {
            return null;
        }
        final Authentication authentication = (Authentication) principal;
        if (authentication.getPrincipal() instanceof Jwt user) {
            /* oauth */
            if (user.getClaimAsString("preferred_username") == null) {
                throw new IllegalArgumentException("Principal has no username");
            }
            return user.getClaimAsString("preferred_username");
        } else if (authentication.getPrincipal() instanceof User user) {
            /* internal */
            if (user.getUsername() == null) {
                throw new IllegalArgumentException("Principal has no username");
            }
            return user.getUsername();
        } else if (authentication.getPrincipal() instanceof UserDetails user) {
            /* basic */
            if (user.getUsername() == null) {
                throw new IllegalArgumentException("Principal has no username");
            }
            return user.getUsername();
        }
        throw new IllegalArgumentException("Unknown principal instance: " + authentication.getPrincipal().getClass());
    }

}
