package at.tuwien.endpoint;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.List;

import static at.tuwien.entities.identifier.VisibilityType.EVERYONE;

@Slf4j
public abstract class AbstractEndpoint {

    private final TableService tableService;
    private final DatabaseService databaseService;
    private final IdentifierService identifierService;

    @Autowired
    protected AbstractEndpoint(TableService tableService, DatabaseService databaseService,
                               IdentifierService identifierService) {
        this.tableService = tableService;
        this.databaseService = databaseService;
        this.identifierService = identifierService;
    }

    protected Boolean hasDatabasePermission(Long databaseId, Long tableId, String permissionCode,
                                            Principal principal) {
        final Table table;
        try {
            table = tableService.find(databaseId, tableId);
        } catch (DatabaseNotFoundException e) {
            log.debug("failed to find database with id {}", databaseId);
            return false;
        } catch (TableNotFoundException e) {
            log.debug("failed to find table with id {} in database with id {}", tableId, databaseId);
            return false;
        }
        if (principal != null && table.getDatabase().getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user is creator of database with id {}", permissionCode, databaseId);
            return true;
        }
        /* view-only operations are allowed on public databases */
        if (table.getDatabase().getIsPublic() && List.of("DATA_EXPORT", "DATA_VIEW").contains(permissionCode)) {
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

    protected Boolean hasQueryPermission(Long databaseId, Long queryId, String permissionCode, Principal principal) {
        final Database database;
        try {
            database = databaseService.find(databaseId);
        } catch (DatabaseNotFoundException e) {
            log.debug("failed to find database with id {}", databaseId);
            return false;
        }
        if (hasPublicIdentifier(databaseId, queryId, permissionCode)) {
            return true;
        }
        /* modification operations are limited to the creator */
        if (isMyPrivateIdentifier(databaseId, queryId, principal, permissionCode)) {
            return true;
        }
        /* view-only operations are allowed on public databases */
        if (database.getIsPublic() && List.of("QUERY_VIEW_ALL", "QUERY_VIEW", "QUERY_EXPORT").contains(
                permissionCode)) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
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
        /* modification operations are limited to the creator */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because database is private and creator is the current user",
                    permissionCode);
            return true;
        }
        log.debug("failed to grant permission {} because database is private and creator is not the " +
                "current user", permissionCode);
        return false;
    }

    protected Boolean hasPublicIdentifier(Long databaseId, Long queryId, String permissionCode) {
        final Identifier identifier;
        try {
            identifier = identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
        } catch (IdentifierNotFoundException e) {
            log.debug("failed to find identifier with database id {} and query id {}", databaseId, queryId);
            return false;
        }
        if (identifier.getVisibility().equals(EVERYONE)) {
            log.debug("grant permission {} because identifier visibility is public", permissionCode);
            return true;
        }
        log.debug("failed to grant permission {} because identifier visibility is not public", permissionCode);
        return false;
    }

    protected Boolean isMyPrivateIdentifier(Long databaseId, Long queryId, Principal principal, String permissionCode) {
        final Identifier identifier;
        try {
            identifier = identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
        } catch (IdentifierNotFoundException e) {
            log.debug("failed to find identifier with database id {} and query id {}", databaseId, queryId);
            return false;
        }
        if (identifier.getDatabase().getIsPublic()) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
        if (principal == null) {
            log.debug("failed to grant permission {} because database is private and principal is null",
                    permissionCode);
            return false;
        }
        if (identifier.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because database is private and identifier creator is the current user",
                    permissionCode);
            return true;
        }
        log.debug("failed to grant permission {} because database is private and identifier creator is not the " +
                        "current user", permissionCode);
        return false;
    }

}
