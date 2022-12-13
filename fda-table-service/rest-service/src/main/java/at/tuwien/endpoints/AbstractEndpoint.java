package at.tuwien.endpoints;

import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.List;


@Slf4j
public abstract class AbstractEndpoint {

    private final AccessService accessService;
    private final DatabaseService databaseService;

    @Autowired
    protected AbstractEndpoint(AccessService accessService, DatabaseService databaseService) {
        this.accessService = accessService;
        this.databaseService = databaseService;
    }

    protected Boolean hasDatabasePermission(Long containerId, Long databaseId, String permissionCode,
                                            Principal principal) {
        log.debug("validate has database permission, containerId={}, databaseId={}, permissionCode={}, principal={}",
                containerId, databaseId, permissionCode, principal);
        final Database database;
        try {
            database = databaseService.find(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", databaseId);
            return false;
        }
        if (principal != null && database.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user is creator of database with id {}", permissionCode, databaseId);
            return true;
        }
        /* view-only operations are allowed on public databases */
        if (database.getIsPublic() && List.of("TABLE_CREATE", "TABLES_VIEW").contains(permissionCode)) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
        /* modification operations are limited to the creator */
        if (principal == null) {
            log.error("Failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        log.debug("grant permission {} because user is creator", permissionCode);
        return true;
    }

    protected Boolean hasTablePermission(Long containerId, Long databaseId, Long tableId, String permissionCode,
                                         Principal principal) throws AccessDeniedException {
        log.debug("validate has table permissions, containerId={}, databaseId={}, tableId={}, permissionCode={}, principal={}",
                containerId, databaseId, tableId, permissionCode, principal);
        final Database database;
        try {
            database = databaseService.find(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", databaseId);
            return false;
        }
        if (principal != null && database.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user is creator of database with id {}", permissionCode, databaseId);
            return true;
        }
        /* view-only operations are allowed on public databases */
        if (database.getIsPublic() && List.of("TABLE_INFO").contains(permissionCode)) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
        /* modification operations are limited to the creator */
        if (principal == null) {
            log.error("Failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        final DatabaseAccess access = accessService.hasAccess(databaseId, tableId, principal.getName());
        if (hasReadAccess(access) && List.of("TABLE_INFO", "CHECK_ACCESS").contains(permissionCode)) {
            log.debug("grant permission {} because user {} has at least read access", permissionCode, principal.getName());
            return true;
        }
        log.error("Failed to grant permission {} because user {} has insufficient access {} or is not creator", permissionCode, principal.getName(), access.getType());
        return false;
    }

    private boolean hasReadAccess(DatabaseAccess access) {
        return List.of(AccessType.READ, AccessType.WRITE_OWN, AccessType.WRITE_ALL).contains(access.getType());
    }

}
