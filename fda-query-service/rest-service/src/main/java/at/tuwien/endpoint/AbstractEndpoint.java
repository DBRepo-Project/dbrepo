package at.tuwien.endpoint;

import at.tuwien.api.user.UserDetailsDto;
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
import org.springframework.security.core.userdetails.UserDetails;

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
        } catch (DatabaseNotFoundException | TableNotFoundException e) {
            log.error("Failed to find table with id {}", tableId);
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (table.getDatabase().getIsPublic() && List.of("DATA_EXPORT", "DATA_VIEW").contains(permissionCode)) {
            return true;
        }
        /* modification operations are limited to the creator */
        if (principal == null) {
            return false;
        }
        final UserDetails userDetails = (UserDetailsDto) principal /* with pre-authorization this always holds */;
        if (userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Current user has insufficient authorities");
            log.debug("current user misses ROLE_RESEARCHER");
            return false;
        }
        return table.getDatabase().getCreator().getUsername().equals(userDetails.getUsername());
    }

    protected Boolean hasQueryPermission(Long databaseId, Long queryId, String permissionCode, Principal principal) {
        final Database database;
        try {
            database = databaseService.find(databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", databaseId);
            return false;
        }
        if (!database.getIsPublic() && !hasPublicIdentifier(queryId, principal)) {
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (List.of("QUERY_VIEW_ALL", "QUERY_VIEW", "QUERY_EXPORT").contains(permissionCode)) {
            return true;
        }
        /* modification operations are limited to the creator */
        if (principal == null) {
            return false;
        }
        final UserDetails userDetails = (UserDetailsDto) principal /* with pre-authorization this always holds */;
        if (userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Current user has insufficient authorities");
            log.debug("current user misses ROLE_RESEARCHER");
            return false;
        }
        /* modification operations are limited to the creator */
        return database.getCreator().getUsername().equals(userDetails.getUsername());
    }

    /**
     * Determines if an identifier exists for a query with id and if the principal user is allowed to see it
     *
     * @param queryId   The query id.
     * @param principal The principal.
     * @return True if the query result set can be viewed, false otherwise.
     */
    protected Boolean hasPublicIdentifier(Long queryId, Principal principal) {
        final Identifier identifier;
        try {
            identifier = identifierService.findByQueryId(queryId);
        } catch (IdentifierNotFoundException e) {
            log.warn("Identifier not found");
            return false;
        }
        if (identifier.getVisibility().equals(EVERYONE)) {
            return true;
        }
        if (principal == null) {
            return false;
        }
        final UserDetails userDetails = (UserDetails) principal;
        return identifier.getCreator().getUsername().equals(userDetails.getUsername());
    }

}
