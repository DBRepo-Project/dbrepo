package at.tuwien.auth;

import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Slf4j
@Component
public class UserPermissionEvaluator implements PermissionEvaluator {

    private final DatabaseService databaseService;

    @Autowired
    public UserPermissionEvaluator(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        log.trace("has permission auth {} target domain {} permission {}", auth, targetDomainObject, permission);
        if (auth == null) {
            log.error("Authentication principal is null");
            return false;
        }
        if (!(targetDomainObject instanceof Long)) {
            log.error("Domain is not of type Long");
            return false;
        }
        if (!(permission instanceof String)) {
            log.error("Permission is not of type String");
            return false;
        }
        log.trace("principal is {}", auth.getPrincipal());
        final UserDetailsDto principal;
        if (!(auth.getPrincipal() instanceof UserDetailsDto) || auth.getPrincipal() == null) {
            log.warn("Principal is null");
            principal = null;
        } else {
            principal = (UserDetailsDto) auth.getPrincipal();
        }
        final Long targetDomainId = (Long) targetDomainObject;
        final String permissionCode = (String) permission;
        switch (permissionCode) {
            case "QUERY_VIEW_ALL":
            case "QUERY_EXECUTE":
            case "QUERY_RE_EXECUTE":
            case "QUERY_EXPORT":
            case "QUERY_VIEW":
                final Database database;
                try {
                    database = databaseService.find(targetDomainId);
                } catch (DatabaseNotFoundException e) {
                    log.error("Failed to find database with id {}", targetDomainId);
                    return false;
                }
                /* view-only operations are allowed on public databases */
                if (database.getIsPublic() && permissionCode.equals("QUERY_VIEW_ALL")) {
                    return true;
                }
                /* modification operations are limited to the creator */
                if (principal == null) {
                    return false;
                }
                if (principal.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
                    log.error("Current user has insufficient authorities");
                    log.debug("current user misses ROLE_RESEARCHER");
                    return false;
                }
                return database.getCreator().getUsername().equals(principal.getUsername());
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
