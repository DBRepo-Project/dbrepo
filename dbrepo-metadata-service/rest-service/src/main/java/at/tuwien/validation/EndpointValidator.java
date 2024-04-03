package at.tuwien.validation;

import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.utils.UserUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Component
public class EndpointValidator {

    private final QueryConfig queryConfig;
    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final TableService tableService;

    @Autowired
    public EndpointValidator(QueryConfig queryConfig, AccessService accessService, DatabaseService databaseService,
                             TableService tableService) {
        this.queryConfig = queryConfig;
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.tableService = tableService;
    }

    public void validateOnlyPrivateAccess(Long databaseId, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, DatabaseNotFoundException, AccessDeniedException {
        final Database database = databaseService.find(databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        validateOnlyAccess(databaseId, principal, writeAccessOnly);
    }

    public void validateOnlyPrivateAccess(Long databaseId, Principal principal) throws NotAllowedException,
            DatabaseNotFoundException, AccessDeniedException {
        validateOnlyPrivateAccess(databaseId, principal, false);
    }

    public void validateOnlyAccess(Long databaseId, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, DatabaseNotFoundException, AccessDeniedException {
        if (principal == null) {
            log.error("Access not allowed: database with id {} is not public and no authorization provided", databaseId);
            throw new NotAllowedException("Access not allowed: database with id " + databaseId + " is not public and no authorization provided");
        }
        databaseService.find(databaseId);
        log.trace("principal: {}", principal.getName());
        final DatabaseAccess access = accessService.find(databaseId, UserUtil.getId(principal));
        log.trace("found access: {}", access);
        if (writeAccessOnly && !(access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.error("Access not allowed: no write access");
            throw new NotAllowedException("Access not allowed: no write access");
        }
    }

    public void validateColumnCreateConstraints(TableCreateDto data) throws TableMalformedException {
        if (data == null) {
            throw new TableMalformedException("Validation failed: table data is null");
        }
        final List<ColumnTypeDto> needSize = List.of(ColumnTypeDto.CHAR, ColumnTypeDto.VARCHAR, ColumnTypeDto.BINARY, ColumnTypeDto.VARBINARY, ColumnTypeDto.BIT, ColumnTypeDto.TINYINT, ColumnTypeDto.SMALLINT, ColumnTypeDto.MEDIUMINT, ColumnTypeDto.INT);
        final List<ColumnTypeDto> needSizeAndD = List.of(ColumnTypeDto.DOUBLE, ColumnTypeDto.DECIMAL);
        final List<ColumnTypeDto> needDateFormat = List.of(ColumnTypeDto.DATETIME, ColumnTypeDto.TIMESTAMP, ColumnTypeDto.TIME);
        /* check size */
        final Optional<ColumnCreateDto> optional0 = data.getColumns()
                .stream()
                .filter(c -> Objects.isNull(c.getSize()))
                .filter(c -> needSize.contains(c.getType()))
                .findFirst();
        if (optional0.isPresent()) {
            log.error("Validation failed: column {} needs size parameter", optional0.get().getName());
            throw new TableMalformedException("Validation failed: column " + optional0.get().getName() + " needs size parameter");
        }
        /* check size and d */
        final Optional<ColumnCreateDto> optional1 = data.getColumns()
                .stream()
                .filter(c -> needSizeAndD.contains(c.getType()))
                .filter(c -> Objects.isNull(c.getSize()) || Objects.isNull(c.getD()))
                .findFirst();
        if (optional1.isPresent()) {
            log.error("Validation failed: column {} needs size and d parameter", optional1.get().getName());
            throw new TableMalformedException("Validation failed: column " + optional1.get().getName() + " needs size and d parameter");
        }
        final Optional<ColumnCreateDto> optional1a = data.getColumns()
                .stream()
                .filter(c -> needSizeAndD.contains(c.getType()))
                .filter(c -> c.getSize() > 65 || c.getD() > 38)
                .findFirst();
        if (optional1a.isPresent()) {
            log.error("Validation failed: column {} needs size (max 65) and d (max 30)", optional1a.get().getName());
            throw new TableMalformedException("Validation failed: column " + optional1a.get().getName() + " needs size (max 65) and d (max 30)");
        }
        final Optional<ColumnCreateDto> optional1b = data.getColumns()
                .stream()
                .filter(c -> needSizeAndD.contains(c.getType()))
                .filter(c -> c.getSize() < c.getD())
                .findFirst();
        if (optional1b.isPresent()) {
            log.error("Validation failed: column {} needs size >= d", optional1b.get().getName());
            throw new TableMalformedException("Validation failed: column " + optional1b.get().getName() + " needs size >= d");
        }
        /* check enum */
        final Optional<ColumnCreateDto> optional2 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.ENUM))
                .filter(c -> c.getEnums() == null || c.getEnums().isEmpty())
                .findFirst();
        if (optional2.isPresent()) {
            log.error("Validation failed: column {} needs at least 1 allowed enum value", optional2.get().getName());
            throw new TableMalformedException("Validation failed: column " + optional2.get().getName() + " needs at least 1 allowed enum value");
        }
        /* check set */
        final Optional<ColumnCreateDto> optional3 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.SET))
                .filter(c -> c.getEnums() == null || c.getSets().isEmpty())
                .findFirst();
        if (optional3.isPresent()) {
            log.error("Validation failed: column {} needs at least 1 allowed set value", optional3.get().getName());
            throw new TableMalformedException("Validation failed: column " + optional3.get().getName() + " needs at least 1 allowed set value");
        }
        /* check date */
        final Optional<ColumnCreateDto> optional4 = data.getColumns()
                .stream()
                .filter(c -> needDateFormat.contains(c.getType()))
                .filter(c -> Objects.isNull(c.getDfid()))
                .findFirst();
        if (optional4.isPresent()) {
            log.error("Validation failed: column {} needs a format", optional4.get().getName());
            throw new TableMalformedException("Validation failed: column " + optional4.get().getName() + " needs a format");
        }
    }

    public boolean validateOnlyMineOrWriteAccessOrHasRole(UUID ownerId, Principal principal, DatabaseAccess access, String role) {
        if (UserUtil.hasRole(principal, role)) {
            log.debug("validation passed: role {} present", role);
            return true;
        }
        if (access == null) {
            /* should never happen */
            log.error("validation failed: access is null");
            return false;
        }
        if (ownerId.equals(UserUtil.getId(principal)) && (access.getType().equals(AccessType.WRITE_ALL) || access.getType().equals(AccessType.WRITE_OWN))) {
            log.debug("validation passed: user id {} matches owner id {} and has write access {}", UserUtil.getId(principal), ownerId, access.getType());
            return true;
        }
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.debug("validation passed: user with id {} has write all access", UserUtil.getId(principal));
            return true;
        }
        log.debug("validation failed: user with id {} has insufficient access {} or role", UserUtil.getId(principal), access.getType());
        return false;
    }

    public boolean validateOnlyMineOrReadAccessOrHasRole(UUID ownerId, Principal principal, DatabaseAccess access, String role) {
        if (validateOnlyMineOrWriteAccessOrHasRole(ownerId, principal, access, role)) {
            return true;
        }
        if (access.getType().equals(AccessType.READ)) {
            log.debug("validation passed: user with id {} has read access", UserUtil.getId(principal));
            return true;
        }
        log.debug("validation failed: user with id {} has insufficient access {} or role", UserUtil.getId(principal), access.getType());
        return false;
    }

    public void validateOnlyOwnerOrWriteAll(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, NotAllowedException, TableNotFoundException, AccessDeniedException {
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        final Table table = tableService.find(databaseId, tableId);
        log.trace("principal: {}", principal.getName());
        log.trace("table creator: {}", table.getCreatedBy());
        final DatabaseAccess access = accessService.find(databaseId, UserUtil.getId(principal));
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.READ)) {
            log.error("Access not allowed: insufficient access (only read-access)");
            throw new NotAllowedException("Access not allowed: insufficient access (only read-access)");
        }
        if (table.getCreatedBy().equals(UserUtil.getId(principal)) && (access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.trace("grant access: table creator with write access");
            return;
        }
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.trace("grant access: write-all access");
            return;
        }
        log.error("Access not allowed: insufficient access (neither creator {} nor write-all access)", table.getCreatedBy());
        throw new NotAllowedException("Access not allowed: insufficient access (neither creator nor write-all access)");
    }

    public void validateOnlyPrivateHasRole(Long databaseId, Principal principal, String role)
            throws DatabaseNotFoundException, NotAllowedException {
        final Database database = databaseService.find(databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        log.trace("database with id {} is private", databaseId);
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal: {}", principal.getName());
        if (!UserUtil.hasRole(principal, role)) {
            log.error("Access not allowed: role {} missing", role);
            throw new NotAllowedException("Access not allowed: role " + role + " missing");
        }
        log.trace("principal has role '{}': access granted", role);
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
        if (words.isEmpty()) {
            return;
        }
        log.error("Query contains forbidden keyword(s): {}", words);
        throw new QueryMalformedException("Query contains forbidden keyword(s): " + Arrays.toString(words.toArray()));
    }

    public void validateOnlyAccessOrPublic(Long databaseId, Principal principal)
            throws DatabaseNotFoundException, NotAllowedException, AccessDeniedException {
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
        final DatabaseAccess access = accessService.find(databaseId, UserUtil.getId(principal));
        log.trace("found access {}", access);
    }

    public void validateOnlyWriteOwnOrWriteAllAccess(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException, AccessDeniedException {
        final Table table = tableService.find(databaseId, tableId);
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal is {}", principal);
        final DatabaseAccess access = accessService.find(databaseId, UserUtil.getId(principal));
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.debug("user {} has write-all access, skip.", principal.getName());
            return;
        }
        if (table.getOwnedBy().equals(UserUtil.getId(principal)) && access.getType().equals(AccessType.WRITE_OWN)) {
            log.debug("user {} has write-own access to their own table, skip.", principal.getName());
            return;
        }
        log.error("Access not allowed: no write access for table with id {}", tableId);
        throw new NotAllowedException("Access not allowed: no write access for table with id " + tableId);
    }

    /**
     * Precondition: identifier.getPublicationYear() is not null
     *
     * @param identifier The identifier that will be created.
     * @return True if the publication date is valid, false otherwise.
     */
    public boolean validatePublicationDate(IdentifierSaveDto identifier) {
        if (identifier.getPublicationMonth() != null && (identifier.getPublicationMonth() < 1 || identifier.getPublicationMonth() > 12)) {
            log.trace("publication month {} needs to fulfill: 1 >= publicationMonth <= 12", identifier.getPublicationMonth());
            return false;
        }
        if (identifier.getPublicationDay() != null && (identifier.getPublicationDay() < 1 || identifier.getPublicationDay() > 31)) {
            log.trace("publication day {} needs to fulfill: 1 >= publicationDay <= 31", identifier.getPublicationDay());
            return false;
        }
        if (identifier.getPublicationMonth() != null && identifier.getPublicationDay() != null) {
            final String paddedMonth = identifier.getPublicationMonth() <= 9 ? "0" + identifier.getPublicationMonth() : "" + identifier.getPublicationMonth();
            final String paddedDay = identifier.getPublicationDay() <= 9 ? "0" + identifier.getPublicationDay() : "" + identifier.getPublicationDay();
            final boolean result = GenericValidator.isDate(identifier.getPublicationYear() + "-" + paddedMonth + "-" + paddedDay, "yyyy-MM-dd", true);
            if (!result) {
                log.trace("publication date {}-{}-{} needs to be valid", identifier.getPublicationYear(), paddedMonth,
                        identifier.getPublicationDay());
                return false;
            }
            return true;
        }
        log.trace("publication date is valid");
        return true;
    }

}
