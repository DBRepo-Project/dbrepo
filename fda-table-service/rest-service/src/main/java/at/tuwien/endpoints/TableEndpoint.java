package at.tuwien.endpoints;

import at.tuwien.api.database.table.*;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotAllowedException;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table")
public class TableEndpoint extends AbstractEndpoint {

    private final TableService tableService;
    private final MessageQueueService amqpService;
    private final TableMapper tableMapper;

    @Autowired
    public TableEndpoint(TableService tableService, DatabaseService databaseService, MessageQueueService amqpService,
                         TableMapper tableMapper) {
        super(tableService, databaseService);
        this.tableService = tableService;
        this.amqpService = amqpService;
        this.tableMapper = tableMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List all tables", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<TableBriefDto>> findAll(@NotNull @PathVariable("id") Long containerId,
                                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                                       Principal principal)
            throws DatabaseNotFoundException {
        if (!hasDatabasePermission(containerId, databaseId, "TABLES_VIEW", principal)) {
            log.error("Missing table view permission");
            throw new NotAllowedException("Missing table view permission");
        }
        return ResponseEntity.ok(tableService.findAll(containerId, databaseId, principal)
                .stream()
                .map(tableMapper::tableToTableBriefDto)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create a table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableBriefDto> create(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @Valid @RequestBody TableCreateDto createDto,
                                                Principal principal)
            throws ImageNotSupportedException, DatabaseNotFoundException, TableMalformedException, AmqpException,
            TableNameExistsException, ContainerNotFoundException, UserNotFoundException, QueryMalformedException {
        if (!hasDatabasePermission(containerId, databaseId, "TABLE_CREATE", principal)) {
            log.error("Missing table create permission");
            throw new NotAllowedException("Missing table create permission");
        }
        final Table table = tableService.createTable(containerId, databaseId, createDto, principal);
        amqpService.create(table);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tableMapper.tableToTableBriefDto(table));
    }


    @GetMapping("/{tableId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get information about table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableDto> findById(@NotNull @PathVariable("id") Long containerId,
                                             @NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId,
                                             Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ContainerNotFoundException {
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_INFO", principal)) {
            log.error("Missing table view permission");
            throw new NotAllowedException("Missing table view permission");
        }
        final Table table = tableService.findById(containerId, databaseId, tableId, principal);
        log.debug(table);
        TableDto tableDto = tableMapper.tableToTableDto(table);
        log.debug(tableDto);
        return ResponseEntity.ok(tableDto);
    }

    @PutMapping("/{tableId}")
    @Transactional
    @Operation(summary = "Update a table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableBriefDto> update(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @PathVariable("tableId") Long tableId,
                                                Principal principal) {
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_UPDATE", principal)) {
            log.error("Missing table update permission");
            throw new NotAllowedException("Missing table update permission");
        }
        return ResponseEntity.unprocessableEntity().body(new TableBriefDto());
    }

    @DeleteMapping("/{tableId}")
    @Transactional
    @Operation(summary = "Delete a table", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void delete(@NotNull @PathVariable("id") Long containerId,
                       @NotNull @PathVariable("databaseId") Long databaseId,
                       @NotNull @PathVariable("tableId") Long tableId,
                       Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DataProcessingException, ContainerNotFoundException, TableMalformedException, QueryMalformedException {
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_DELETE", principal)) {
            log.error("Missing table delete permission");
            throw new NotAllowedException("Missing table delete permission");
        }
        tableService.deleteTable(containerId, databaseId, tableId, principal);
    }

}
