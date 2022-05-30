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
        final UserDetailsDto principal = (UserDetailsDto) auth.getPrincipal();
        final Long targetDomainId = (Long) targetDomainObject;
        final String permissionCode = (String) permission;
        final Database database;
        try {
            database = databaseService.find(targetDomainId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", targetDomainId);
            return false;
        }
        switch (permissionCode) {
            case "IDENTIFIER_CREATE":
                if (principal.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
                    return false;
                }
                if (database.getIsPublic()) {
                    return true;
                }
                /* only creator can create identifiers for non-public databases */
                return database.getCreator().getId().equals(principal.getId());
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
