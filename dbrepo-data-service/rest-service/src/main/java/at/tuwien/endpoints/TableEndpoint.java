package at.tuwien.endpoints;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.*;
import at.tuwien.utils.UserUtil;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/table")
public class TableEndpoint extends AbstractEndpoint {

    private final TableService tableService;
    private final SchemaService schemaService;
    private final MetricsService metricsService;
    private final StorageService storageService;
    private final CredentialService credentialService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public TableEndpoint(TableService tableService, SchemaService schemaService, MetricsService metricsService,
                         StorageService storageService, CredentialService credentialService,
                         EndpointValidator endpointValidator, MetadataServiceGateway metadataServiceGateway) {
        this.tableService = tableService;
        this.schemaService = schemaService;
        this.metricsService = metricsService;
        this.storageService = storageService;
        this.credentialService = credentialService;
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database or table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Table name already exists in database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TableDto> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                           @Valid @RequestBody TableCreateDto data) throws DatabaseNotFoundException,
            RemoteUnavailableException, TableMalformedException, DatabaseUnavailableException, TableExistsException,
            TableNotFoundException, QueryMalformedException, MetadataServiceException, ContainerNotFoundException {
        log.debug("endpoint create table, databaseId={}, data.name={}", databaseId, data.getName());
        /* check */
        if (data.getConstraints().getPrimaryKey().isEmpty()) {
            log.error("Table must have a primary key");
            throw new TableMalformedException("Table must have a primary key");
        }
        /* create */
        final PrivilegedDatabaseDto database = credentialService.getDatabase(databaseId);
        try {
            final TableDto table = tableService.createTable(database, data);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(schemaService.inspectTable(database, table.getInternalName()));
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database or table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TableDto> update(@NotNull @PathVariable("databaseId") Long databaseId,
                                           @NotNull @PathVariable("tableId") Long tableId,
                                           @Valid @RequestBody TableUpdateDto data) throws RemoteUnavailableException,
            TableMalformedException, DatabaseUnavailableException, TableNotFoundException, MetadataServiceException {
        log.debug("endpoint update table, databaseId={}, data.description={}", databaseId, data.getDescription());
        /* create */
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        try {
            tableService.updateTable(table, data);
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            QueryMalformedException, MetadataServiceException {
        log.debug("endpoint delete table, databaseId={}, tableId={}", databaseId, tableId);
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        try {
            tableService.delete(table);
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
                            schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to get table data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<Map<String, Object>>> getData(@NotNull @PathVariable("databaseId") Long databaseId,
                                                             @NotNull @PathVariable("tableId") Long tableId,
                                                             @RequestParam(required = false) Instant timestamp,
                                                             @RequestParam(required = false) Long page,
                                                             @RequestParam(required = false) Long size,
                                                             @NotNull HttpServletRequest request,
                                                             Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            PaginationException, MetadataServiceException, NotAllowedException {
        log.debug("endpoint get table data, databaseId={}, tableId={}, timestamp={}, page={}, size={}", databaseId,
                tableId, timestamp, page, size);
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
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed find table data: authentication required");
                throw new NotAllowedException("Failed to find table data: authentication required");
            }
            credentialService.getAccess(databaseId, UserUtil.getId(principal));
        }
        try {
            final HttpHeaders headers = new HttpHeaders();
            if (request.getMethod().equals("HEAD")) {
                headers.set("Access-Control-Expose-Headers", "X-Count");
                headers.set("X-Count", "" + tableService.getCount(table, timestamp));
                return ResponseEntity.ok()
                        .headers(headers)
                        .build();
            }
            headers.set("Access-Control-Expose-Headers", "X-Headers");
            headers.set("X-Headers", String.join(",", table.getColumns().stream().map(ColumnDto::getInternalName).toList()));
            final Dataset<Row> dataset = tableService.getData(table.getDatabase(), table.getInternalName(), timestamp,
                    null, null, null, null);
            metricsService.countTableGetData(databaseId, tableId);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(transform(dataset));
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Create table data not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database or blob in storage service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service or storage service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> insertRawTuple(@NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("tableId") Long tableId,
                                               @Valid @RequestBody TupleDto data,
                                               @NotNull Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException {
        log.debug("endpoint insert raw table data, databaseId={}, tableId={}", databaseId, tableId);
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = credentialService.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        try {
            tableService.createTuple(table, data);
            metadataServiceGateway.updateTableStatistics(databaseId, tableId);
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Update table data not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> updateRawTuple(@NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("tableId") Long tableId,
                                               @Valid @RequestBody TupleUpdateDto data,
                                               @NotNull Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, MetadataServiceException {
        log.debug("endpoint update raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = credentialService.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        try {
            tableService.updateTuple(table, data);
            metadataServiceGateway.updateTableStatistics(databaseId, tableId);
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Delete table data not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> deleteRawTuple(@NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("tableId") Long tableId,
                                               @Valid @RequestBody TupleDeleteDto data,
                                               @NotNull Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException, MetadataServiceException {
        log.debug("endpoint delete raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = credentialService.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        try {
            tableService.deleteTuple(table, data);
            metadataServiceGateway.updateTableStatistics(databaseId, tableId);
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Find table history not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table history in data database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableHistoryDto>> getHistory(@NotNull @PathVariable("databaseId") Long databaseId,
                                                            @NotNull @PathVariable("tableId") Long tableId,
                                                            @RequestParam(value = "size", required = false) Long size,
                                                            Principal principal) throws DatabaseUnavailableException,
            RemoteUnavailableException, TableNotFoundException, NotAllowedException, MetadataServiceException,
            PaginationException {
        log.debug("endpoint find table history, databaseId={}, tableId={}", databaseId, tableId);
        if (size != null && size <= 0) {
            log.error("Invalid size: must be > 0");
            throw new PaginationException("Invalid size: must be bigger than zero");
        } else if (size == null) {
            log.debug("size not set: default to 100L");
            size = 100L;
        }
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to find table history: no authentication found");
                throw new NotAllowedException("Failed to find table history: no authentication found");
            }
            credentialService.getAccess(databaseId, UserUtil.getId(principal));
        }
        try {
            final List<TableHistoryDto> dto = tableService.history(table, size);
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Find table schema not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to parse table schema",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableDto>> getSchema(@NotNull @PathVariable("databaseId") Long databaseId)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            DatabaseMalformedException, TableNotFoundException, MetadataServiceException {
        log.debug("endpoint inspect table schemas, databaseId={}", databaseId);
        final PrivilegedDatabaseDto database = credentialService.getDatabase(databaseId);
        try {
            return ResponseEntity.ok(tableService.getSchemas(database));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{tableId}/export")
    @Observed(name = "dbrepo_table_data_export")
    @Operation(summary = "Get table data",
            description = "Gets data from table with id as downloadable file. For tables in private databases, the user needs to have at least *READ* access to the associated database.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Exported table data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InputStreamResource.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Export table data not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<InputStreamResource> exportDataset(@NotNull @PathVariable("databaseId") Long databaseId,
                                                             @NotNull @PathVariable("tableId") Long tableId,
                                                             @RequestParam(required = false) Instant timestamp,
                                                             Principal principal)
            throws RemoteUnavailableException, TableNotFoundException, NotAllowedException, StorageUnavailableException,
            QueryMalformedException, MetadataServiceException {
        log.debug("endpoint export table data, databaseId={}, tableId={}, timestamp={}", databaseId, tableId, timestamp);
        /* parameters */
        if (timestamp == null) {
            timestamp = Instant.now();
            log.debug("timestamp not set: default to {}", timestamp);
        }
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to export private table: principal is null");
                throw new NotAllowedException("Failed to export private table: principal is null");
            }
            credentialService.getAccess(databaseId, UserUtil.getId(principal));
        }
        final Dataset<Row> dataset = tableService.getData(table.getDatabase(), table.getInternalName(), timestamp, null,
                null, null, null);
        metricsService.countTableGetData(databaseId, tableId);
        final ExportResourceDto resource = storageService.transformDataset(dataset);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        log.trace("export table resulted in resource {}", resource);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Import table dataset not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> importDataset(@NotNull @PathVariable("databaseId") Long databaseId,
                                              @NotNull @PathVariable("tableId") Long tableId,
                                              @Valid @RequestBody ImportDto data,
                                              @NotNull Principal principal)
            throws RemoteUnavailableException, TableNotFoundException, NotAllowedException, MetadataServiceException,
            StorageNotFoundException, MalformedException, StorageUnavailableException, QueryMalformedException,
            DatabaseUnavailableException {
        log.debug("endpoint insert table data, databaseId={}, tableId={}, data.location={}", databaseId, tableId, data.getLocation());
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        final DatabaseAccessDto access = credentialService.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        if (data.getLineTermination() == null) {
            data.setLineTermination("\\r\\n");
            log.debug("line termination not present, default to {}", data.getLineTermination());
        }
        try {
            tableService.importDataset(table, data);
        } catch (SQLException | TableMalformedException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
        metadataServiceGateway.updateTableStatistics(databaseId, tableId);
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find table or database in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<TableStatisticDto> statistic(@NotNull @PathVariable("databaseId") Long databaseId,
                                                       @NotNull @PathVariable("tableId") Long tableId)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, TableMalformedException {
        log.debug("endpoint generate table statistic, databaseId={}, tableId={}", databaseId, tableId);
        final PrivilegedTableDto table = credentialService.getTable(databaseId, tableId);
        try {
            return ResponseEntity.ok(tableService.getStatistics(table));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
