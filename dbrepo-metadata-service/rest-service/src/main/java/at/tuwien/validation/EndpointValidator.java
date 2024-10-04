package at.tuwien.validation;

import at.tuwien.SortType;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.UserService;
import at.tuwien.utils.UserUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;

@Log4j2
@Component
public class EndpointValidator {

    private final UserService userService;
    private final AccessService accessService;

    @Autowired
    public EndpointValidator(UserService userService, AccessService accessService) {
        this.userService = userService;
        this.accessService = accessService;
    }

    public void validateOnlyPrivateAccess(Database database, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, UserNotFoundException, AccessNotFoundException {
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", database.getId());
            return;
        }
        validateOnlyAccess(database, principal, writeAccessOnly);
    }

    public void validateOnlyPrivateAccess(Database database, Principal principal) throws NotAllowedException,
            UserNotFoundException, AccessNotFoundException {
        validateOnlyPrivateAccess(database, principal, false);
    }

    public void validateOnlyAccess(Database database, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, UserNotFoundException, AccessNotFoundException {
        if (principal == null) {
            throw new NotAllowedException("No principal provided");
        }
        final User user = userService.findByUsername(principal.getName());
        final DatabaseAccess access = accessService.find(database, user);
        log.trace("found access: {}", access);
        if (writeAccessOnly && !(access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.error("Access not allowed: no write access");
            throw new NotAllowedException("Access not allowed: no write access");
        }
    }

    public void validateColumnCreateConstraints(TableCreateDto data) throws MalformedException {
        if (data == null) {
            throw new MalformedException("Validation failed: table data is null");
        }
        final List<ColumnTypeDto> needSize = List.of(ColumnTypeDto.CHAR, ColumnTypeDto.VARCHAR);
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
            throw new MalformedException("Validation failed: column " + optional0.get().getName() + " needs size parameter");
        }
        /* check size and d */
        final Optional<ColumnCreateDto> optional1 = data.getColumns()
                .stream()
                .filter(c -> needSizeAndD.contains(c.getType()))
                .filter(c -> Objects.isNull(c.getSize()) || Objects.isNull(c.getD()))
                .findFirst();
        if (optional1.isPresent()) {
            log.error("Validation failed: column {} needs size and d parameter", optional1.get().getName());
            throw new MalformedException("Validation failed: column " + optional1.get().getName() + " needs size and d parameter");
        }
        final Optional<ColumnCreateDto> optional1a = data.getColumns()
                .stream()
                .filter(c -> needSizeAndD.contains(c.getType()))
                .filter(c -> c.getSize() > 65 || c.getD() > 38)
                .findFirst();
        if (optional1a.isPresent()) {
            log.error("Validation failed: column {} needs size (max 65) and d (max 30)", optional1a.get().getName());
            throw new MalformedException("Validation failed: column " + optional1a.get().getName() + " needs size (max 65) and d (max 30)");
        }
        final Optional<ColumnCreateDto> optional1b = data.getColumns()
                .stream()
                .filter(c -> needSizeAndD.contains(c.getType()))
                .filter(c -> c.getSize() < c.getD())
                .findFirst();
        if (optional1b.isPresent()) {
            log.error("Validation failed: column {} needs size >= d", optional1b.get().getName());
            throw new MalformedException("Validation failed: column " + optional1b.get().getName() + " needs size >= d");
        }
        /* check enum */
        final Optional<ColumnCreateDto> optional2 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.ENUM))
                .filter(c -> c.getEnums() == null || c.getEnums().isEmpty())
                .findFirst();
        if (optional2.isPresent()) {
            log.error("Validation failed: column {} needs at least 1 allowed enum value", optional2.get().getName());
            throw new MalformedException("Validation failed: column " + optional2.get().getName() + " needs at least 1 allowed enum value");
        }
        /* check set */
        final Optional<ColumnCreateDto> optional3 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.SET))
                .filter(c -> c.getEnums() == null || c.getSets().isEmpty())
                .findFirst();
        if (optional3.isPresent()) {
            log.error("Validation failed: column {} needs at least 1 allowed set value", optional3.get().getName());
            throw new MalformedException("Validation failed: column " + optional3.get().getName() + " needs at least 1 allowed set value");
        }
        /* check date */
        final Optional<ColumnCreateDto> optional4 = data.getColumns()
                .stream()
                .filter(c -> needDateFormat.contains(c.getType()))
                .filter(c -> Objects.isNull(c.getDfid()))
                .findFirst();
        if (optional4.isPresent()) {
            log.error("Validation failed: column {} needs a format", optional4.get().getName());
            throw new MalformedException("Validation failed: column " + optional4.get().getName() + " needs a format");
        }
    }

    public boolean validateOnlyMineOrWriteAccessOrHasRole(User owner, Principal principal, DatabaseAccess access, String role) {
        if (UserUtil.hasRole(principal, role)) {
            log.debug("validation passed: role {} present", role);
            return true;
        }
        if (access == null) {
            /* should never happen */
            log.error("validation failed: access is null");
            return false;
        }
        if (owner.equals(principal) && (access.getType().equals(AccessType.WRITE_ALL) || access.getType().equals(AccessType.WRITE_OWN))) {
            log.debug("validation passed: user {} matches owner {} and has write access {}", principal.getName(), owner.getUsername(), access.getType());
            return true;
        }
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.debug("validation passed: user {} has write all access", principal.getName());
            return true;
        }
        log.debug("validation failed: user {} has insufficient access {} or role", principal.getName(), access.getType());
        return false;
    }

    public boolean validateOnlyMineOrReadAccessOrHasRole(User creator, Principal principal, DatabaseAccess access, String role) {
        if (validateOnlyMineOrWriteAccessOrHasRole(creator, principal, access, role)) {
            return true;
        }
        if (access.getType().equals(AccessType.READ)) {
            log.debug("validation passed: user {} has read access", principal.getName());
            return true;
        }
        log.debug("validation failed: user {} has insufficient access {} or role", principal.getName(), access.getType());
        return false;
    }

    @Transactional(readOnly = true)
    public void validateOnlyOwnerOrWriteAll(Table table, User user) throws NotAllowedException,
            AccessNotFoundException {
        log.trace("table creator: {}", table.getCreatedBy());
        final DatabaseAccess access = accessService.find(table.getDatabase(), user);
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.READ)) {
            log.error("Access not allowed: insufficient access (only read-access)");
            throw new NotAllowedException("Access not allowed: insufficient access (only read-access)");
        }
        if (table.getCreatedBy().equals(user.getId()) && (access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
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

    public void validateOnlyPrivateHasRole(Database database, Principal principal, String role)
            throws NotAllowedException {
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", database.getId());
            return;
        }
        log.trace("database with id {} is private", database.getId());
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

    public void validateOnlyAccessOrPublic(Database database, Principal principal) throws NotAllowedException,
            AccessNotFoundException {
        if (database.getIsPublic()) {
            log.debug("database with id {} is public: no access needed", database.getId());
            return;
        }
        log.trace("database with id {} is private", database.getId());
        if (principal == null) {
            log.error("Access not allowed: database with id {} is not public and no authorization provided", database.getId());
            throw new NotAllowedException("Access not allowed: database with id " + database.getId() + " is not public and no authorization provided");
        }
        final User user = User.builder()
                .username(principal.getName())
                .build();
        final DatabaseAccess access = accessService.find(database, user);
        log.trace("found access {}", access);
    }

    @Transactional(readOnly = true)
    public void validateOnlyWriteOwnOrWriteAllAccess(Table table, User user) throws NotAllowedException,
            AccessNotFoundException {
        final DatabaseAccess access = accessService.find(table.getDatabase(), user);
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.debug("user {} has write-all access, skip.", user.getId());
            return;
        }
        if (table.getOwnedBy().equals(user.getId()) && access.getType().equals(AccessType.WRITE_OWN)) {
            log.debug("user {} has write-own access to their own table, skip.", user.getId());
            return;
        }
        log.error("Access not allowed: no write access for table with id {}", table.getId());
        throw new NotAllowedException("Access not allowed: no write access for table with id " + table.getId());
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
