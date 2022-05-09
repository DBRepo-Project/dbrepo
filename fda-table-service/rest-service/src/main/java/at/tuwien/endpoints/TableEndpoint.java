package at.tuwien.endpoints;

import at.tuwien.api.database.table.*;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
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
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table")
public class TableEndpoint {

    private final TableService tableService;
    private final MessageQueueService amqpService;
    private final TableMapper tableMapper;

    @Autowired
    public TableEndpoint(TableService tableService, MessageQueueService amqpService, TableMapper tableMapper) {
        this.tableService = tableService;
        this.amqpService = amqpService;
        this.tableMapper = tableMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List all tables")
    public ResponseEntity<List<TableBriefDto>> findAll(@NotNull @PathVariable("id") Long id,
                                                       @NotNull @PathVariable("databaseId") Long databaseId)
            throws DatabaseNotFoundException {
        return ResponseEntity.ok(tableService.findAll(id, databaseId)
                .stream()
                .map(tableMapper::tableToTableBriefDto)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Create a table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableBriefDto> create(@NotNull @PathVariable("id") Long id,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @Valid @RequestBody TableCreateDto createDto)
            throws ImageNotSupportedException, DatabaseNotFoundException, DataProcessingException,
            ArbitraryPrimaryKeysException, TableMalformedException, AmqpException, TableNameExistsException,
            ContainerNotFoundException {
        final Table table = tableService.createTable(id, databaseId, createDto);
        amqpService.create(table);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tableMapper.tableToTableBriefDto(table));
    }


    @GetMapping("/{tableId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get information about table")
    public ResponseEntity<TableDto> findById(@NotNull @PathVariable("id") Long id,
                                             @NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException, ContainerNotFoundException {
        final Table table = tableService.findById(id, databaseId, tableId);
        log.debug(table);
        TableDto tableDto = tableMapper.tableToTableDto(table);
        log.debug(tableDto);
        return ResponseEntity.ok(tableDto);
    }

    @PutMapping("/{tableId}")
    @Transactional
    @Operation(summary = "Update a table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TableBriefDto> update(@NotNull @PathVariable("id") Long id,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @PathVariable("tableId") Long tableId) {
        // TODO
        return ResponseEntity.unprocessableEntity().body(new TableBriefDto());
    }

    @DeleteMapping("/{tableId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Delete a table", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void delete(@NotNull @PathVariable("id") Long id,
                       @NotNull @PathVariable("databaseId") Long databaseId,
                       @NotNull @PathVariable("tableId") Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DataProcessingException, ContainerNotFoundException {
        tableService.deleteTable(id, databaseId, tableId);
    }

}
