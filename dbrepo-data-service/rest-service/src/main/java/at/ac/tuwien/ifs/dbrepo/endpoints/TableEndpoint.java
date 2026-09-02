package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import at.ac.tuwien.ifs.dbrepo.service.DataService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
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
import org.apache.spark.sql.Row;
import org.apache.spark.sql.classic.Dataset;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/table")
public class TableEndpoint {

    private final DataMapper dataMapper;
    private final DataService dataService;
    private final TableService tableService;
    private final MariaDbMapper mariaDbMapper;
    private final AnalyseService analyseService;
    private final MetadataService metadataService;
    private final ReplicationService replicationService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;

    private static final String MEDIA_TYPE_TEXT_CSV = "text/csv";

    @Autowired
    public TableEndpoint(DataMapper dataMapper, DataService dataService, TableService tableService,
                         MariaDbMapper mariaDbMapper, AnalyseService analyseService, MetadataService metadataService,
                         ReplicationService replicationService, EndpointValidator endpointValidator,
                         MetadataServiceGateway metadataServiceGateway) {
        this.dataMapper = dataMapper;
        this.dataService = dataService;
        this.tableService = tableService;
        this.mariaDbMapper = mariaDbMapper;
        this.analyseService = analyseService;
        this.metadataService = metadataService;
        this.replicationService = replicationService;
        this.endpointValidator = endpointValidator;
        this.metadataServiceGateway = metadataServiceGateway;
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
        final Database database = metadataService.getDatabase(databaseId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(tableService.create(database, data));
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
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            tableService.update(database, table, data);
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
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
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
                                     @RequestParam(required = false) String sortColumn,
                                     @RequestParam(required = false) String sortDirection,
                                     @NotNull @RequestHeader("Accept") String accept,
                                     @NotNull HttpServletRequest request,
                                     Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            PaginationException, MetadataServiceException, NotAllowedException, DatabaseNotFoundException,
            FormatNotAvailableException, MalformedException, ColumnNotFoundException, StorageNotFoundException, ImageInvalidException, AnalyseDataTypesException {
        log.debug("endpoint get table data, databaseId={}, tableId={}, timestamp={}, page={}, size={}, sortColumn={}, sortDirection={}, accept={}",
                databaseId, tableId, timestamp, page, size, sortColumn, sortDirection, accept);
        endpointValidator.validateDataParams(page, size);
        endpointValidator.validateDataSortParams(sortColumn, sortDirection);
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
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed find table data: authentication required");
                throw new NotAllowedException("Failed to find table data: authentication required");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
            }
        }
        try {
            final HttpHeaders headers = new HttpHeaders();
            if (request.getMethod().equals("HEAD")) {
                headers.set("Access-Control-Expose-Headers", "X-Count");
                headers.set("X-Count", "" + tableService.getCount(database, table.getInternalName(), timestamp));
                return ResponseEntity.ok()
                        .headers(headers)
                        .build();
            }
            final List<String> sortColumns = resolveSortColumns(database, table, sortColumn);
            final String effectiveSortDirection = sortColumn == null ? "asc" : sortDirection;
            final String query = mariaDbMapper.defaultRawSelectQuery(database.getInternalName(),
                    table.getInternalName(), timestamp,
                    accept.equals(MEDIA_TYPE_TEXT_CSV) ? null : page,
                    accept.equals(MEDIA_TYPE_TEXT_CSV) ? null : size,
                    sortColumns, effectiveSortDirection);
            headers.set("Access-Control-Expose-Headers", "X-Headers");
            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
                    final Dataset<Row> dataset1 = dataService.getSubsetAsJson(database, query,
                            table.getColumns().stream()
                                    .map(Column::getInternalName)
                                    .toList());
                    headers.set("X-Headers", dataMapper.datasetToColumnNameHeader(dataset1));
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(dataMapper.datasetToJson(dataset1));
                case MEDIA_TYPE_TEXT_CSV:
                    final List<String> responseColumns = table.getColumns().stream()
                            .map(Column::getInternalName)
                            .toList();
                    final Dataset<Row> dataset2 = dataService.getSubsetAsCsv(database, query);
                    headers.set("Content-Disposition", "attachment; filename=\"dataset.csv\"");
                    headers.set("X-Headers", String.join(",", responseColumns));
                    return ResponseEntity.status(HttpStatus.OK)
                            .contentType(MediaType.parseMediaType(MEDIA_TYPE_TEXT_CSV))
                            .headers(headers)
                            .body(dataMapper.datasetToCsv(dataset2, responseColumns));
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

    private List<String> resolveSortColumns(Database database, Table table, String sortColumn)
            throws ColumnNotFoundException, SQLException, TableNotFoundException {
        if (sortColumn != null) {
            return List.of(validateSortColumn(table, sortColumn));
        }
        return resolvePrimaryKeySortColumns(database, table);
    }

    private String validateSortColumn(Table table, String sortColumn) throws ColumnNotFoundException {
        return table.getColumns()
                .stream()
                .map(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column::getInternalName)
                .filter(sortColumn::equals)
                .findFirst()
                .orElseThrow(() -> new ColumnNotFoundException("Failed to find column: " + sortColumn));
    }

    private List<String> resolvePrimaryKeySortColumns(Database database, Table table)
            throws SQLException, TableNotFoundException {
        final TableDto inspectedTable = tableService.inspect(database, table.getInternalName());
        if (inspectedTable.getConstraints() == null || inspectedTable.getConstraints().getPrimaryKey() == null) {
            return List.of();
        }
        return inspectedTable.getConstraints().getPrimaryKey().stream()
                .map(primaryKey -> primaryKey.getColumn().getInternalName())
                .toList();
    }

    private boolean hasReplicaLocations(Database database, Table table) {
        return hasReplicaLocations(database.getReplicaUrls()) || hasReplicaLocations(table.getReplicaUrls());
    }

    private boolean hasReplicaLocations(Map<String, UUID> replicaUrls) {
        return replicaUrls != null && !replicaUrls.isEmpty();
    }

    private TupleDto tupleFromReplicationPayload(Table table, DataReplicationDto data) throws TableMalformedException {
        final Map<String, Object> values = tupleData(data);
        requireReplicationKey(table, values);
        final Map<String, Object> tuple = new LinkedHashMap<>();
        table.getColumns()
                .stream()
                .map(Column::getInternalName)
                .filter(values::containsKey)
                .forEach(column -> tuple.put(column, values.get(column)));
        return TupleDto.builder()
                .data(tuple)
                .build();
    }

    private TupleUpdateDto tupleUpdateFromReplicationPayload(Table table, DataReplicationDto data)
            throws TableMalformedException {
        final Map<String, Object> values = tupleData(data);
        final Object replicationKey = requireReplicationKey(table, values);
        final Map<String, Object> tuple = new LinkedHashMap<>();
        table.getColumns()
                .stream()
                .map(Column::getInternalName)
                .filter(column -> !"replication_key".equals(column))
                .filter(values::containsKey)
                .forEach(column -> tuple.put(column, values.get(column)));
        if (tuple.isEmpty()) {
            throw new TableMalformedException("Replication payload is missing tuple data");
        }
        return TupleUpdateDto.builder()
                .keys(replicationKeyKeys(replicationKey))
                .data(tuple)
                .build();
    }

    private TupleDeleteDto tupleDeleteFromReplicationPayload(Table table, DataReplicationDto data)
            throws TableMalformedException {
        final Object replicationKey = requireReplicationKey(table, tupleData(data));
        return TupleDeleteDto.builder()
                .keys(replicationKeyKeys(replicationKey))
                .build();
    }

    private Map<String, Object> tupleData(DataReplicationDto data) throws TableMalformedException {
        if (data == null || data.getTuple() == null || data.getTuple().getData() == null) {
            throw new TableMalformedException("Replication payload is missing tuple data");
        }
        return data.getTuple()
                .getData();
    }

    private Object requireReplicationKey(Table table, Map<String, Object> values) throws TableMalformedException {
        final boolean hasReplicationKeyColumn = table.getColumns() != null && table.getColumns()
                .stream()
                .map(Column::getInternalName)
                .anyMatch("replication_key"::equals);
        if (!hasReplicationKeyColumn) {
            throw new TableMalformedException("Table is missing the replication_key column");
        }
        final Object replicationKey = values.get("replication_key");
        if (replicationKey == null) {
            throw new TableMalformedException("Replication payload is missing the replication_key value");
        }
        return replicationKey;
    }

    private Map<String, Object> replicationKeyKeys(Object replicationKey) {
        final Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("replication_key", replicationKey);
        return keys;
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
                                               Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint insert raw table data, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        endpointValidator.validateOnlyWriteAccess(database, table, principal);
        try {
            if (hasReplicaLocations(database, table)) {
                final TupleWithTimestampsDto created = tableService.createTupleWithTimestamps(database, table, data);
                replicationService.replicateTuple(created, database, table);
            } else {
                tableService.createTuple(database, table, data);
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{tableId}/data/replicate")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_table_data_replicate_create")
    @Operation(summary = "Insert replicated tuple",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created replicated table data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TupleWithTimestampsDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Replicated table data is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database or blob in storage service",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service or storage service",
                    content = {@Content}),
    })
    public ResponseEntity<TupleWithTimestampsDto> insertTupleForReplication(
            @NotNull @PathVariable("databaseId") UUID databaseId,
            @NotNull @PathVariable("tableId") UUID tableId,
            @Valid @RequestBody DataReplicationDto data)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException,
            MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint insert replicated table data, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            final TupleWithTimestampsDto created = tableService.createTupleWithTimestamps(database, table,
                    tupleFromReplicationPayload(table, data));
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
                                               Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, MetadataServiceException,
            DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        log.debug("endpoint update raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        endpointValidator.validateOnlyWriteAccess(database, table, principal);
        try {
            if (hasReplicaLocations(database, table)) {
                final TupleWithTimestampsDto updated = tableService.updateTupleWithTimestamps(database, table, data);
                replicationService.replicateTupleUpdate(updated, database, table);
            } else {
                tableService.updateTuple(database, table, data);
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{tableId}/data/replicate")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_table_data_replicate_update")
    @Operation(summary = "Update replicated tuple",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Updated replicated table data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TupleWithTimestampsDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Replicated table data is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database or blob in storage service",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service or storage service",
                    content = {@Content}),
    })
    public ResponseEntity<TupleWithTimestampsDto> updateTupleForReplication(
            @NotNull @PathVariable("databaseId") UUID databaseId,
            @NotNull @PathVariable("tableId") UUID tableId,
            @Valid @RequestBody DataReplicationDto data)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException,
            MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint update replicated table data, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            final TupleWithTimestampsDto updated = tableService.updateTupleWithTimestamps(database, table,
                    tupleUpdateFromReplicationPayload(table, data));
            return ResponseEntity.ok(updated);
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
                                               Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, MetadataServiceException,
            DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        log.debug("endpoint delete raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        endpointValidator.validateOnlyWriteAccess(database, table, principal);
        try {
            if (hasReplicaLocations(database, table)) {
                final TupleWithTimestampsDto deleted = tableService.deleteTupleWithTimestamps(database, table, data);
                replicationService.replicateTupleDelete(deleted, database, table);
            } else {
                tableService.deleteTuple(database, table, data);
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{tableId}/data/replicate")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_table_data_replicate_delete")
    @Operation(summary = "Delete replicated tuple",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Deleted replicated table data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TupleWithTimestampsDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Replicated table data is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database or blob in storage service",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service or storage service",
                    content = {@Content}),
    })
    public ResponseEntity<TupleWithTimestampsDto> deleteTupleForReplication(
            @NotNull @PathVariable("databaseId") UUID databaseId,
            @NotNull @PathVariable("tableId") UUID tableId,
            @Valid @RequestBody DataReplicationDto data)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException,
            MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint delete replicated table data, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            final TupleWithTimestampsDto deleted = tableService.deleteTupleWithTimestamps(database, table,
                    tupleDeleteFromReplicationPayload(table, data));
            return ResponseEntity.ok(deleted);
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
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to find table history: no authentication found");
                throw new NotAllowedException("Failed to find table history: no authentication found");
            }
            endpointValidator.validateOnlyAccess(database, principal);
        }
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
    public ResponseEntity<List<TableDto>> findAll(@NotNull @PathVariable("databaseId") UUID databaseId)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            DatabaseMalformedException, TableNotFoundException, MetadataServiceException {
        log.debug("endpoint inspect table schemas, databaseId={}", databaseId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            return ResponseEntity.ok(tableService.explore(database));
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
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        endpointValidator.validateOnlyWriteAccess(database, table, principal);
        if (data.getLineTermination() == null) {
            data.setLineTermination("\\r\\n");
            log.debug("line termination not present, default to {}", data.getLineTermination());
        }
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
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to get statistic",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table or database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<TableStatisticDto> getStatistic(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                          @NotNull @PathVariable("tableId") UUID tableId,
                                                          Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, TableMalformedException, DatabaseNotFoundException, NotAllowedException {
        log.debug("endpoint generate table statistic, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to get statistic from table: unauthorized");
                throw new NotAllowedException("Failed to get statistic from table: unauthorized");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
            }
        }
        try {
            return ResponseEntity.ok(tableService.getStatistics(database, table.getId(), table.getInternalName()));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
