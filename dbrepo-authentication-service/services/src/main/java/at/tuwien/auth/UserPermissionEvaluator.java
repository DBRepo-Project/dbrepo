package at.tuwien.auth;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Slf4j
@Component
public class UserPermissionEvaluator implements PermissionEvaluator {

    private final UserService userService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public UserPermissionEvaluator(UserService userService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (auth == null || !(targetDomainObject instanceof Long) || !(permission instanceof String)) {
            return false;
        }
        final UserDetails caller = userDetailsService.loadUserByUsername(auth.getName());
        final Long targetDomainId = (Long) targetDomainObject;
        final User domainObject;
        try {
            domainObject = userService.find(targetDomainId);
        } catch (UserNotFoundException e) {
            log.error("User with id {} was not found", targetDomainId);
            return false;
        }
        return caller.getUsername().equals(domainObject.getUsername());
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
