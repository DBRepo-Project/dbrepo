package at.tuwien.validation;

import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Log4j2
@Component
public class EndpointValidator {

    private final AccessService accessService;
    private final DatabaseService databaseService;

    @Autowired
    public EndpointValidator(AccessService accessService, DatabaseService databaseService) {
        this.accessService = accessService;
        this.databaseService = databaseService;
    }

    public void validateOnlyAccess(Long databaseId, Principal principal) throws NotAllowedException {
        validateOnlyAccess(databaseId, principal, false);
    }

    public void validateOnlyPrivateAccess(Long containerId, Long databaseId, Principal principal, boolean writeAccessOnly) throws NotAllowedException, DatabaseNotFoundException {
        final Database database = databaseService.find(containerId, databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        validateOnlyAccess(databaseId, principal, writeAccessOnly);
    }

    public void validateOnlyPrivateAccess(Long containerId, Long databaseId, Principal principal) throws NotAllowedException, DatabaseNotFoundException {
        validateOnlyPrivateAccess(containerId, databaseId, principal, false);
    }

    public void validateOnlyAccess(Long databaseId, Principal principal, boolean writeAccessOnly) throws NotAllowedException {
        log.trace("database with id {} is private", databaseId);
        if (principal == null) {
            log.error("Access not allowed: database with id {} is not public and no authorization provided", databaseId);
            throw new NotAllowedException("Access not allowed: database with id " + databaseId + " is not public and no authorization provided");
        }
        log.trace("principal is {}", principal);
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        log.trace("found access {}", access);
        if (writeAccessOnly && !(access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.error("Access not allowed: no write access");
            throw new NotAllowedException("Access not allowed: no write access");
        }
    }

    public void validateOnlyOwner(Long containerId, Long databaseId, Principal principal)
            throws DatabaseNotFoundException, NotAllowedException {
        final Database database = databaseService.find(containerId, databaseId);
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal is {}", principal);
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        log.trace("found access {}", access);
        if (!database.getOwner().equals(principal)) {
            log.error("Access not allowed: not the owner of this database with id {}", databaseId);
            throw new NotAllowedException("Access not allowed: not the owner of this database");
        }
    }

    public void validateOnlyPrivateHasRole(Long containerId, Long databaseId, Principal principal, String role)
            throws DatabaseNotFoundException, NotAllowedException {
        final Database database = databaseService.find(containerId, databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        log.trace("database with id {} is private", databaseId);
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal is {}", principal);
        if (!User.hasRole(principal, role)) {
            log.error("Access not allowed: role {} missing", role);
            throw new NotAllowedException("Access not allowed: role " + role + " missing");
        }
        log.trace("principal has role '{}': access granted", role);
    }
}
