package at.tuwien.endpoints;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.EntityService;
import at.tuwien.service.TableService;
import at.tuwien.service.UserService;
import at.tuwien.utils.UserUtil;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
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
import org.springframework.http.HttpHeaders;
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
@RequestMapping(path = "/api/database/{databaseId}/table")
public class TableEndpoint {

    private final UserService userService;
    private final TableService tableService;
    private final EntityService entityService;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public TableEndpoint(UserService userService, TableService tableService, EntityService entityService,
                         MetadataMapper metadataMapper, DatabaseService databaseService,
                         EndpointValidator endpointValidator) {
        this.userService = userService;
        this.tableService = tableService;
        this.entityService = entityService;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.endpointValidator = endpointValidator;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_tables_findall")
    @Operation(summary = "List tables",
            description = "Lists all tables known to the metadata database.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List tables",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableBriefDto.class)))}),
            @ApiResponse(responseCode = "403",
                    description = "List tables not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableBriefDto>> list(@NotNull @PathVariable("databaseId") Long databaseId,
                                                    Principal principal) throws NotAllowedException,
            DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException {
        log.debug("endpoint list tables, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        endpointValidator.validateOnlyPrivateAccess(database, principal);
        endpointValidator.validateOnlyPrivateHasRole(database, principal, "list-tables");
        final List<TableBriefDto> dto = database.getTables()
                .stream()
                .map(metadataMapper::tableToTableBriefDto)
                .collect(Collectors.toList());
        log.trace("list tables resulted in tables {}", dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{tableId}/suggest")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Observed(name = "dbrepo_semantic_table_analyse")
    @Operation(summary = "Suggest semantics",
            description = "Suggests semantic concepts for a table. Requires role `table-semantic-analyse`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Suggested table semantics successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = EntityDto.class)))}),
            @ApiResponse(responseCode = "400",
                    description = "Failed to parse statistic in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Generated query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<EntityDto>> analyseTable(@NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException {
        log.debug("endpoint analyse table semantics, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = tableService.findById(databaseId, tableId);
        final List<EntityDto> dtos = entityService.suggestByTable(table);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @PutMapping("/{tableId}")
    @Transactional
    @PreAuthorize("hasAuthority('update-table-statistic')")
    @Observed(name = "dbrepo_statistic_table_update")
    @Operation(summary = "Update statistics",
            description = "Updates basic statistical properties (min, max, mean, median, std.dev) for numerical columns in a table with id. Requires role `update-table-statistic`",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated table statistics successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Failed to map column statistic to known columns",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> updateStatistic(@NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @PathVariable("tableId") Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException, DataServiceException, DataServiceConnectionException {
        log.debug("endpoint update table statistics, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = tableService.findById(databaseId, tableId);
        tableService.updateStatistics(table);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{tableId}/column/{columnId}")
    @Transactional
    @PreAuthorize("hasAuthority('modify-table-column-semantics') or hasAuthority('modify-foreign-table-column-semantics')")
    @Observed(name = "dbrepo_semantics_column_save")
    @Operation(summary = "Update semantics",
            description = "Updates column semantics of a table column with id. Only the table owner with at least *READ* access to the associated database can update the column semantics (requires role `modify-table-column-semantics`) or foreign table columns if role `modify-foreign-table-column-semantics`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated column semantics successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ColumnDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Update semantic concept query is malformed or update unit of measurement query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find user/table/database/ontology in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ColumnDto> update(@NotNull @PathVariable("databaseId") Long databaseId,
                                            @NotNull @PathVariable("tableId") Long tableId,
                                            @NotNull @PathVariable("columnId") Long columnId,
                                            @NotNull @Valid @RequestBody ColumnSemanticsUpdateDto updateDto,
                                            @NotNull Principal principal) throws NotAllowedException,
            MalformedException, DataServiceException, DataServiceConnectionException, UserNotFoundException,
            TableNotFoundException, DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        log.debug("endpoint update table, databaseId={}, tableId={}, columnId={}", databaseId, tableId, columnId);
        final User user = userService.findByUsername(principal.getName());
        final Table table = tableService.findById(databaseId, tableId);
        if (!UserUtil.hasRole(principal, "modify-foreign-table-column-semantics")) {
            endpointValidator.validateOnlyAccess(table.getDatabase(), principal, true);
            endpointValidator.validateOnlyOwnerOrWriteAll(table, user);
        }
        TableColumn column = tableService.findColumnById(table, columnId);
        column = tableService.update(column, updateDto);
        log.info("Updated table semantics of table with id {}", tableId);
        final ColumnDto columnDto = metadataMapper.tableColumnToColumnDto(column);
        log.trace("find table data resulted in column {}", columnDto);
        return ResponseEntity.accepted()
                .body(columnDto);
    }

    @GetMapping("/{tableId}/column/{columnId}/suggest")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Observed(name = "dbrepo_semantic_column_analyse")
    @Operation(summary = "Suggest semantics",
            description = "Suggests column semantics. Requires role `table-semantic-analyse`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Suggested table column semantics successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableColumnEntityDto.class)))}),
            @ApiResponse(responseCode = "400",
                    description = "Generated query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableColumnEntityDto>> analyseTableColumn(@NotNull @PathVariable("databaseId") Long databaseId,
                                                                         @NotNull @PathVariable("tableId") Long tableId,
                                                                         @NotNull @PathVariable("columnId") Long columnId)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException {
        log.debug("endpoint analyse table column semantics, databaseId={}, tableId={}, columnId={}", databaseId, tableId, columnId);
        final Table table = tableService.findById(databaseId, tableId);
        TableColumn column = tableService.findColumnById(table, columnId);
        final List<TableColumnEntityDto> dtos = entityService.suggestByColumn(column);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @PostMapping
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("hasAuthority('create-table')")
    @Observed(name = "dbrepo_table_create")
    @Operation(summary = "Create table",
            description = "Creates a table in the database with id. Requires role `create-table`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
            @ApiResponse(responseCode = "403",
                    description = "Create table not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Create table conflicts with existing table name",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TableDto> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                           @NotNull @Valid @RequestBody TableCreateDto data,
                                           @NotNull Principal principal) throws NotAllowedException, MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, TableNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        log.debug("endpoint create table, databaseId={}, data.name={}", databaseId, data.getName());
        final Database database = databaseService.findById(databaseId);
        endpointValidator.validateOnlyAccess(database, principal, true);
        endpointValidator.validateColumnCreateConstraints(data);
        final Table table = tableService.createTable(database, data, principal);
        final TableDto dto = metadataMapper.customTableToTableDto(table);
        log.info("Created table with id {}", dto.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/{tableId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_tables_find")
    @Operation(summary = "Find table",
            description = "Finds a table with id.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
            @ApiResponse(responseCode = "502",
                    description = "Failed to establish connection with broker service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to obtain queue information from broker service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TableDto> findById(@NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId,
                                             Principal principal) throws DataServiceException,
            DataServiceConnectionException, TableNotFoundException, DatabaseNotFoundException, QueueNotFoundException {
        log.debug("endpoint find table, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = tableService.findById(databaseId, tableId);
        final TableDto dto = metadataMapper.customTableToTableDto(table);
        final HttpHeaders headers = new HttpHeaders();
        if (UserUtil.isSystem(principal)) {
            headers.set("X-Username", table.getDatabase().getContainer().getPrivilegedUsername());
            headers.set("X-Password", table.getDatabase().getContainer().getPrivilegedPassword());
            headers.set("X-Host", table.getDatabase().getContainer().getHost());
            headers.set("X-Port", "" + table.getDatabase().getContainer().getPort());
            headers.set("X-Type", table.getDatabase().getContainer().getImage().getJdbcMethod());
            headers.set("X-Database", table.getDatabase().getInternalName());
            headers.set("X-Sidecar-Host", table.getDatabase().getContainer().getSidecarHost());
            headers.set("X-Sidecar-Port", "" + table.getDatabase().getContainer().getSidecarPort());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password X-Host X-Port X-Type X-Database X-Sidecar-Host X-Sidecar-Port");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(dto);
    }

    @DeleteMapping("/{tableId}")
    @Transactional
    @PreAuthorize("hasAuthority('delete-table') or hasAuthority('delete-foreign-table')")
    @Observed(name = "dbrepo_table_delete")
    @Operation(summary = "Delete table",
            description = "Deletes a table with id. Only the owner of a table can perform this action (requires role `delete-table`) or anyone can delete a table (requires role `delete-foreign-table`).",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Delete table successfully"),
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
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, TableNotFoundException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint delete table, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = tableService.findById(databaseId, tableId);
        /* roles */
        if (!table.getOwner().equals(principal) && !UserUtil.hasRole(principal, "delete-foreign-table")) {
            log.error("Failed to delete table: not owned by current user");
            throw new NotAllowedException("Failed to delete table: not owned by current user");
        }
        /* check */
        if (!table.getIdentifiers().isEmpty()) {
            log.error("Failed to delete table: identifier already associated");
            throw new NotAllowedException("Failed to delete table: identifier already associated");
        }
        /* delete table */
        tableService.deleteTable(table);
        return ResponseEntity.accepted()
                .build();
    }

}
