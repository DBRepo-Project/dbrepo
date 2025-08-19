package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.ExportResourceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/table")
public class TableEndpoint extends RestEndpoint {

    private final CacheService cacheService;
    private final TableService tableService;
    private final MariaDbMapper mariaDbMapper;
    private final SubsetService subsetService;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;
    private final ReplicationService replicationService;

    private static final String MEDIA_TYPE_TEXT_CSV = "text/csv";

    @Autowired
    public TableEndpoint(CacheService cacheService, TableService tableService, MariaDbMapper mariaDbMapper,
                         SubsetService subsetService, StorageService storageService, DatabaseService databaseService,
                         EndpointValidator endpointValidator, MetadataServiceGateway metadataServiceGateway, ReplicationService replicationService) {
        this.cacheService = cacheService;
        this.tableService = tableService;
        this.mariaDbMapper = mariaDbMapper;
        this.subsetService = subsetService;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.endpointValidator = endpointValidator;
        this.metadataServiceGateway = metadataServiceGateway;
        this.replicationService = replicationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Create table",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Table schema or query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database or table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "Table name already exists in database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<TableDto> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                           @Valid @RequestBody CreateTableDto data) throws DatabaseNotFoundException,
            RemoteUnavailableException, TableMalformedException, DatabaseUnavailableException, TableExistsException,
            TableNotFoundException, QueryMalformedException, MetadataServiceException, ContainerNotFoundException {
        log.debug("endpoint create table, databaseId={}, data.name={}", databaseId, data.getName());
        /* check */
        if (data.getConstraints().getPrimaryKey().isEmpty()) {
            log.error("Table must have a primary key");
            throw new TableMalformedException("Table must have a primary key");
        }
        /* create */
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        // If replicas are configured for this database, ensure a replication_key column exists
        if (database.getReplicaUrls() != null && !database.getReplicaUrls().isEmpty()) {
            final boolean hasReplicationKey = data.getColumns()
                    .stream()
                    .anyMatch(c -> "replication_key".equalsIgnoreCase(c.getName()));
            if (!hasReplicationKey) {
                final java.util.List<CreateTableColumnDto> mutableColumns = new java.util.ArrayList<>(data.getColumns());
                mutableColumns.add(CreateTableColumnDto.builder()
                        .name("replication_key")
                        .type(ColumnTypeDto.VARCHAR)
                        .size(36L)
                        .nullAllowed(false)
                        .description("Replication key")
                        .build());
                data.setColumns(mutableColumns);
            }
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(databaseService.createTable(database, data));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{tableId}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Update table",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Table schema or query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database or table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<TableDto> update(@NotNull @PathVariable("databaseId") UUID databaseId,
                                           @NotNull @PathVariable("tableId") UUID tableId,
                                           @Valid @RequestBody TableUpdateDto data) throws RemoteUnavailableException,
            TableMalformedException, DatabaseUnavailableException, TableNotFoundException, MetadataServiceException,
            DatabaseNotFoundException {
        log.debug("endpoint update table, databaseId={}, data.description={}", databaseId, data.getDescription());
        /* create */
        final TableDto table = cacheService.getTable(databaseId, tableId);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            tableService.updateTable(database, table, data);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{tableId}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Delete table",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Deletion query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @NotNull @PathVariable("tableId") UUID tableId)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            QueryMalformedException, MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint delete table, databaseId={}, tableId={}", databaseId, tableId);
        final TableDto table = cacheService.getTable(databaseId, tableId);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            tableService.delete(database, table);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/{tableId}/data", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Observed(name = "dbrepo_table_data_list")
    @Operation(summary = "Get table data",
            description = "Gets data from a table with id. For a table in a private database, the user needs to have at least *READ* access to the associated database. Requests with HTTP method **GET** return the full dataset, requests with HTTP method **HEAD** only the number of tuples in the `X-Count` header.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Get table data",
                    headers = {@Header(name = "X-Count", description = "Number of rows", schema = @Schema(implementation = Long.class), required = true),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose `X-Count` custom header", schema = @Schema(implementation = String.class), required = true)},
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map[].class)),
                            @Content(mediaType = "text/csv")}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to get table data",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to format data",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<?> getData(@NotNull @PathVariable("databaseId") UUID databaseId,
                                     @NotNull @PathVariable("tableId") UUID tableId,
                                     @RequestParam(required = false) Instant timestamp,
                                     @RequestParam(required = false) Long page,
                                     @RequestParam(required = false) Long size,
                                     @NotNull @RequestHeader("Accept") String accept,
                                     @NotNull HttpServletRequest request,
                                     Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            PaginationException, MetadataServiceException, NotAllowedException, DatabaseNotFoundException,
            FormatNotAvailableException, StorageUnavailableException {
        log.debug("endpoint get table data, databaseId={}, tableId={}, timestamp={}, page={}, size={}, accept={}",
                databaseId, tableId, timestamp, page, size, accept);
        endpointValidator.validateDataParams(page, size);
        /* parameters */
        if (page == null) {
            page = 0L;
            log.debug("page not set: default to {}", page);
        }
        if (size == null) {
            size = 10L;
            log.debug("size not set: default to {}", size);
        }
        if (timestamp == null) {
            timestamp = Instant.now();
            log.debug("timestamp not set: default to {}", timestamp);
        }
        final TableDto table = cacheService.getTable(databaseId, tableId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed find table data: authentication required");
                throw new NotAllowedException("Failed to find table data: authentication required");
            }
            if (!isSystem(principal)) {
                cacheService.getAccess(databaseId, getUsername(principal));
            }
        }
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            final HttpHeaders headers = new HttpHeaders();
            if (request.getMethod().equals("HEAD")) {
                headers.set("Access-Control-Expose-Headers", "X-Count");
                headers.set("X-Count", "" + tableService.getCount(database, table.getInternalName(), timestamp));
                return ResponseEntity.ok()
                        .headers(headers)
                        .build();
            }
            headers.set("Access-Control-Expose-Headers", "X-Headers");
            headers.set("X-Headers", String.join(",", table.getColumns().stream().map(ColumnDto::getInternalName).toList()));
            final String query = mariaDbMapper.defaultRawSelectQuery(database.getInternalName(),
                    table.getInternalName(), timestamp,
                    accept.equals(MEDIA_TYPE_TEXT_CSV) ? null : page,
                    accept.equals(MEDIA_TYPE_TEXT_CSV) ? null : size);
            final Dataset<Row> dataset = subsetService.getData(database, query);
            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
                    log.trace("accept header matches json");
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(transform(dataset));
                case MEDIA_TYPE_TEXT_CSV:
                    log.trace("accept header matches csv");
                    final ExportResourceDto resource = storageService.transformDataset(dataset);
                    headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
                    return ResponseEntity.status(HttpStatus.OK)
                            .headers(headers)
                            .body(storageService.transformDataset(dataset)
                                    .getResource());
                default:
                    log.atError()
                            .setMessage("Invalid data format " + accept + " accepted")
                            .addKeyValue("request_header_accept", accept)
                            .log();
                    throw new FormatNotAvailableException("Header 'Accept' must be one of: application/json, text/csv value");
            }
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{tableId}/data")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Observed(name = "dbrepo_table_data_create")
    @Operation(summary = "Insert tuple",
            description = "Inserts a data tuple into a table, then the table statistics are updated. The user needs to have at least *WRITE_OWN* access to the associated database. Requires role `insert-table-data`.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created table data"),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Create table data not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database or blob in storage service",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service or storage service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> insertRawTuple(@NotNull @PathVariable("databaseId") UUID databaseId,
                                               @NotNull @PathVariable("tableId") UUID tableId,
                                               @Valid @RequestBody TupleDto data,
                                               Principal principal,
                                               @RequestHeader("Authorization") String authorization)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint insert raw table data, databaseId={}, tableId={}", databaseId, tableId);
        final TableDto table = cacheService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = cacheService.getAccess(databaseId, getUsername(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getUsername(), getUsername(principal));
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        
        // Check if table has replication_key column and auto-generate UUID if needed
        final boolean hasReplicationKey = table.getColumns().stream()
                .anyMatch(c -> "replication_key".equalsIgnoreCase(c.getInternalName()));
        if (hasReplicationKey && (data.getData() == null || !data.getData().containsKey("replication_key"))) {
            // Create a new map with the replication_key added
            final java.util.Map<String, Object> dataWithReplicationKey = new java.util.LinkedHashMap<>();
            if (data.getData() != null) {
                dataWithReplicationKey.putAll(data.getData());
            }
            dataWithReplicationKey.put("replication_key", java.util.UUID.randomUUID().toString());
            data = TupleDto.builder().data(dataWithReplicationKey).build();
            log.debug("Auto-generated replication_key UUID: {}", dataWithReplicationKey.get("replication_key"));
        }
        
        try {
            if (database.getReplicaUrls() != null && !database.getReplicaUrls().isEmpty()) {
                final Map<String, Object> created = tableService.createTupleWithTimestamps(database, table, data);
                log.info(String.valueOf(data.getData().get("replication_key")));
                log.atInfo()
                        .setMessage("created tuple with timestamps")
                        .addKeyValue("created", created)
                        .log();

                log.info("created tuple with timestamps {}", created);
                // replicate tuple to replication service
                replicationService.replicateTuple(created, database, table);
            } else {
                tableService.createTuple(database, table, data);
                log.debug("created tuple without timestamps (no replicas configured)");
            }
            metadataServiceGateway.updateTableStatistics(databaseId, tableId, authorization);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{tableId}/data/replicate")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Observed(name = "dbrepo_table_data_create")
    @Operation(summary = "Insert tuple",
            description = "Inserts a data tuple into a table, then the table statistics are updated. The user needs to have at least *WRITE_OWN* access to the associated database. Requires role `insert-table-data`.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created table data with timestamps",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database or blob in storage service",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service or storage service",
                    content = {@Content}),
    })
    public ResponseEntity<Map<String, Object>> insertTupleForReplication(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                         @NotNull @PathVariable("tableId") UUID tableId,
                                                                         @Valid @RequestBody DataReplicationDto data,
                                                                         Principal principal,
                                                                         @RequestHeader("Authorization") String authorization)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException, DatabaseNotFoundException, NotAllowedException {
        log.info("endpoint replicate insert, databaseId={}, tableId={}", databaseId, tableId);
        final TableDto table = cacheService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = cacheService.getAccess(databaseId, getUsername(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getUsername(), getUsername(principal));
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            // Log remote timestamps for debugging/traceability
            final Object remoteInsertedAt = data.getTuple() != null ? data.getTuple().get("inserted_at") : null;
            final Object remoteDeletedAt = data.getTuple() != null ? data.getTuple().get("deleted_at") : null;
            log.info("remote timestamps inserted_at={}, deleted_at={}", remoteInsertedAt, remoteDeletedAt);

            // Build a clean TupleDto containing only actual table columns (exclude versioning/meta keys)
            final java.util.Map<String, Object> clean = new java.util.LinkedHashMap<>();
            for (ColumnDto c : table.getColumns()) {
                clean.put(c.getInternalName(), data.getTuple() != null ? data.getTuple().get(c.getInternalName()) : null);
            }
            final TupleDto tuple = TupleDto.builder().data(clean).build();

            final Map<String, Object> created = tableService.createTupleWithTimestamps(database, table, tuple);
            log.atInfo()
                    .setMessage("replicate insert created tuple with timestamps")
                    .addKeyValue("created", created)
                    .log();
            metadataServiceGateway.updateTableStatistics(databaseId, tableId, authorization);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(created);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{tableId}/data")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Observed(name = "dbrepo_table_data_update")
    @Operation(summary = "Update tuple",
            description = "Updates a data tuple into a table, then the table statistics are updated. The user needs to have at least *WRITE_OWN* access to the associated database. Requires role `insert-table-data`.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated table data"),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Update table data not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> updateRawTuple(@NotNull @PathVariable("databaseId") UUID databaseId,
                                               @NotNull @PathVariable("tableId") UUID tableId,
                                               @Valid @RequestBody TupleUpdateDto data,
                                               Principal principal,
                                               @RequestHeader("Authorization") String authorization)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, MetadataServiceException,
            DatabaseNotFoundException {
        log.debug("endpoint update raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final TableDto table = cacheService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = cacheService.getAccess(databaseId, getUsername(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getUsername(), getUsername(principal));
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            tableService.updateTuple(database, table, data);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{tableId}/data")
    @PreAuthorize("hasAuthority('delete-table-data')")
    @Observed(name = "dbrepo_table_data_delete")
    @Operation(summary = "Delete tuple",
            description = "Deletes a data tuple into a table, then the table statistics are updated. The user needs to have at least *WRITE_OWN* access to the associated database. Requires role `delete-table-data`.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted table data"),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Delete table data not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> deleteRawTuple(@NotNull @PathVariable("databaseId") UUID databaseId,
                                               @NotNull @PathVariable("tableId") UUID tableId,
                                               @Valid @RequestBody TupleDeleteDto data,
                                               Principal principal,
                                               @RequestHeader("Authorization") String authorization)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, MetadataServiceException,
            DatabaseNotFoundException {
        log.debug("endpoint delete raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final TableDto table = cacheService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = cacheService.getAccess(databaseId, getUsername(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getUsername(), getUsername(principal));
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            tableService.deleteTuple(database, table, data);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{tableId}/history")
    @Observed(name = "dbrepo_table_data_history")
    @Operation(summary = "Get history",
            description = "Gets the insert/delete operations history performed. For tables in private databases, the user needs to have at least *READ* access to the associated database.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found table history",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableHistoryDto.class)))}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination size request, must be > 0",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Find table history not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table history in data database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<List<TableHistoryDto>> getHistory(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                            @NotNull @PathVariable("tableId") UUID tableId,
                                                            @RequestParam(value = "size", required = false) Long size,
                                                            Principal principal) throws DatabaseUnavailableException,
            RemoteUnavailableException, TableNotFoundException, NotAllowedException, MetadataServiceException,
            PaginationException, DatabaseNotFoundException {
        log.debug("endpoint find table history, databaseId={}, tableId={}", databaseId, tableId);
        if (size != null && size <= 0) {
            log.error("Invalid size: must be > 0");
            throw new PaginationException("Invalid size: must be bigger than zero");
        } else if (size == null) {
            log.debug("size not set: default to 100L");
            size = 100L;
        }
        final TableDto table = cacheService.getTable(databaseId, tableId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to find table history: no authentication found");
                throw new NotAllowedException("Failed to find table history: no authentication found");
            }
            cacheService.getAccess(databaseId, getUsername(principal));
        }
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            final List<TableHistoryDto> dto = tableService.history(database, table, size);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(dto);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_table_schema_list")
    @Operation(summary = "Find tables",
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Got table schemas",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableDto.class)))}),
            @ApiResponse(responseCode = "400",
                    description = "Schema data malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Find table schema not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to parse table schema",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<List<TableDto>> getSchema(@NotNull @PathVariable("databaseId") UUID databaseId)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            DatabaseMalformedException, TableNotFoundException, MetadataServiceException {
        log.debug("endpoint inspect table schemas, databaseId={}", databaseId);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            return ResponseEntity.ok(databaseService.exploreTables(database));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{tableId}/data/import")
    @Observed(name = "dbrepo_table_data_import")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Operation(summary = "Import dataset",
            description = "Imports a dataset in a table. Then update the table statistics. The user needs to have at least *WRITE_OWN* access to the associated database when importing into a owned table. Otherwise *WRITE_ALL* access in needed. Requires role `insert-table-data`.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Imported dataset successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Dataset and/or query are malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Import table dataset not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> importDataset(@NotNull @PathVariable("databaseId") UUID databaseId,
                                              @NotNull @PathVariable("tableId") UUID tableId,
                                              @Valid @RequestBody ImportDto data,
                                              Principal principal,
                                              @RequestHeader("Authorization") String authorization)
            throws RemoteUnavailableException, TableNotFoundException, NotAllowedException, MetadataServiceException,
            StorageNotFoundException, MalformedException, StorageUnavailableException, QueryMalformedException,
            DatabaseUnavailableException, DatabaseNotFoundException {
        log.atDebug()
                .setMessage("endpoint insert table data")
                .addKeyValue("database_id", databaseId)
                .addKeyValue("table_id", tableId)
                .addKeyValue("data", data)
                .log();
        final TableDto table = cacheService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = cacheService.getAccess(databaseId, getUsername(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getUsername(), getUsername(principal));
        if (data.getLineTermination() == null) {
            data.setLineTermination("\\r\\n");
            log.debug("line termination not present, default to {}", data.getLineTermination());
        }
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            tableService.importDataset(database, table, data);
        } catch (SQLException | TableMalformedException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
        metadataServiceGateway.updateTableStatistics(databaseId, tableId, authorization);
        return ResponseEntity.accepted()
                .build();
    }

    @GetMapping("/{tableId}/statistic")
    @Observed(name = "dbrepo_table_statistic")
    @Operation(summary = "Get table statistic",
            description = "Gets basic statistical properties (min, max, mean, median, std.dev) of numerical columns of a table with id.",
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Generated table statistic",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableStatisticDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Failed to obtain column statistic",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table or database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<TableStatisticDto> statistic(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                       @NotNull @PathVariable("tableId") UUID tableId)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, TableMalformedException, DatabaseNotFoundException {
        log.debug("endpoint generate table statistic, databaseId={}, tableId={}", databaseId, tableId);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        final TableDto table = cacheService.getTable(databaseId, tableId);
        try {
            return ResponseEntity.ok(tableService.getStatistics(database, table.getInternalName()));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
