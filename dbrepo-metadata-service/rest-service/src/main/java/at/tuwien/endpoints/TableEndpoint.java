package at.tuwien.endpoints;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.CreateTableDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.TableUpdateDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.*;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
public class TableEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final TableService tableService;
    private final EntityService entityService;
    private final AccessService accessService;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public TableEndpoint(UserService userService, TableService tableService, EntityService entityService,
                         AccessService accessService, MetadataMapper metadataMapper, DatabaseService databaseService,
                         EndpointValidator endpointValidator) {
        this.userService = userService;
        this.tableService = tableService;
        this.entityService = entityService;
        this.accessService = accessService;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.endpointValidator = endpointValidator;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_tables_findall")
    @Operation(summary = "List tables",
            description = "Lists all tables known to the metadata database. When a database has a hidden schema (i.e. when `is_schema_public` is `false`), then the user needs to have at least read access and the role `list-tables`.",
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
        endpointValidator.validateOnlyPrivateSchemaAccess(database, principal);
        endpointValidator.validateOnlyPrivateSchemaHasRole(database, principal, "list-tables");
        return ResponseEntity.ok(database.getTables()
                .stream()
                .filter(t -> t.getIsPublic() || t.getIsSchemaPublic())
                .map(metadataMapper::tableToTableBriefDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{tableId}/suggest")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Observed(name = "dbrepo_semantic_table_analyse")
    @Operation(summary = "Suggest semantics",
            description = "Suggests semantic concepts for a table. This action can only be performed by the table owner. Requires role `table-semantic-analyse`.",
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
            @ApiResponse(responseCode = "403",
                    description = "Not the table owner.",
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
                                                        @NotNull @PathVariable("tableId") Long tableId,
                                                        @NotNull Principal principal)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException, NotAllowedException {
        log.debug("endpoint analyse table semantics, databaseId={}, tableId={}, principal.name={}", databaseId, tableId,
                principal);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!table.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to analyse table semantics: not owner");
            throw new NotAllowedException("Failed to analyse table semantics: not owner");
        }
        return ResponseEntity.ok()
                .body(entityService.suggestByTable(table));
    }

    @PutMapping("/{tableId}/statistic")
    @Transactional
    @PreAuthorize("hasAuthority('update-table-statistic')")
    @Observed(name = "dbrepo_statistic_table_update")
    @Operation(summary = "Update statistics",
            description = "Updates basic statistical properties (min, max, mean, median, std.dev) for numerical columns in a table with id. This action can only be performed by the table owner. Requires role `update-table-statistic`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated table statistics successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Failed to map column statistic to known columns",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not the owner",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/table in metadata database",
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
                                                @NotNull @PathVariable("tableId") Long tableId,
                                                @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, SearchServiceException, NotAllowedException,
            SearchServiceConnectionException, MalformedException, DataServiceException, DataServiceConnectionException {
        log.debug("endpoint update table statistics, databaseId={}, tableId={}, principal.name={}", databaseId, tableId,
                principal.getName());
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!table.getOwner().getId().equals(getId(principal)) && !isSystem(principal)) {
            log.error("Failed to update table statistics: not owner");
            throw new NotAllowedException("Failed to update table statistics: not owner");
        }
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
    public ResponseEntity<ColumnDto> updateColumn(@NotNull @PathVariable("databaseId") Long databaseId,
                                                  @NotNull @PathVariable("tableId") Long tableId,
                                                  @NotNull @PathVariable("columnId") Long columnId,
                                                  @NotNull @Valid @RequestBody ColumnSemanticsUpdateDto updateDto,
                                                  @NotNull Principal principal) throws NotAllowedException,
            MalformedException, DataServiceException, DataServiceConnectionException, UserNotFoundException,
            TableNotFoundException, DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        log.debug("endpoint update table, databaseId={}, tableId={}, columnId={}, principal.name={}", databaseId,
                tableId, columnId, principal.getName());
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!hasRole(principal, "modify-foreign-table-column-semantics")) {
            endpointValidator.validateOnlyAccess(database, principal, true);
            endpointValidator.validateOnlyOwnerOrWriteAll(table, userService.findById(getId(principal)));
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.tableColumnToColumnDto(tableService.update(
                        tableService.findColumnById(table, columnId), updateDto)));
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
                                                                         @NotNull @PathVariable("columnId") Long columnId,
                                                                         @NotNull Principal principal)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException {
        log.debug("endpoint analyse table column semantics, databaseId={}, tableId={}, columnId={}, principal.name={}",
                databaseId, tableId, columnId, principal.getName());
        return ResponseEntity.ok()
                .body(entityService.suggestByColumn(
                        tableService.findColumnById(
                                tableService.findById(databaseService.findById(databaseId), tableId), columnId)));
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
    public ResponseEntity<TableBriefDto> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @Valid @RequestBody CreateTableDto data,
                                                @NotNull Principal principal) throws NotAllowedException, MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, TableNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        log.debug("endpoint create table, databaseId={}, data.name={}, principal.name={}", databaseId, data.getName(),
                principal.getName());
        final Database database = databaseService.findById(databaseId);
        endpointValidator.validateOnlyAccess(database, principal, true);
        endpointValidator.validateColumnCreateConstraints(data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.tableToTableBriefDto(
                        tableService.createTable(database, data, principal)));
    }

    @PutMapping("/{tableId}")
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("hasAuthority('update-table')")
    @Observed(name = "dbrepo_table_update")
    @Operation(summary = "Update table",
            description = "Updates a table in the database with id. Requires role `update-table`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated the table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Update table visibility payload is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Update table visibility not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table could not be found",
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
    public ResponseEntity<TableBriefDto> update(@NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @PathVariable("tableId") Long tableId,
                                                @NotNull @Valid @RequestBody TableUpdateDto data,
                                                @NotNull Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint update table, databaseId={}, data.is_public={}, data.is_schema_public={}, principal.name={}",
                databaseId, data.getIsPublic(), data.getIsSchemaPublic(), principal.getName());
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!table.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to update table: not owner");
            throw new NotAllowedException("Failed to update table: not owner");
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.tableToTableBriefDto(
                        tableService.updateTable(table, data)));
    }

    @GetMapping("/{tableId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_tables_find")
    @Operation(summary = "Find table",
            description = "Finds a table with id. When a table is hidden (i.e. when `is_public` is `false`), then the user needs to have at least read access and the role `find-table`. When the `system` role is present, the endpoint responds with additional connection metadata in the header.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find table successfully",
                    headers = {@Header(name = "X-Username", description = "The authentication username", schema = @Schema(implementation = String.class)),
                            @Header(name = "X-Password", description = "The authentication password", schema = @Schema(implementation = String.class)),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose custom headers", schema = @Schema(implementation = String.class))},
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
            DataServiceConnectionException, TableNotFoundException, DatabaseNotFoundException, QueueNotFoundException,
            UserNotFoundException, NotAllowedException, AccessNotFoundException {
        log.debug("endpoint find table, databaseId={}, tableId={}", databaseId, tableId);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        boolean isOwner = false;
        if (principal != null) {
            isOwner = table.getOwner().getId().equals(getId(principal));
            if (!table.getIsSchemaPublic()) {
                try {
                    accessService.find(table.getDatabase(), userService.findById(getId(principal)));
                } catch (UserNotFoundException | AccessNotFoundException e) {
                    if (!isOwner && !isSystem(principal)) {
                        log.error("Failed to find table with id {}: private and no access permission", table);
                        throw new NotAllowedException("Failed to find table with id " + tableId + ": private and no access permission");
                    }
                }
            }
        }
        if (!table.getIsSchemaPublic() && !isOwner && !isSystem(principal)) {
            log.debug("remove schema from table: {}.{}", database.getInternalName(), table.getInternalName());
            table.setColumns(List.of());
            table.setConstraints(null);
        }
        final TableDto dto = metadataMapper.tableToTableDto(table);
        final HttpHeaders headers = new HttpHeaders();
        if (isSystem(principal)) {
            headers.set("X-Username", table.getDatabase().getContainer().getPrivilegedUsername());
            headers.set("X-Password", table.getDatabase().getContainer().getPrivilegedPassword());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password");
        } else {
            removeInternalData(dto.getDatabase().getContainer());
        }
        return ResponseEntity.ok()
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
        log.debug("endpoint delete table, databaseId={}, tableId={}, principal.name={}", databaseId, tableId,
                principal.getName());
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        /* roles */
        if (!table.getOwner().getId().equals(getId(principal)) && !hasRole(principal, "delete-foreign-table")) {
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
