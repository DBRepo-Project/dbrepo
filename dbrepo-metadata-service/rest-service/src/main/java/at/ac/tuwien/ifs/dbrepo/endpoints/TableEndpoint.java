package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.EntityDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.TableColumnEntityDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.*;
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
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/v1/database/{databaseId}/table")
public class TableEndpoint extends AbstractEndpoint {
    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;


    private final UserService userService;
    private final TableService tableService;
    private final EntityService entityService;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final DashboardService dashboardService;
    private final EndpointValidator endpointValidator;
    private final ReplicationService replicationService;

    @Autowired
    public TableEndpoint(UserService userService, TableService tableService, EntityService entityService,
                         MetadataMapper metadataMapper, DatabaseService databaseService,
                         DashboardService dashboardService, EndpointValidator endpointValidator,
                         ReplicationService replicationService) {
        this.userService = userService;
        this.tableService = tableService;
        this.entityService = entityService;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.dashboardService = dashboardService;
        this.endpointValidator = endpointValidator;
        this.replicationService = replicationService;
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
            if (isSystem(principal)) {
                return tables;
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUser().getUsername().equals(getUsername(principal)))
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
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not the table owner.",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Generated query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content}),
    })
    public ResponseEntity<List<EntityDto>> analyseTable(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                        @NotNull @PathVariable("tableId") UUID tableId,
                                                        Principal principal)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException, NotAllowedException {
        log.debug("endpoint analyse table semantics, databaseId={}, tableId={}", databaseId, tableId);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!table.getOwner().getUsername().equals(getUsername(principal))) {
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
        if (!table.getOwner().getUsername().equals(getUsername(principal)) && !isSystem(principal)) {
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
            MalformedException, DataServiceException, DataServiceConnectionException, UserNotFoundException,
            TableNotFoundException, DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, OntologyNotFoundException, SemanticEntityNotFoundException {
        log.debug("endpoint update table, databaseId={}, tableId={}, columnId={}", databaseId,
                tableId, columnId);
        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        if (!hasRole(principal, "modify-foreign-table-column-semantics")) {
            endpointValidator.validateOnlyAccess(database, principal, true);
            endpointValidator.validateOnlyOwnerOrWriteAll(table, userService.findByUsername(getUsername(principal)));
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
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content}),
    })
    public ResponseEntity<List<TableColumnEntityDto>> analyseTableColumn(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                         @NotNull @PathVariable("tableId") UUID tableId,
                                                                         @NotNull @PathVariable("columnId") UUID columnId)
            throws MalformedException, TableNotFoundException, DatabaseNotFoundException {
        log.debug("endpoint analyse table column semantics, databaseId={}, tableId={}, columnId={}",
                databaseId, tableId, columnId);
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
        dashboardService.update(table.getDatabase());


        // Handle replication after the transaction is committed
        if (data.getCreationLocation() == null && database.getReplicaUrls() != null && database.getReplicaUrls().size() > 0) {
            log.debug("Triggering replication for table - databaseId: {}, tableName: {}, replicaUrls: {}", 
                    databaseId, data.getName(), database.getReplicaUrls());
            try {
                // Create a new list to avoid lazy loading issues
                List<ReplicaLocation> replicas = new ArrayList<>(database.getReplicaUrls());
                data.setCreationLocation(baseUrl);
                replicationService.replicateTable(data, databaseId, replicas, table.getId());
            } catch (Exception e) {
                log.error("Failed to trigger replication for table {} in database {}: {}", data.getName(), databaseId, e.getMessage());
                // Don't fail the table creation if replication fails
            }
        } else {
            log.debug("Skipping table replication - databaseId: {}, replicaUrls size: {}", 
                    databaseId, database.getReplicaUrls() != null ? database.getReplicaUrls().size() : 0);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.tableToTableBriefDto(table));
    }

    @PostMapping("/replicate")
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_table_replicate")
    @Operation(summary = "Replicate table creation",
            description = "Creates a table from replication notification. Requires system authority.",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Replicated table successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Create table query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Replicate table not permitted",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database or container could not be found",
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
    public ResponseEntity<TableBriefDto> replicateTable(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                        @NotNull @Valid @RequestBody TableNotificationDto tableNotificationDto,
                                                        Principal principal)
            throws NotAllowedException, MalformedException, DataServiceException, DataServiceConnectionException, 
            DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException, TableNotFoundException, 
            TableExistsException, SearchServiceException, SearchServiceConnectionException, OntologyNotFoundException, 
            SemanticEntityNotFoundException, DashboardServiceException, DashboardServiceConnectionException {

        log.debug("endpoint replicate table, databaseId={}, tableName={}", databaseId, 
                tableNotificationDto.getCreateTableDto().getName());
        
        final Database database = databaseService.findById(databaseId);
        
        // Create the table using the CreateTableDto from the notification
        CreateTableDto createTableDto = tableNotificationDto.getCreateTableDto();

        // Set creationLocation to null to avoid infinite replication loops
        createTableDto.setCreationLocation(null);
        
        final Table table = tableService.createTable(database, createTableDto, principal, tableNotificationDto.getCreationId());
        dashboardService.update(table.getDatabase());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.tableToTableBriefDto(table));
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
        if (!table.getOwner().getUsername().equals(getUsername(principal))) {
            log.error("Failed to update table: not owner");
            throw new NotAllowedException("Failed to update table: not owner");
        }
        final Table table1 = tableService.updateTable(table, data);
        dashboardService.update(table1.getDatabase());
        return ResponseEntity.accepted()
                .body(metadataMapper.tableToTableBriefDto(table1));
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
            if (isSystem(principal)) {
                return ResponseEntity.ok(metadataMapper.tableToTableDto(table));
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUser().getUsername().equals(getUsername(principal)))
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
        if (!table.getOwner().getUsername().equals(getUsername(principal)) && !hasRole(principal, "delete-foreign-table")) {
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

    @PutMapping("/{tableId}/replication-url")
    @Transactional
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_table_replication_url_update")
    @Operation(summary = "Update table replication URL",
            description = "Updates the replication URL with the remote table ID for a given table. Only the table owner can perform this operation. Requires role `modify-table-replication`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Replication URL updated successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "The replication URL update payload is malformed or replication URL not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Replication URL update is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
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
    public ResponseEntity<TableBriefDto> updateReplicationUrl(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                           @NotNull @PathVariable("tableId") UUID tableId,
                                                           @NotNull @Valid @RequestBody TableUpdateReplicationUrlDto data,
                                                           Principal principal) throws TableNotFoundException,
            NotAllowedException, SearchServiceException, SearchServiceConnectionException, DatabaseNotFoundException {
        log.debug("endpoint update replication URL, databaseId={}, tableId={}, replicaUrl={}, replicaTableId={}",
                databaseId, tableId, data.getReplicaUrl(), data.getReplicaTableId());

        final Database database = databaseService.findById(databaseId);
        final Table table = tableService.findById(database, tableId);
        
        if (!table.getOwner().getUsername().equals(getUsername(principal))) {
            log.error("Failed to update replication URL: not owner");
            throw new NotAllowedException("Failed to update replication URL: not owner");
        }

        final Table updatedTable = tableService.updateReplicationUrl(tableId, data);
        return ResponseEntity.accepted()
                .body(metadataMapper.tableToTableBriefDto(updatedTable));
    }

}
