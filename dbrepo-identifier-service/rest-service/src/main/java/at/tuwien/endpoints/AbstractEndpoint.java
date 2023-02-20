package at.tuwien.endpoints;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.List;

@Slf4j
public abstract class AbstractEndpoint {

    private final UserService userService;
    private final DatabaseService databaseService;

    @Autowired
    protected AbstractEndpoint(UserService userService, DatabaseService databaseService) {
        this.userService = userService;
        this.databaseService = databaseService;
    }

    protected Boolean hasDatabasePermission(Long containerId, Long databaseId, String permissionCode,
                                            Principal principal) throws UserNotFoundException {
        log.trace("validate has database permission, containerId={}, databaseId={}, permissionCode={}, principal={}",
                containerId, databaseId, permissionCode, principal);
        final Database database;
        try {
            database = databaseService.find(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with container id {} and database id {}", containerId, databaseId);
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (principal == null) {
            log.error("Failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        final User user = userService.findByUsername(principal.getName());
        /* data steward */
        if (List.of("CREATE_IDENTIFIER").contains(permissionCode) && user.getRoles().stream().anyMatch(r -> r.name().equals("ROLE_DATA_STEWARD"))) {
            log.debug("grant permission {} because of role data steward", permissionCode);
            return true;
        }
        /* modification operations are limited to the creator */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.trace("grant permission {} because user {} is creator {}", permissionCode, principal.getName(),
                    database.getCreator().getUsername());
            return true;
        }
        log.error("Failed to grant permission {} because database owner with id {} is not the current user with id {}", permissionCode, database.getCreator().getId(), user.getId());
        return false;
    }

}
