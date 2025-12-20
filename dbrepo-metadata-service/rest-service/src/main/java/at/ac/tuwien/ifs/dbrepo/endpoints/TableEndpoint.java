package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/v1/database/{databaseId}/table")
public class TableEndpoint extends RestEndpoint {

    private final TableService tableService;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final DashboardService dashboardService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public TableEndpoint(TableService tableService, MetadataMapper metadataMapper, DatabaseService databaseService,
                         DashboardService dashboardService, EndpointValidator endpointValidator) {
        this.tableService = tableService;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.dashboardService = dashboardService;
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
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content}),
    })
    public ResponseEntity<List<TableBriefDto>> list(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                    Principal principal) throws NotAllowedException,
            DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException {
        log.debug("endpoint list tables, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        endpointValidator.validateOnlyPrivateSchemaAccess(database, principal);
        endpointValidator.validateOnlyPrivateSchemaHasRole(database, principal, "list-tables");
        return ResponseEntity.ok(filterTables(database, principal)
                .stream()
                .map(metadataMapper::tableToTableBriefDto)
                .collect(Collectors.toList()));
    }

    public List<Table> filterTables(Database database, Principal principal) {
        final List<Table> tables = database.getTables();
        DatabaseAccess access = null;
        if (principal != null) {
            if (AuthUtil.isSystem(principal)) {
                return tables;
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUsername().equals(AuthUtil.getUsername(principal)))
                    .findFirst();
            if (optional.isPresent()) {
                access = optional.get();
            }
        }
        final Boolean hasAccess = access != null;
        return tables.stream()
                .filter(t -> t.getIsPublic() || t.getIsSchemaPublic() || hasAccess)
                .toList();
    }

    @PutMapping("/{tableId}/statistic")
    @Transactional
    @PreAuthorize("hasAuthority('update-table-statistic') or hasAnyAuthority('system')")
    @Observed(name = "dbrepo_statistic_table_update")
    @Operation(summary = "Update statistics",
            description = "Updates basic statistical properties (min, max, mean, median, std.dev) for numerical columns in a table with id. This action can only be performed by the table owner. Requires role `update-table-statistic`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated table statistics successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Failed to map column statistic to known columns",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not the owner",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> updateStatistic(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                @NotNull @PathVariable("tableId") UUID tableId,
                                                Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, SearchServiceException, NotAllowedException,
            SearchServiceConnectionException, MalformedException, DataServiceException, DataServiceConnectionException {
        log.debug("endpoint update table statistics, databaseId={}, tableId={}", databaseId, tableId);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!table.getOwnedBy().equals(AuthUtil.getUsername(principal)) && !AuthUtil.isSystem(principal)) {
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
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find user/table/database/ontology in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<ColumnDto> updateColumn(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                  @NotNull @PathVariable("tableId") UUID tableId,
                                                  @NotNull @PathVariable("columnId") UUID columnId,
                                                  @NotNull @Valid @RequestBody ColumnSemanticsUpdateDto updateDto,
                                                  Principal principal) throws NotAllowedException,
            MalformedException, DataServiceException, DataServiceConnectionException, TableNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException, OntologyNotFoundException,
            SearchServiceConnectionException, SemanticEntityNotFoundException {
        log.debug("endpoint update table, databaseId={}, tableId={}, columnId={}", databaseId,
                tableId, columnId);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!AuthUtil.hasRole(principal, "modify-foreign-table-column-semantics")) {
            endpointValidator.validateOnlyAccess(database, principal, true);
            endpointValidator.validateOnlyOwnerOrWriteAll(table, AuthUtil.getUsername(principal));
        }
        tableService.update(tableService.findColumnById(table, columnId), updateDto);
        return ResponseEntity.accepted()
                .build();
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
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Create table not permitted",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database, container or user could not be found",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "Create table conflicts with existing table name",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<TableBriefDto> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                @NotNull @Valid @RequestBody CreateTableDto data,
                                                Principal principal) throws NotAllowedException, MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, TableNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException,
            DashboardServiceException, DashboardServiceConnectionException {
        log.debug("endpoint create table, databaseId={}, data.name={}", databaseId, data.getName());
        final Database database = databaseService.findById(databaseId);
        endpointValidator.validateOnlyAccess(database, principal, true);
        endpointValidator.validateColumnCreateConstraints(data);
        final Table table = tableService.createTable(database, data, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.tableToTableBriefDto(table));
    }

    @PutMapping("/{tableId}")
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("hasAuthority('update-table') or hasAuthority('system')")
    @Observed(name = "dbrepo_table_update")
    @Operation(summary = "Update table",
            description = "Updates a table in the database with id. Requires role `update-table` or `system`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated the table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Update table visibility payload is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Update table visibility not permitted",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Table could not be found",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<TableBriefDto> update(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                @NotNull @PathVariable("tableId") UUID tableId,
                                                @NotNull @Valid @RequestBody TableUpdateDto data,
                                                Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException {
        log.debug("endpoint update table, databaseId={}, data.is_public={}, data.is_schema_public={}",
                databaseId, data.getIsPublic(), data.getIsSchemaPublic());
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!table.getOwnedBy().equals(AuthUtil.getUsername(principal)) && !AuthUtil.isSystem(principal)) {
            log.error("Failed to update table: not owner");
            throw new NotAllowedException("Failed to update table: not owner");
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.tableToTableBriefDto(tableService.updateTable(table, data)));
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database or container could not be found",
                    content = {@Content}),
    })
    public ResponseEntity<TableDto> findById(@NotNull @PathVariable("databaseId") UUID databaseId,
                                             @NotNull @PathVariable("tableId") UUID tableId,
                                             Principal principal) throws TableNotFoundException,
            DatabaseNotFoundException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint find table, databaseId={}, tableId={}", databaseId, tableId);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (principal != null) {
            if (AuthUtil.isSystem(principal)) {
                return ResponseEntity.ok(metadataMapper.tableToTableDto(table));
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUsername().equals(AuthUtil.getUsername(principal)))
                    .findFirst();
            if (table.getIsPublic() || table.getIsSchemaPublic() || optional.isPresent()) {
                return ResponseEntity.ok(metadataMapper.tableToTableDto(table));
            }
        }
        if (!table.getIsPublic() && !table.getIsSchemaPublic()) {
            log.error("Failed to find table: not public and no access found");
            throw new NotAllowedException("Failed to find table: not public and no access found");
        }
        return ResponseEntity.ok(metadataMapper.tableToTableDto(table));
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
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database or container could not be found",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @NotNull @PathVariable("tableId") UUID tableId,
                                       Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, TableNotFoundException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException {
        log.debug("endpoint delete table, databaseId={}, tableId={}", databaseId, tableId);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        /* roles */
        if (!table.getOwnedBy().equals(AuthUtil.getUsername(principal)) && !AuthUtil.hasRole(principal, "delete-foreign-table")) {
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
        dashboardService.update(databaseService.findById(databaseId));
        return ResponseEntity.accepted()
                .build();
    }

}
