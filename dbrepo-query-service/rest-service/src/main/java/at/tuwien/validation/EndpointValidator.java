package at.tuwien.validation;

import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.VisibilityType;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Component
public class EndpointValidator {

    private final QueryConfig queryConfig;
    private final TableService tableService;
    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final IdentifierRepository identifierRepository;

    @Autowired
    public EndpointValidator(QueryConfig queryConfig, TableService tableService, AccessService accessService,
                             DatabaseService databaseService, IdentifierRepository identifierRepository) {
        this.queryConfig = queryConfig;
        this.tableService = tableService;
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.identifierRepository = identifierRepository;
    }

    public void validateDataParams(Long page, Long size) throws PaginationException {
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

    public void validateDataParams(Long page, Long size, SortType sortDirection, String sortColumn)
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
    public void validateForbiddenStatements(ExecuteStatementDto data) throws QueryMalformedException {
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
        throw new QueryMalformedException("Query contains forbidden keyword(s): " + Arrays.toString(words.toArray()));
    }

    public void validateOnlyAccessOrPublic(Long databaseId, Principal principal)
            throws DatabaseNotFoundException, NotAllowedException {
        final Database database = databaseService.find(databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        log.trace("database with id {} is private", databaseId);
        if (principal == null) {
            log.error("Access not allowed: database with id {} is not public and no authorization provided", databaseId);
            throw new NotAllowedException("Access not allowed: database with id " + databaseId + " is not public and no authorization provided");
        }
        log.trace("principal is {}", principal);
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        log.trace("found access {}", access);
    }

    public void validateOnlyAccessOrPublic(Long databaseId, Long queryId, Principal principal)
            throws NotAllowedException, DatabaseNotFoundException {
        final Optional<Identifier> optional = identifierRepository.findSubsetIdentifier(databaseId, queryId);
        if (optional.isPresent()) {
            final Identifier identifier = optional.get();
            log.trace("found identifier for query with id {}", queryId);
            if (principal != null && identifier.getVisibility().equals(VisibilityType.SELF)) {
                if (identifier.getCreator().getUsername().equals(principal.getName())) {
                    return;
                }
                log.error("Access not allowed: visibility is 'self' and user is not the creator");
                throw new NotAllowedException("Access not allowed: visibility is 'self' and you are not the creator");
            }
            if (!identifier.getVisibility().equals(VisibilityType.EVERYONE)) {
                log.error("Access not allowed: visibility is not 'everyone'");
                throw new NotAllowedException("Access not allowed: visibility is not 'everyone'");
            }
            log.trace("identifier is public, validation passed");
            return;
        }
        validateOnlyAccessOrPublic(databaseId, principal);
    }

    public void validateOnlyWriteOwnOrWriteAllAccess(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException {
        final Table table = tableService.find(databaseId, tableId);
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal is {}", principal);
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.debug("user {} has write-all access, skip.", principal.getName());
            return;
        }
        if (table.getOwner().getUsername().equals(principal.getName()) && access.getType().equals(AccessType.WRITE_OWN)) {
            log.debug("user {} has write-own access to their own table, skip.", principal.getName());
            return;
        }
        log.error("Access not allowed: no write access for table with id {}", tableId);
        throw new NotAllowedException("Access not allowed: no write access for table with id " + tableId);
    }

}
