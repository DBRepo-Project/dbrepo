package at.tuwien.auth;

import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.mapper.DatabaseMapper;
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

    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;

    @Autowired
    public UserPermissionEvaluator(DatabaseMapper databaseMapper, DatabaseService databaseService) {
        this.databaseMapper = databaseMapper;
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
        final Long targetDomainId = (Long) targetDomainObject;
        final String permissionCode = (String) permission;
        final Database database;
        try {
            database = databaseService.findById(targetDomainId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", targetDomainId);
            return false;
        }
        switch (permissionCode) {
            case "DATABASE_VIEW":
                if (database.getIsPublic()) {
                    return true;
                }
                if (auth.getAuthorities().isEmpty()) {
                    return false;
                }
                final UserDetailsDto detailsDto = (UserDetailsDto) auth.getPrincipal();
                /* only the creator can view */
                return database.getCreator().getId().equals(detailsDto.getId());
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
