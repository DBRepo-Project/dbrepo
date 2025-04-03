package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.SortType;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierSaveDto;
import at.ac.tuwien.ifs.dbrepo.endpoints.AbstractEndpoint;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Component
public class EndpointValidator extends AbstractEndpoint {

    public static final List<ColumnTypeDto> NEED_NOTHING = List.of(ColumnTypeDto.BOOL, ColumnTypeDto.SERIAL);
    public static final List<ColumnTypeDto> NEED_SIZE = List.of(ColumnTypeDto.VARCHAR, ColumnTypeDto.BINARY, ColumnTypeDto.VARBINARY);
    public static final List<ColumnTypeDto> CAN_HAVE_SIZE = List.of(ColumnTypeDto.CHAR, ColumnTypeDto.VARCHAR, ColumnTypeDto.BINARY, ColumnTypeDto.VARBINARY, ColumnTypeDto.BIT, ColumnTypeDto.TINYINT, ColumnTypeDto.SMALLINT, ColumnTypeDto.MEDIUMINT, ColumnTypeDto.INT);
    public static final List<ColumnTypeDto> CAN_HAVE_SIZE_AND_D = List.of(ColumnTypeDto.DOUBLE, ColumnTypeDto.DECIMAL);

    private final UserService userService;
    private final AccessService accessService;

    @Autowired
    public EndpointValidator(UserService userService, AccessService accessService) {
        this.userService = userService;
        this.accessService = accessService;
    }

    public void validateOnlyPrivateSchemaAccess(Database database, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, UserNotFoundException, AccessNotFoundException {
        if (database.getIsSchemaPublic()) {
            log.trace("database schema with id {} is public: no access needed", database.getId());
            return;
        }
        validateOnlyAccess(database, principal, writeAccessOnly);
    }

    public void validateOnlyPrivateSchemaAccess(Database database, Principal principal) throws NotAllowedException,
            UserNotFoundException, AccessNotFoundException {
        validateOnlyPrivateSchemaAccess(database, principal, false);
    }

    public void validateOnlyAccess(Database database, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, UserNotFoundException, AccessNotFoundException {
        if (principal == null) {
            throw new NotAllowedException("No principal provided");
        }
        if (isSystem(principal)) {
            return;
        }
        final DatabaseAccess access = accessService.find(database, userService.findById(getId(principal)));
        log.trace("found access: {}", access);
        if (writeAccessOnly && !(access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.error("Access not allowed: no write access");
            throw new NotAllowedException("Access not allowed: no write access");
        }
    }

    public void validateColumnCreateConstraints(CreateTableDto data) throws MalformedException {
        if (data == null) {
            throw new MalformedException("Validation failed: table data is null");
        }
        /* check size */
        final Optional<CreateTableColumnDto> optional0 = data.getColumns()
                .stream()
                .filter(c -> Objects.isNull(c.getSize()))
                .filter(c -> NEED_SIZE.contains(c.getType()))
                .findFirst();
        if (optional0.isPresent()) {
            log.error("Validation failed: column {} need size parameter", optional0.get().getName());
            throw new MalformedException("Validation failed: column " + optional0.get().getName() + " need size parameter");
        }
        final Optional<CreateTableColumnDto> optional0a = data.getColumns()
                .stream()
                .filter(c -> !Objects.isNull(c.getSize()))
                .filter(c -> CAN_HAVE_SIZE.contains(c.getType()) || CAN_HAVE_SIZE_AND_D.contains(c.getType()))
                .filter(c -> c.getSize() < 0)
                .findFirst();
        if (optional0a.isPresent()) {
            log.error("Validation failed: column {} needs positive size parameter", optional0a.get().getName());
            throw new MalformedException("Validation failed: column " + optional0a.get().getName() + " needs positive size parameter");
        }
        final Optional<CreateTableColumnDto> optional0b = data.getColumns()
                .stream()
                .filter(c -> !Objects.isNull(c.getD()))
                .filter(c -> CAN_HAVE_SIZE_AND_D.contains(c.getType()))
                .filter(c -> c.getD() < 0)
                .findFirst();
        if (optional0b.isPresent()) {
            log.error("Validation failed: column {} needs positive d parameter", optional0b.get().getName());
            throw new MalformedException("Validation failed: column " + optional0b.get().getName() + " needs positive d parameter");
        }
        /* check size and d */
        final Optional<CreateTableColumnDto> optional1 = data.getColumns()
                .stream()
                .filter(c -> Objects.isNull(c.getSize()) ^ Objects.isNull(c.getD()))
                .filter(c -> CAN_HAVE_SIZE_AND_D.contains(c.getType()))
                .findFirst();
        if (optional1.isPresent()) {
            log.error("Validation failed: column {} either needs both size and d parameter or none (use default)", optional1.get().getName());
            throw new MalformedException("Validation failed: column " + optional1.get().getName() + " either needs both size and d parameter or none (use default)");
        }
        /* check enum */
        final Optional<CreateTableColumnDto> optional2 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.ENUM))
                .filter(c -> c.getEnums() == null || c.getEnums().isEmpty())
                .findFirst();
        if (optional2.isPresent()) {
            log.error("Validation failed: column {} needs at least 1 allowed enum value", optional2.get().getName());
            throw new MalformedException("Validation failed: column " + optional2.get().getName() + " needs at least 1 allowed enum value");
        }
        /* check set */
        final Optional<CreateTableColumnDto> optional3 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.SET))
                .filter(c -> c.getSets() == null || c.getSets().isEmpty())
                .findFirst();
        if (optional3.isPresent()) {
            log.error("Validation failed: column {} needs at least 1 allowed set value", optional3.get().getName());
            throw new MalformedException("Validation failed: column " + optional3.get().getName() + " needs at least 1 allowed set value");
        }
        /* check serial */
        final List<CreateTableColumnDto> list4a = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.SERIAL))
                .toList();
        if (list4a.size() > 1) {
            log.error("Validation failed: only one column of type serial allowed");
            throw new MalformedException("Validation failed: only one column of type serial allowed");
        }
        final Optional<CreateTableColumnDto> optional4a = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.SERIAL))
                .filter(CreateTableColumnDto::getNullAllowed)
                .findFirst();
        if (optional4a.isPresent()) {
            log.error("Validation failed: column {} type serial demands non-null", optional4a.get().getName());
            throw new MalformedException("Validation failed: column " + optional4a.get().getName() + " type serial demands non-null");
        }
    }

    public boolean validateOnlyMineOrWriteAccessOrHasRole(User owner, Principal principal, DatabaseAccess access, String role) {
        if (hasRole(principal, role)) {
            log.debug("validation passed: role {} present", role);
            return true;
        }
        if (access == null) {
            /* should never happen */
            log.error("validation failed: access is null");
            return false;
        }
        if (owner.getUsername().equals(principal.getName()) && (access.getType().equals(AccessType.WRITE_ALL) || access.getType().equals(AccessType.WRITE_OWN))) {
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

    @Transactional(readOnly = true)
    public void validateOnlyOwnerOrWriteAll(Table table, User user) throws NotAllowedException,
            AccessNotFoundException {
        log.trace("table owner: {}", table.getOwnedBy());
        final DatabaseAccess access = accessService.find(table.getDatabase(), user);
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.READ)) {
            log.error("Access not allowed: insufficient access (only read-access)");
            throw new NotAllowedException("Access not allowed: insufficient access (only read-access)");
        }
        if (table.getOwnedBy().equals(user.getId()) && (access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.trace("grant access: table owner with write access");
            return;
        }
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.trace("grant access: write-all access");
            return;
        }
        log.error("Access not allowed: insufficient access (neither owner {} nor write-all access)", table.getOwnedBy());
        throw new NotAllowedException("Access not allowed: insufficient access (neither owner nor write-all access)");
    }

    public void validateOnlyPrivateDataHasRole(Database database, Principal principal, String role)
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
        if (!hasRole(principal, role)) {
            log.error("Access not allowed: role {} missing", role);
            throw new NotAllowedException("Access not allowed: role " + role + " missing");
        }
        log.trace("principal has role '{}': access granted", role);
    }

    public void validateOnlyPrivateSchemaHasRole(Database database, Principal principal, String role)
            throws NotAllowedException {
        if (database.getIsSchemaPublic()) {
            log.trace("database with id {} has public schema: no access needed", database.getId());
            return;
        }
        log.trace("database with id {} has private schema", database.getId());
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal: {}", principal.getName());
        if (!hasRole(principal, role)) {
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
