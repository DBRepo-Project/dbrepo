package at.tuwien.endpoint;

import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.ml.dataframe.QueryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.tuwien.entities.identifier.VisibilityType.EVERYONE;

@Slf4j
public abstract class AbstractEndpoint {

    private final QueryConfig queryConfig;
    private final TableService tableService;
    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final IdentifierService identifierService;

    @Autowired
    protected AbstractEndpoint(TableService tableService, AccessService accessService, DatabaseService databaseService,
                               IdentifierService identifierService, QueryConfig queryConfig) {
        this.queryConfig = queryConfig;
        this.tableService = tableService;
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.identifierService = identifierService;
    }

    protected Boolean hasDatabasePermission(Long containerId, Long databaseId, String permissionCode,
                                            Principal principal) throws NotAllowedException {
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
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        /* modification operations are limited to the creator */
        if (database.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user {} is creator {}", permissionCode, principal.getName(),
                    database.getCreator().getUsername());
            return true;
        }
        /* check view access */
        if (List.of("QUERY_EXECUTE").contains(permissionCode)) {
            log.debug("grant permission {} because user has access {}", permissionCode, access.getType());
            return true;
        }
        /* write permission */
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

    /**
     * Do not allow aggregate functions and comments
     * https://mariadb.com/kb/en/aggregate-functions/
     */
    protected void validateForbiddenStatements(ExecuteStatementDto data) throws QueryMalformedException {
        final List<String> words = new LinkedList<>();
        Arrays.stream(queryConfig.getNotSupportedKeywords())
                .forEach(keyword -> {
                    final Pattern pattern = Pattern.compile(keyword);
                    final Matcher matcher = pattern.matcher(data.getStatement());
                    final boolean found = matcher.find();
                    if (found) {
                        words.add(keyword);
                    }
                });
        if (words.size() == 0) {
            return;
        }
        log.error("Query contains forbidden keyword(s): {}", words);
        log.debug("forbidden keywords: {}", words);
        throw new QueryMalformedException("Query contains forbidden keyword(s): " + Arrays.toString(words.toArray()));
    }

    protected Boolean hasTablePermission(Long containerId, Long databaseId, Long tableId, String permissionCode,
                                         Principal principal) throws NotAllowedException {
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
            log.error("Failed to find table with id {} in database with id {}", tableId, databaseId);
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
        if (table.getCreator().getUsername().equals(principal.getName())) {
            log.debug("grant permission {} because user {} is table creator {}", permissionCode, principal.getName(),
                    table.getCreator().getUsername());
            return true;
        }
        final Authentication authentication = (Authentication) principal /* with pre-authorization this always holds */;
        if (authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_RESEARCHER"))) {
            log.error("Failed to grant permission {} because current user misses authority 'ROLE_RESEARCHER'",
                    permissionCode);
            return false;
        }
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        /* check view access */
        if (List.of("TABLE_EXPORT", "DATA_VIEW", "DATA_HISTORY", "QUERY_VIEW_ALL", "QUERY_RE_EXECUTE", "QUERY_VIEW", "FIND_VIEW").contains(permissionCode)) {
            log.trace("grant permission {} because user has access {}", permissionCode, access.getType());
            return true;
        }
        if (List.of("DATA_INSERT", "DATA_UPDATE", "DATA_DELETE", "QUERY_PERSIST").contains(permissionCode) && (access.getType().equals(AccessType.WRITE_ALL))) {
            /* write own is already effective with creator check above */
            log.debug("grant permission {} because user {} is has table write permission {}", permissionCode, principal.getName(),
                    access.getType());
            return true;
        }
        log.debug("failed to grant permission {} because database is not owner by the current user and also has not appropriate access", permissionCode);
        return false;
    }

    protected Boolean hasQueryPermission(Long containerId, Long databaseId, Long queryId, String permissionCode,
                                         Principal principal) throws NotAllowedException {
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
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        /* check view access */
        if (List.of("DATA_VIEW", "DATA_HISTORY", "QUERY_VIEW_ALL", "QUERY_RE_EXECUTE", "QUERY_VIEW", "FIND_VIEW", "QUERY_EXPORT").contains(permissionCode)) {
            log.trace("grant permission {} because user has access {}", permissionCode, access.getType());
            return true;
        }
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.trace("grant permission {} because user has access {}", permissionCode, access.getType());
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
