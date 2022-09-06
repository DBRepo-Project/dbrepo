package at.tuwien.endpoints;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.List;

@Slf4j
public abstract class AbstractEndpoint {

    private final DatabaseService databaseService;
    private final ContainerService containerService;

    @Autowired
    protected AbstractEndpoint(DatabaseService databaseService, ContainerService containerService) {
        this.databaseService = databaseService;
        this.containerService = containerService;
    }

    protected Boolean hasDatabasePermission(Long containerId, Long databaseId, String permissionCode,
                                            Principal principal) {
        final Database database;
        try {
            database = databaseService.findById(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.debug("failed to find database with id {}", databaseId);
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (database.getIsPublic() && List.of().contains(permissionCode)) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
        if (principal == null) {
            log.debug("failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        /* modification operations are limited to the creator */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user {} is creator {}", permissionCode, principal.getName(),
                    database.getCreator().getUsername());
            return true;
        }
        log.debug("failed to grant permission {} because database is not owner by the current user", permissionCode);
        return false;
    }

    protected Boolean hasContainerPermission(Long containerId, String permissionCode, Principal principal) {
        final Container container;
        try {
            container = containerService.find(containerId);
        } catch (ContainerNotFoundException e) {
            log.debug("failed to find container with id {}", containerId);
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (List.of().contains(permissionCode)) {
            log.debug("grant permission {} because it does not require authentication", permissionCode);
            return true;
        }
        if (principal == null) {
            log.debug("failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        /* modification operations are limited to the creator */
        if (container.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user {} is creator {}", permissionCode, principal.getName(),
                    container.getCreator().getUsername());
            return true;
        }
        log.debug("failed to grant permission {} because container is not owner by the current user", permissionCode);
        return false;
    }

}
