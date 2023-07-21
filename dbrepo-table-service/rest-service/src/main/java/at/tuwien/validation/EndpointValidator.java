package at.tuwien.validation;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
@Component
public class EndpointValidator {

    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final TableService tableService;

    @Autowired
    public EndpointValidator(AccessService accessService, DatabaseService databaseService, TableService tableService) {
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.tableService = tableService;
    }

    public void validateOnlyPrivateAccess(Long databaseId, Principal principal, boolean writeAccessOnly) throws NotAllowedException, DatabaseNotFoundException {
        final Database database = databaseService.find(databaseId);
        if (database.getIsPublic()) {
            log.trace("database with id {} is public: no access needed", databaseId);
            return;
        }
        validateOnlyAccess(databaseId, principal, writeAccessOnly);
    }

    public void validateOnlyPrivateAccess(Long databaseId, Principal principal) throws NotAllowedException, DatabaseNotFoundException {
        validateOnlyPrivateAccess(databaseId, principal, false);
    }

    public void validateOnlyAccess(Long databaseId, Principal principal, boolean writeAccessOnly) throws NotAllowedException, DatabaseNotFoundException {
        if (principal == null) {
            log.error("Access not allowed: database with id {} is not public and no authorization provided", databaseId);
            throw new NotAllowedException("Access not allowed: database with id " + databaseId + " is not public and no authorization provided");
        }
        databaseService.find(databaseId);
        log.trace("principal: {}", principal.getName());
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
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
        final List<ColumnTypeDto> needSizeAndD = List.of(ColumnTypeDto.FLOAT, ColumnTypeDto.DOUBLE, ColumnTypeDto.DECIMAL);
        final List<ColumnTypeDto> needDateFormat = List.of(ColumnTypeDto.DATETIME, ColumnTypeDto.TIMESTAMP, ColumnTypeDto.TIME);
        /* check size */
        final Optional<ColumnCreateDto> optional0 = data.getColumns()
                .stream()
                .filter(c -> Objects.isNull(c.getSize()))
                .filter(c -> needSize.contains(c.getType()))
                .findFirst();
        if (optional0.isPresent()) {
            throw new TableMalformedException("Validation failed: column " + optional0.get().getName() + " needs size parameter");
        }
        /* check size and d */
        final Optional<ColumnCreateDto> optional1 = data.getColumns()
                .stream()
                .filter(c -> Objects.isNull(c.getSize()) || Objects.isNull(c.getD()))
                .filter(c -> needSizeAndD.contains(c.getType()))
                .findFirst();
        if (optional1.isPresent()) {
            throw new TableMalformedException("Validation failed: column " + optional1.get().getName() + " needs size and d parameter");
        }
        /* check enum */
        final Optional<ColumnCreateDto> optional2 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.ENUM))
                .filter(c -> c.getEnums() == null || c.getEnums().size() == 0)
                .findFirst();
        if (optional2.isPresent()) {
            throw new TableMalformedException("Validation failed: column " + optional2.get().getName() + " needs at least 1 allowed enum value");
        }
        /* check set */
        final Optional<ColumnCreateDto> optional3 = data.getColumns()
                .stream()
                .filter(c -> c.getType().equals(ColumnTypeDto.SET))
                .filter(c -> c.getEnums() == null || c.getSets().size() == 0)
                .findFirst();
        if (optional3.isPresent()) {
            throw new TableMalformedException("Validation failed: column " + optional3.get().getName() + " needs at least 1 allowed set value");
        }
        /* check date */
        final Optional<ColumnCreateDto> optional4 = data.getColumns()
                .stream()
                .filter(c -> needDateFormat.contains(c.getType()))
                .filter(c -> Objects.isNull(c.getDfid()))
                .findFirst();
        if (optional4.isPresent()) {
            throw new TableMalformedException("Validation failed: column " + optional4.get().getName() + " needs a format");
        }
    }

    public void validateOnlyOwnerOrWriteAll(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, NotAllowedException, TableNotFoundException, ContainerNotFoundException {
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        final Table table = tableService.findById(databaseId, tableId);
        log.trace("principal: {}", principal.getName());
        log.trace("table creator: {}", table.getCreator().getUsername());
        final DatabaseAccess access = accessService.find(databaseId, principal.getName());
        log.trace("found access {}", access);
        if (access.getType().equals(AccessType.READ)) {
            log.error("Access not allowed: insufficient access (only read-access)");
            throw new NotAllowedException("Access not allowed: insufficient access (only read-access)");
        }
        if (table.getCreator().equalsPrincipal(principal) && (access.getType().equals(AccessType.WRITE_OWN) || access.getType().equals(AccessType.WRITE_ALL))) {
            log.trace("grant access: table creator with write access");
            return;
        }
        if (access.getType().equals(AccessType.WRITE_ALL)) {
            log.trace("grant access: write-all access");
            return;
        }
        log.error("Access not allowed: insufficient access (neither creator {} nor write-all access)", table.getCreator().getUsername());
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
        if (!User.hasRole(principal, role)) {
            log.error("Access not allowed: role {} missing", role);
            throw new NotAllowedException("Access not allowed: role " + role + " missing");
        }
        log.trace("principal has role '{}': access granted", role);
    }
}
