package at.tuwien.endpoints;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.List;


@Slf4j
public abstract class AbstractEndpoint {

    private final DatabaseService databaseService;

    @Autowired
    protected AbstractEndpoint(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    protected Boolean hasDatabasePermission(Long containerId, Long databaseId, String permissionCode,
                                            Principal principal) {
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
            log.debug("failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.debug("failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        log.debug("grant permission {} because user is creator", permissionCode);
        return true;
    }

    protected Boolean hasTablePermission(Long containerId, Long databaseId, Long tableId, String permissionCode,
                                            Principal principal) {
        final Database database;
        try {
            database = databaseService.find(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.debug("Failed to find database with id {}", databaseId);
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
            log.debug("failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.debug("failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        log.debug("grant permission {} because user is creator", permissionCode);
        return true;
    }

}
