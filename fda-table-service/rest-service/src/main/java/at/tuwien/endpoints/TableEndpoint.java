package at.tuwien.endpoints;

import at.tuwien.api.database.table.*;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.TableService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table")
public class TableEndpoint extends AbstractEndpoint {

    private final TableMapper tableMapper;
    private final TableService tableService;
    private final MessageQueueService amqpService;

    @Autowired
    public TableEndpoint(TableMapper tableMapper, TableService tableService, MessageQueueService amqpService,
                         DatabaseService databaseService, AccessService accessService) {
        super(accessService, databaseService);
        this.tableMapper = tableMapper;
        this.amqpService = amqpService;
        this.tableService = tableService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "table.list", description = "Time needed to list the tables")
    @Operation(summary = "List all tables", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<TableBriefDto>> list(@NotNull @PathVariable("id") Long containerId,
                                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                                       Principal principal)
            throws DatabaseNotFoundException, NotAllowedException {
        log.debug("endpoint list tables, containerId={}, databaseId={}, principal={}", containerId, databaseId,
                principal);
        if (!hasDatabasePermission(containerId, databaseId, "TABLES_VIEW", principal)) {
            log.error("Missing table view permission");
            throw new NotAllowedException("Missing table view permission");
        }
        final List<TableBriefDto> dto = tableService.findAll(containerId, databaseId)
                .stream()
                .map(tableMapper::tableToTableBriefDto)
                .collect(Collectors.toList());
        log.trace("list tables resulted in tables {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Transactional
    @Timed(value = "table.create", description = "Time needed to create a table")
    @Operation(summary = "Create a table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableBriefDto> create(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @Valid @RequestBody TableCreateDto createDto,
                                                @NotNull Principal principal)
            throws ImageNotSupportedException, DatabaseNotFoundException, TableMalformedException, AmqpException,
            TableNameExistsException, ContainerNotFoundException, UserNotFoundException, QueryMalformedException,
            NotAllowedException {
        log.debug("endpoint create table, containerId={}, databaseId={}, createDto={}, principal={}", containerId,
                databaseId, createDto, principal);
        if (!hasDatabasePermission(containerId, databaseId, "TABLE_CREATE", principal)) {
            log.error("Missing table create permission");
            throw new NotAllowedException("Missing table create permission");
        }
        final Table table = tableService.createTable(containerId, databaseId, createDto, principal);
        amqpService.create(table);
        final TableBriefDto dto = tableMapper.tableToTableBriefDto(table);
        log.trace("create table resulted in table {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }


    @GetMapping("/{tableId}")
    @Transactional(readOnly = true)
    @Timed(value = "table.find", description = "Time needed to find a table")
    @Operation(summary = "Get information about table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableDto> findById(@NotNull @PathVariable("id") Long containerId,
                                             @NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId,
                                             Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ContainerNotFoundException, NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint find table, containerId={}, databaseId={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_INFO", principal)) {
            log.error("Missing table view permission");
            throw new NotAllowedException("Missing table view permission");
        }
        final Table table = tableService.findById(containerId, databaseId, tableId);
        final TableDto dto = tableMapper.tableToTableDto(table);
        log.trace("find table resulted in table {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{tableId}")
    @Transactional
    @Timed(value = "table.update", description = "Time needed to update a table")
    @Operation(summary = "Update a table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableBriefDto> update(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @PathVariable("tableId") Long tableId,
                                                @NotNull Principal principal) throws NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint update table, containerId={}, databaseId={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_UPDATE", principal)) {
            log.error("Missing table update permission");
            throw new NotAllowedException("Missing table update permission");
        }
        log.trace("update table resulted in table {}", "");
        return ResponseEntity.unprocessableEntity()
                .build();
    }

    @DeleteMapping("/{tableId}")
    @Transactional
    @Timed(value = "table.delete", description = "Time needed to delete a table")
    @Operation(summary = "Delete a table", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void delete(@NotNull @PathVariable("id") Long containerId,
                       @NotNull @PathVariable("databaseId") Long databaseId,
                       @NotNull @PathVariable("tableId") Long tableId,
                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DataProcessingException, ContainerNotFoundException, TableMalformedException, QueryMalformedException,
            NotAllowedException, AccessDeniedException {
        log.debug("endpoint delete table, containerId={}, databaseId={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_DELETE", principal)) {
            log.error("Missing table delete permission");
            throw new NotAllowedException("Missing table delete permission");
        }
        tableService.deleteTable(containerId, databaseId, tableId);
    }

}
