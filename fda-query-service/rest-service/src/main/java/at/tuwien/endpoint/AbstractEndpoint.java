package at.tuwien.endpoint;

import at.tuwien.SortType;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static at.tuwien.entities.identifier.VisibilityType.EVERYONE;

@Slf4j
public abstract class AbstractEndpoint {

    private final TableService tableService;
    private final DatabaseService databaseService;
    private final IdentifierService identifierService;
    private final DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    protected AbstractEndpoint(TableService tableService, DatabaseService databaseService,
                               IdentifierService identifierService, DatabaseAccessRepository databaseAccessRepository) {
        this.tableService = tableService;
        this.databaseService = databaseService;
        this.identifierService = identifierService;
        this.databaseAccessRepository = databaseAccessRepository;
    }

    protected Boolean hasDatabasePermission(Long containerId, Long databaseId, String permissionCode,
                                            Principal principal) {
        log.trace("validate database permission, containerId={}, databaseId={}, permissionCode={}, principal={}",
                containerId, databaseId, permissionCode, principal);
        final Database database;
        try {
            database = databaseService.find(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", databaseId);
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (database.getIsPublic() && List.of("DATA_VIEW", "DATA_HISTORY", "QUERY_VIEW_ALL").contains(permissionCode)) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
        if (List.of("LIST_VIEWS", "FIND_VIEW", "DATA_VIEW").contains(permissionCode)) {
            log.debug("grant permission {} because it is allowed on public/private databases", permissionCode);
            return true;
        }
        if (principal == null) {
            log.error("Failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId,
                principal.getName());
        if (optional.isEmpty()) {
            log.error("Failed to grant permission {} because user has not access", permissionCode);
            return false;
        }
        final AccessType accessType = optional.get()
                .getType();
        /* check view access */
        if (List.of("QUERY_EXECUTE", "QUERY_PERSIST").contains(permissionCode)) {
            log.trace("grant permission {} because user has access {}", permissionCode, accessType);
            return true;
        }
        /* modification operations are limited to the creator */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user {} is creator {}", permissionCode, principal.getName(),
                    database.getCreator().getUsername());
            return true;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        log.error("Failed to grant permission {} because database is not owner by the current user", permissionCode);
        return false;
    }

    protected void validateDataParams(Long page, Long size) throws PaginationException {
        log.trace("validate data params, page={}, size={}", page, size);
        if ((page == null && size != null) || (page != null && size == null)) {
            log.error("Failed to validate page and/or size number, either both are present or none");
            throw new PaginationException("Failed to validate page and/or size number");
        }
        if (page != null && page < 0) {
            log.error("Failed to validate page number, is lower than zero");
            throw new PaginationException("Failed to validate page number");
        }
        if (size != null && size <= 0) {
            log.error("Failed to validate size number, is lower or equal than zero");
            throw new PaginationException("Failed to validate size number");
        }
    }

    protected void validateDataParams(Long page, Long size, SortType sortDirection, String sortColumn)
            throws PaginationException, SortException {
        log.trace("validate data params, page={}, size={}, sortDirection={}, sortColumn={}", page, size,
                sortDirection, sortColumn);
        validateDataParams(page, size);
        if ((sortDirection == null && sortColumn != null) || (sortDirection != null && sortColumn == null)) {
            log.error("Failed to validate sort direction and/or sort column, either both are present or none");
            throw new SortException("Failed to validate sort direction and/or sort column");
        }
    }

    protected Boolean hasTablePermission(Long containerId, Long databaseId, Long tableId, String permissionCode,
                                         Principal principal) {
        log.trace("validate queue permission, containerId={}, databaseId={}, tableId={}, permissionCode={}, principal={}",
                containerId, databaseId, tableId, permissionCode, principal);
        final Database database;
        try {
            database = databaseService.find(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", databaseId);
            return false;
        }
        final Table table;
        try {
            table = tableService.find(containerId, databaseId, tableId);
        } catch (TableNotFoundException e) {
            log.error("Failed to find table");
            return false;
        } catch (DatabaseNotFoundException e) {
            /* can never occur here */
            return false;
        }
        /* view-only operations are allowed on public databases */
        if (database.getIsPublic() && List.of("TABLE_EXPORT", "DATA_VIEW", "DATA_HISTORY").contains(permissionCode)) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
        if (principal == null) {
            log.error("Failed to grant permission {} because principal is null", permissionCode);
            return false;
        }
        /* modification operations for creators are trivial */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.trace("grant permission {} because user {} is database creator {}", permissionCode, principal.getName(),
                    database.getCreator().getUsername());
            return true;
        }
        if (table.getCreator().getUsername().equals(principal.getName())) {
            log.trace("grant permission {} because user {} is table creator {}", permissionCode, principal.getName(),
                    table.getCreator().getUsername());
            return true;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.debug("failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId,
                principal.getName());
        if (optional.isEmpty()) {
            log.error("Failed to grant permission {} because user has not access", permissionCode);
            return false;
        }
        final AccessType accessType = optional.get()
                .getType();
        /* check view access */
        if (List.of("TABLE_EXPORT", "DATA_VIEW", "DATA_HISTORY", "QUERY_VIEW_ALL", "QUERY_RE_EXECUTE", "QUERY_VIEW", "FIND_VIEW").contains(permissionCode)) {
            log.trace("grant permission {} because user has access {}", permissionCode, accessType);
            return true;
        }
        if (accessType.equals(AccessType.WRITE_ALL)) {
            log.trace("grant permission {} because user has access {}", permissionCode, accessType);
            return true;
        }
        log.debug("failed to grant permission {} because database is not owner by the current user and also has not appropriate access", permissionCode);
        return false;
    }

    protected Boolean hasQueryPermission(Long containerId, Long databaseId, Long queryId, String permissionCode,
                                         Principal principal) {
        log.trace("validate query permission, containerId={}, databaseId={}, queryId={}, permissionCode={}, principal={}",
                containerId, databaseId, queryId, permissionCode, principal);
        final Database database;
        try {
            database = databaseService.find(containerId, databaseId);
        } catch (DatabaseNotFoundException e) {
            log.error("Failed to find database with id {}", databaseId);
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
        if (database.getIsPublic() && List.of("QUERY_VIEW_ALL", "QUERY_VIEW", "QUERY_EXPORT", "QUERY_RE_EXECUTE").contains(
                permissionCode)) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
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
        /* modification operations are limited to the creator */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because database is private and creator is the current user",
                    permissionCode);
            return true;
        }
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId,
                principal.getName());
        if (optional.isEmpty()) {
            log.error("Failed to grant permission {} because user has not access", permissionCode);
            return false;
        }
        final AccessType accessType = optional.get()
                .getType();
        /* check view access */
        if (List.of("DATA_VIEW", "DATA_HISTORY", "QUERY_VIEW_ALL", "QUERY_RE_EXECUTE", "QUERY_VIEW", "FIND_VIEW").contains(permissionCode)) {
            log.trace("grant permission {} because user has access {}", permissionCode, accessType);
            return true;
        }
        if (accessType.equals(AccessType.WRITE_ALL)) {
            log.trace("grant permission {} because user has access {}", permissionCode, accessType);
            return true;
        }
        log.debug("failed to grant permission {} because database is not owner by the current user and also has not appropriate access", permissionCode);
        return false;
    }

    protected Boolean hasPublicIdentifier(Long databaseId, Long queryId, String permissionCode) {
        log.trace("validate has public identifier, databaseId={}, queryId={}, permissionCode={}", databaseId, queryId,
                permissionCode);
        final Identifier identifier;
        try {
            identifier = identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
        } catch (IdentifierNotFoundException e) {
            return false;
        }
        if (identifier.getVisibility().equals(EVERYONE)) {
            log.debug("grant permission {} because identifier visibility is public", permissionCode);
            return true;
        }
        log.error("Failed to grant permission {} because identifier visibility is not public", permissionCode);
        return false;
    }

    protected Boolean isMyPrivateIdentifier(Long databaseId, Long queryId, Principal principal, String permissionCode) {
        log.trace("validate is my private identifier, databaseId={}, queryId={}, permissionCode={}", databaseId, queryId,
                permissionCode);
        final Identifier identifier;
        try {
            identifier = identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
        } catch (IdentifierNotFoundException e) {
            return false;
        }
        if (identifier.getDatabase().getIsPublic()) {
            log.debug("grant permission {} because database is public", permissionCode);
            return true;
        }
        if (principal == null) {
            log.error("Failed to grant permission {} because database is private and principal is null",
                    permissionCode);
            return false;
        }
        if (identifier.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because database is private and identifier creator is the current user",
                    permissionCode);
            return true;
        }
        log.error("Failed to grant permission {} because database is private and identifier creator is not the current user", permissionCode);
        return false;
    }

}
