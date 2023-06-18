package at.tuwien.validation;

import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Log4j2
@Component
public class EndpointValidator {

    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final TableService tableService;

    @Autowired
    public EndpointValidator(AccessService accessService, DatabaseService databaseService, TableService tableService) {
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.tableService = tableService;
    }

    public void validateOnlyPrivateAccess(Long databaseId, Principal principal, boolean writeAccessOnly) throws NotAllowedException, DatabaseNotFoundException {
        final Database database = databaseService.find(databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        validateOnlyAccess(databaseId, principal, writeAccessOnly);
    }

    public void validateOnlyPrivateAccess(Long databaseId, Principal principal) throws NotAllowedException, DatabaseNotFoundException {
        validateOnlyPrivateAccess(databaseId, principal, false);
    }

    public void validateOnlyAccess(Long databaseId, Principal principal, boolean writeAccessOnly) throws NotAllowedException, DatabaseNotFoundException {
        if (principal == null) {
            log.error("Access not allowed: database with id {} is not public and no authorization provided", databaseId);
            throw new NotAllowedException("Access not allowed: database with id " + databaseId + " is not public and no authorization provided");
        }
        databaseService.find(databaseId);
        log.trace("principal: {}", principal.getName());
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        log.trace("found access: {}", access);
        if (writeAccessOnly && !(access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.error("Access not allowed: no write access");
            throw new NotAllowedException("Access not allowed: no write access");
        }
    }

    public void validateOnlyOwnerOrWriteAll(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, NotAllowedException, TableNotFoundException, ContainerNotFoundException {
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        final Table table = tableService.findById(databaseId, tableId);
        log.trace("principal: {}", principal.getName());
        log.trace("table creator: {}", table.getCreator().getUsername());
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.READ)) {
            log.error("Access not allowed: insufficient access (only read-access)");
            throw new NotAllowedException("Access not allowed: insufficient access (only read-access)");
        }
        if (table.getCreator().equalsPrincipal(principal) && (access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.trace("grant access: table creator with write access");
            return;
        }
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.trace("grant access: write-all access");
            return;
        }
        log.error("Access not allowed: insufficient access (neither creator {} nor write-all access)", table.getCreator().getUsername());
        throw new NotAllowedException("Access not allowed: insufficient access (neither creator nor write-all access)");
    }

    public void validateOnlyPrivateHasRole(Long databaseId, Principal principal, String role)
            throws DatabaseNotFoundException, NotAllowedException {
        final Database database = databaseService.find(databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        log.trace("database with id {} is private", databaseId);
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal: {}", principal.getName());
        if (!User.hasRole(principal, role)) {
            log.error("Access not allowed: role {} missing", role);
            throw new NotAllowedException("Access not allowed: role " + role + " missing");
        }
        log.trace("principal has role '{}': access granted", role);
    }
}
