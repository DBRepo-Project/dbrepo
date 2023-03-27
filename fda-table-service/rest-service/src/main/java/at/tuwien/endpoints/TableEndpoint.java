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
import org.springframework.security.access.prepost.PreAuthorize;
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
public class TableEndpoint {

    private final TableMapper tableMapper;
    private final TableService tableService;
    private final MessageQueueService amqpService;

    @Autowired
    public TableEndpoint(TableMapper tableMapper, TableService tableService, MessageQueueService amqpService) {
        this.tableMapper = tableMapper;
        this.amqpService = amqpService;
        this.tableService = tableService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "table.list", description = "Time needed to list the tables")
    @PreAuthorize("hasAuthority('find-tables')")
    @Operation(summary = "List all tables", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<TableBriefDto>> list(@NotNull @PathVariable("id") Long containerId,
                                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                                    Principal principal)
            throws DatabaseNotFoundException {
        log.debug("endpoint list tables, containerId={}, databaseId={}, principal={}", containerId, databaseId,
                principal);
        final List<TableBriefDto> dto = tableService.findAll(containerId, databaseId)
                .stream()
                .map(tableMapper::tableToTableBriefDto)
                .collect(Collectors.toList());
        log.trace("list tables resulted in tables {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('create-table')")
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
    @PreAuthorize("hasAuthority('find-table')")
    @Operation(summary = "Get information about table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableDto> findById(@NotNull @PathVariable("id") Long containerId,
                                             @NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId,
                                             Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ContainerNotFoundException {
        log.debug("endpoint find table, containerId={}, databaseId={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        final Table table = tableService.findById(containerId, databaseId, tableId);
        final TableDto dto = tableMapper.tableToTableDto(table);
        log.trace("find table resulted in table {}", dto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{tableId}")
    @Transactional
    @PreAuthorize("hasAuthority('delete-table')")
    @Timed(value = "table.delete", description = "Time needed to delete a table")
    @Operation(summary = "Delete a table", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void delete(@NotNull @PathVariable("id") Long containerId,
                       @NotNull @PathVariable("databaseId") Long databaseId,
                       @NotNull @PathVariable("tableId") Long tableId,
                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DataProcessingException, ContainerNotFoundException, TableMalformedException, QueryMalformedException {
        log.debug("endpoint delete table, containerId={}, databaseId={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        tableService.deleteTable(containerId, databaseId, tableId);
    }

}
