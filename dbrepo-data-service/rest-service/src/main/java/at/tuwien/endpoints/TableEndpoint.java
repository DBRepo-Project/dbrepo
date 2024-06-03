package at.tuwien.endpoints;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.AnalyseService;
import at.tuwien.service.TableService;
import at.tuwien.utils.UserUtil;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/table")
public class TableEndpoint {

    private final TableService tableService;
    private final AnalyseService analyseService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public TableEndpoint(TableService tableService, AnalyseService analyseService, EndpointValidator endpointValidator,
                         MetadataServiceGateway metadataServiceGateway) {
        this.tableService = tableService;
        this.analyseService = analyseService;
        this.endpointValidator = endpointValidator;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Create table", security = {@SecurityRequirement(name = "basicAuth")})
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
                    description = "Failed to find database in metadata database or table in data database",
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
            TableNotFoundException, QueryMalformedException {
        log.debug("endpoint create table, databaseId={}, data.name={}", databaseId, data.getName());
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(tableService.createTable(database, data));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{tableId}")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Delete table", security = {@SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<Void> delete(@NotBlank @PathVariable("databaseId") Long databaseId,
                                       @NotBlank @PathVariable("tableId") Long tableId)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            QueryMalformedException {
        log.debug("endpoint delete table, databaseId={}, tableId={}", databaseId, tableId);
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
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
    @Operation(summary = "Retrieve table data", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved table data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or table data select query is malformed",
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
    public ResponseEntity<QueryResultDto> getData(@NotBlank @PathVariable("databaseId") Long databaseId,
                                                  @NotBlank @PathVariable("tableId") Long tableId,
                                                  @RequestParam(required = false) Instant timestamp,
                                                  @RequestParam(required = false) Long page,
                                                  @RequestParam(required = false) Long size)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, PaginationException, QueryMalformedException {
        log.debug("endpoint find table data, databaseId={}, tableId={}, timestamp={}, page={}, size={}", databaseId,
                tableId, timestamp, page, size);
        endpointValidator.validateDataParams(page, size);
        /* parameters */
        if (page == null) {
            log.debug("page not set: default to 0");
            page = 0L;
        }
        if (size == null) {
            log.debug("size not set: default to 10");
            size = 10L;
        }
        if (timestamp == null) {
            log.debug("timestamp not set: default to now");
            timestamp = Instant.now();
        }
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Expose-Headers", "X-Count");
        try {
            headers.set("X-Count", "" + tableService.getCount(table, timestamp));
            final QueryResultDto dto = tableService.getData(table, timestamp, page, size);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(dto);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{tableId}/data")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Observed(name = "dbrepo_table_data_create")
    @Operation(summary = "Insert a raw data tuple",
            description = "Inserts a raw data tuple into a table with at least WRITE_OWN access. Then update the table statistics.",
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
    public ResponseEntity<Void> insertRawTuple(@NotBlank @PathVariable("databaseId") Long databaseId,
                                               @NotBlank @PathVariable("tableId") Long tableId,
                                               @Valid @RequestBody TupleDto data,
                                               @NotNull Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException {
        log.debug("endpoint insert raw table data, databaseId={}, tableId={}", databaseId, tableId);
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
        final DatabaseAccessDto access = metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        try {
            tableService.createTuple(table, data);
            final TableStatisticDto statistics = analyseService.analyseTable(databaseId, tableId);
            metadataServiceGateway.updateTableStatistics(databaseId, tableId, statistics);
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
    @Operation(summary = "Update a raw data tuple",
            description = "Updates a raw data tuple in a table with at least WRITE_OWN access. Then update the table statistics.",
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
    public ResponseEntity<Void> updateRawTuple(@NotBlank @PathVariable("databaseId") Long databaseId,
                                               @NotBlank @PathVariable("tableId") Long tableId,
                                               @Valid @RequestBody TupleUpdateDto data,
                                               @NotNull Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException {
        log.debug("endpoint update raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
        final DatabaseAccessDto access = metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        try {
            tableService.updateTuple(table, data);
            final TableStatisticDto statistics = analyseService.analyseTable(databaseId, tableId);
            metadataServiceGateway.updateTableStatistics(databaseId, tableId, statistics);
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
    @Operation(summary = "Delete table data",
            description = "Deletes a raw data tuple in a table with at least WRITE_OWN access. Then update the table statistics.",
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
    public ResponseEntity<Void> deleteRawTuple(@NotBlank @PathVariable("databaseId") Long databaseId,
                                               @NotBlank @PathVariable("tableId") Long tableId,
                                               @Valid @RequestBody TupleDeleteDto data,
                                               @NotNull Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            TableMalformedException, QueryMalformedException, NotAllowedException {
        log.debug("endpoint delete raw table data, databaseId={}, tableId={}, data.keys={}", databaseId, tableId,
                data.getKeys());
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
        final DatabaseAccessDto access = metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        try {
            tableService.deleteTuple(table, data);
            final TableStatisticDto statistics = analyseService.analyseTable(databaseId, tableId);
            metadataServiceGateway.updateTableStatistics(databaseId, tableId, statistics);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{tableId}/history")
    @Observed(name = "dbrepo_table_data_history")
    @Operation(summary = "Find table history", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found table history",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableHistoryDto[].class))}),
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
    public ResponseEntity<List<TableHistoryDto>> getHistory(@NotBlank @PathVariable("databaseId") Long databaseId,
                                                            @NotBlank @PathVariable("tableId") Long tableId,
                                                            Principal principal) throws DatabaseUnavailableException,
            RemoteUnavailableException, TableNotFoundException, NotAllowedException {
        log.debug("endpoint find table history, databaseId={}, tableId={}", databaseId, tableId);
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
        if (!table.getIsPublic() && principal == null) {
            log.error("Failed to find table history: no authentication found");
            throw new NotAllowedException("Failed to find table history: no authentication found");
        }
        metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        try {
            final List<TableHistoryDto> dto = tableService.history(table);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(dto);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Find table schemas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Got table schemas",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableDto[].class))}),
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
    public ResponseEntity<List<TableDto>> getSchema(@NotBlank @PathVariable("databaseId") Long databaseId)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            DatabaseMalformedException, TableNotFoundException, QueryMalformedException {
        log.debug("endpoint inspect table schemas, databaseId={}", databaseId);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        try {
            return ResponseEntity.ok(tableService.getSchemas(database));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{tableId}/export")
    @Observed(name = "dbrepo_table_data_export")
    @Operation(summary = "Export table data", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
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
    public ResponseEntity<InputStreamResource> exportData(@NotBlank @PathVariable("databaseId") Long databaseId,
                                                          @NotBlank @PathVariable("tableId") Long tableId,
                                                          @RequestParam(required = false) Instant timestamp,
                                                          Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            NotAllowedException, StorageUnavailableException, QueryMalformedException, SidecarExportException,
            StorageNotFoundException {
        log.debug("endpoint find table history, databaseId={}, tableId={}, timestamp={}", databaseId, tableId, timestamp);
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
        if (!table.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to export private table: principal is null");
                throw new NotAllowedException("Failed to export private table: principal is null");
            }
            metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        }
        /* parameters */
        if (timestamp == null) {
            log.debug("timestamp not set: default to now");
            timestamp = Instant.now();
        }
        try {
            final HttpHeaders headers = new HttpHeaders();
            final ExportResourceDto resource = tableService.exportDataset(table, timestamp);
            headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
            log.trace("export table resulted in resource {}", resource);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource.getResource());

        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

    @PostMapping("/{tableId}/data/import")
    @Observed(name = "dbrepo_table_data_import")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Operation(summary = "Import data from a dataset",
            description = "Deletes a raw data tuple in a table with at least WRITE_OWN access. Then update the table statistics.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Imported dataset successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Dataset query is malformed",
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
    public ResponseEntity<Void> importDataset(@NotBlank @PathVariable("databaseId") Long databaseId,
                                              @NotBlank @PathVariable("tableId") Long tableId,
                                              @Valid @RequestBody ImportCsvDto data,
                                              @NotNull Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            QueryMalformedException, StorageNotFoundException, SidecarImportException, NotAllowedException {
        log.debug("endpoint insert table data, databaseId={}, tableId={}, data.location={}", databaseId, tableId, data.getLocation());
        final PrivilegedTableDto table = metadataServiceGateway.getTableById(databaseId, tableId);
        final DatabaseAccessDto access = metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(access.getType(), table.getOwner().getId(), UserUtil.getId(principal));
        if (data.getNullElement() == null) {
            log.debug("null element not present, default to empty string");
            data.setNullElement("");
        }
        if (data.getLineTermination() == null) {
            log.debug("line termination not present, default to \\r\\n");
            data.setLineTermination("\r\n");
        }
        try {
            tableService.importDataset(table, data);
            final TableStatisticDto statistics = analyseService.analyseTable(databaseId, tableId);
            metadataServiceGateway.updateTableStatistics(databaseId, tableId, statistics);
            return ResponseEntity.accepted()
                    .build();

        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
