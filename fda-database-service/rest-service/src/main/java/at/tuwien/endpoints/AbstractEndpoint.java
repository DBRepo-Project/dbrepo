package at.tuwien.endpoints;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractEndpoint {

    private final DatabaseService databaseService;
    private final ContainerService containerService;
    private final DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    protected AbstractEndpoint(DatabaseService databaseService, ContainerService containerService,
                               DatabaseAccessRepository databaseAccessRepository) {
        this.databaseService = databaseService;
        this.containerService = containerService;
        this.databaseAccessRepository = databaseAccessRepository;
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
        if (database.getIsPublic() && false) {
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
        /* modification operations are limited to the owner */
        if (database.getOwner().getUsername().equals(principal.getName())) {
            log.trace("grant permission {} because user {} is owner {}", permissionCode, principal.getName(),
                    database.getOwner().getUsername());
            return true;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (List.of("VISIBILITY_DATABASE").contains(permissionCode) && isDeveloper(authentication)) {
            log.debug("grant permission {} because user {} is developer", permissionCode, principal.getName());
            return true;
        }
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, principal.getName());
        if (optional.isEmpty()) {
            log.error("Failed to grant permission {} because user {} does not have access", permissionCode, principal.getName());
            return false;
        }
        final DatabaseAccess access = optional.get();
        log.trace("access type: {}", access.getType());
        if (List.of().contains(permissionCode) && hasAccess(access) && isResearcher(authentication)) {
            log.debug("grant permission {} because user {} has access type {} and is researcher", permissionCode,
                    principal.getName(), optional.get().getType());
            return true;
        }
        log.error("Failed to grant permission {} because user {} does not have access", permissionCode, principal.getName());
        return false;
    }

    protected Boolean hasContainerPermission(Long containerId, String permissionCode, Principal principal) {
        log.trace("validate has container permission, containerId={}, permissionCode={}, principal={}",
                containerId, permissionCode, principal);
        final Container container;
        try {
            container = containerService.find(containerId);
        } catch (ContainerNotFoundException e) {
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
        /* modification operations are limited to the owner */
        if (container.getOwner().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user {} is owner {}", permissionCode, principal.getName(),
                    container.getOwner().getUsername());
            return true;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (List.of("CREATE_DATABASE").contains(permissionCode) &&
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEVELOPER"))) {
            log.debug("grant permission {} because user {} is developer", permissionCode, principal.getName());
            return true;
        }
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        log.error("Failed to grant permission {} because container is not owner by the current user", permissionCode);
        return false;
    }

    protected boolean isResearcher(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"));
    }

    protected boolean isDeveloper(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DEVELOPER"));
    }

    protected boolean hasReadAccess(DatabaseAccess access) {
        return access.getType().equals(AccessType.READ);
    }

    protected boolean hasWriteAccess(DatabaseAccess access) {
        return access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL);
    }

    protected boolean hasAccess(DatabaseAccess access) {
        return hasReadAccess(access) || hasWriteAccess(access);
    }
}
