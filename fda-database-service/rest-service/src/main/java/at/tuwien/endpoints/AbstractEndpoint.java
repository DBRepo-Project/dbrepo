package at.tuwien.endpoints;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

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
        log.trace("validate has database permission, containerId={}, databaseId={}, permissionCode={}, principal={}",
                containerId, databaseId, permissionCode, principal);
        final Database database;
        try {
            database = databaseService.findById(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database");
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (database.getIsPublic() && List.of().contains(permissionCode)) {
            log.trace("grant permission {} because database is public", permissionCode);
            return true;
        }
        if (principal == null) {
            log.error("Failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        /* view-only operations are allowed on all databases */
        if (List.of("CHECK_ACCESS").contains(permissionCode)) {
            log.debug("grant permission {} because of public/private database", permissionCode);
            return true;
        }
        /* modification operations are limited to the creator */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.trace("grant permission {} because user {} is creator {}", permissionCode, principal.getName(),
                    database.getCreator().getUsername());
            return true;
        }
        log.error("Failed to grant permission {} because database is not owner by the current user", permissionCode);
        return false;
    }

    protected Boolean hasContainerPermission(Long containerId, String permissionCode, Principal principal) {
        log.trace("validate has container permission, containerId={}, permissionCode={}, principal={}",
                containerId, permissionCode, principal);
        final Container container;
        try {
            container = containerService.find(containerId);
        } catch (ContainerNotFoundException e) {
            log.error("Failed to find container");
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (List.of().contains(permissionCode)) {
            log.debug("grant permission {} because it does not require authentication", permissionCode);
            return true;
        }
        if (principal == null) {
            log.error("Failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        /* modification operations are limited to the creator */
        if (container.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user {} is creator {}", permissionCode, principal.getName(),
                    container.getCreator().getUsername());
            return true;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        log.error("Failed to grant permission {} because container is not owner by the current user", permissionCode);
        return false;
    }

}
