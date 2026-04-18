package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.api.Result;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
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
import org.apache.logging.log4j.util.Strings;
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
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/table")
public class TableEndpoint {

    private final TableService tableService;
    private final MetadataService metadataService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;

    private static final String MEDIA_TYPE_TEXT_CSV = "text/csv";

    @Autowired
    public TableEndpoint(TableService tableService, MetadataService metadataService,
                         EndpointValidator endpointValidator, MetadataServiceGateway metadataServiceGateway) {
        this.tableService = tableService;
        this.metadataService = metadataService;
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
                            @Header(name = "X-Headers", description = "Header names", schema = @Schema(implementation = String.class)),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose `X-Count` and `X-Headers` custom headers", schema = @Schema(implementation = String.class), required = true)},
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
            FormatNotAvailableException, MalformedException, ColumnNotFoundException, StorageNotFoundException, ImageInvalidException, AnalyseDataTypesException, DatabaseMalformedException {
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
            headers.set("Access-Control-Expose-Headers", "X-Count X-Headers");
            final Set<String> columns = Set.copyOf(table.getColumns().stream().map(Column::getInternalName).toList());
            final List<Map<String, Object>> data = tableService.getData(database, columns, table.getInternalName(), timestamp,
                    accept.equals(MEDIA_TYPE_TEXT_CSV) ? null : page,
                    accept.equals(MEDIA_TYPE_TEXT_CSV) ? null : size);
            headers.set("X-Headers", Strings.join(columns, ','));
            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(data);
                case MEDIA_TYPE_TEXT_CSV:
//                    final Dataset<Row> dataset2 = dataService.getSubsetAsCsv(database, query);
                    headers.set("Content-Disposition", "attachment; filename=\"dataset.csv\"");
//                    headers.set("X-Headers", dataMapper.datasetToColumnNameHeader(dataset2));
                    return ResponseEntity.status(HttpStatus.OK)
                            .headers(headers)
                            .body(null);
//                            .body(dataset2);
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
                                               Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint insert raw table data, databaseId={}, tableId={}", databaseId, tableId);
        final Table table = metadataService.getTable(databaseId, tableId);
        final Database database = metadataService.getDatabase(databaseId);
        endpointValidator.validateOnlyWriteAccess(database, table, principal);
        try {
            tableService.createTuple(database, table, data);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .build();
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
            DatabaseUnavailableException, DatabaseNotFoundException, TableMalformedException {
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
        } catch (SQLException e) {
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
