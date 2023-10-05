package at.tuwien.endpoints;

import at.tuwien.api.amqp.QueueDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.TableService;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/database/{databaseId}/table")
public class TableEndpoint {

    private final TableMapper tableMapper;
    private final TableService tableService;
    private final RabbitMqConfig rabbitMqConfig;
    private final EndpointValidator endpointValidator;
    private final MessageQueueService messageQueueService;

    @Autowired
    public TableEndpoint(TableMapper tableMapper, TableService tableService, RabbitMqConfig rabbitMqConfig,
                         EndpointValidator endpointValidator, MessageQueueService messageQueueService) {
        this.tableMapper = tableMapper;
        this.tableService = tableService;
        this.rabbitMqConfig = rabbitMqConfig;
        this.endpointValidator = endpointValidator;
        this.messageQueueService = messageQueueService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "table.list", description = "Time needed to list the tables")
    @Operation(summary = "List all tables", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List tables",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableBriefDto.class)))}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "List tables not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableBriefDto>> list(@NotNull @PathVariable("databaseId") Long databaseId,
                                                    Principal principal)
            throws DatabaseNotFoundException, NotAllowedException, AccessDeniedException {
        log.debug("endpoint list tables, databaseId={}, principal={}", databaseId, principal);
        endpointValidator.validateOnlyPrivateAccess(databaseId, principal);
        endpointValidator.validateOnlyPrivateHasRole(databaseId, principal, "list-tables");
        final List<TableBriefDto> dto = tableService.findAll(databaseId)
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Create table query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Create table not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Create table conflicts with existing table name",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Broker service failed to create queue",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TableBriefDto> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @Valid @RequestBody TableCreateDto createDto,
                                                @NotNull Principal principal)
            throws ImageNotSupportedException, DatabaseNotFoundException, TableMalformedException, AmqpException,
            TableNameExistsException, ContainerNotFoundException, UserNotFoundException, QueryMalformedException,
            NotAllowedException, AccessDeniedException {
        log.debug("endpoint create table, databaseId={}, createDto={}, principal={}", databaseId, createDto, principal);
        endpointValidator.validateOnlyAccess(databaseId, principal, true);
        endpointValidator.validateColumnCreateConstraints(createDto);
        final Table table = tableService.createTable(databaseId, createDto, principal);
        final TableBriefDto dto = tableMapper.tableToTableBriefDto(table);
        log.trace("create table resulted in table {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }


    @GetMapping("/{tableId}")
    @Transactional(readOnly = true)
    @Timed(value = "table.find", description = "Time needed to find a table")
    @Operation(summary = "Get information about table", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find table successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database or container could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find table not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TableDto> findById(@NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId,
                                             Principal principal) throws TableNotFoundException,
            DatabaseNotFoundException, QueueNotFoundException, BrokerRemoteException {
        log.debug("endpoint find table, databaseId={}, tableId={}, principal={}", databaseId, tableId, principal);
        final Table table = tableService.findById(databaseId, tableId);
        final TableDto dto = tableMapper.tableToTableDto(table);
        if (principal != null) {
            /* extra effort only when logged-in */
            final QueueDto queue = messageQueueService.findQueue(rabbitMqConfig.getQueueName());
            dto.setQueueType(queue.getType());
        }
        log.trace("find table resulted in table {}", dto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{tableId}")
    @Transactional
    @PreAuthorize("hasAuthority('delete-table')")
    @Timed(value = "table.delete", description = "Time needed to delete a table")
    @Operation(summary = "Delete a table", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Delete table successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Delete table query resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database or container could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Delete table not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Delete table resulted in an invalid state",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DataProcessingException, ContainerNotFoundException, TableMalformedException, QueryMalformedException {
        log.debug("endpoint delete table, databaseId={}, tableId={}, principal={}", databaseId, tableId, principal);
        tableService.deleteTable(databaseId, tableId);
        return ResponseEntity.accepted()
                .build();
    }

}
