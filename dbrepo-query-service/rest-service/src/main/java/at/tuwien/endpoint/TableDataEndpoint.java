package at.tuwien.endpoint;

import at.tuwien.SortType;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.time.Instant;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/data")
public class TableDataEndpoint extends AbstractEndpoint {

    private final QueryService queryService;

    @Autowired
    public TableDataEndpoint(QueryService queryService, DatabaseService databaseService,
                             IdentifierService identifierService, TableService tableService,
                             AccessService accessService, QueryConfig queryConfig) {
        super(tableService, accessService, databaseService, identifierService, queryConfig);
        this.queryService = queryService;
    }

    @PostMapping
    @Transactional
    @Timed(value = "data.insert", description = "Time needed to insert data into a table")
    @Operation(summary = "Insert data", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Insert data successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Insert data is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Insertion of data resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> insert(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, NotAllowedException, DatabaseConnectionException,
            UserNotFoundException {
        log.debug("endpoint insert data, containerId={}, databaseId={}, tableId={}, data={}, principal={}", containerId,
                databaseId, tableId, data, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "DATA_INSERT", principal)) {
            log.error("Missing data insert permission");
            throw new NotAllowedException("Missing data insert permission");
        }
        queryService.insert(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping
    @Transactional
    @Deprecated
    @Timed(value = "data.update", description = "Time needed to update data in a table")
    @Operation(summary = "Update data", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Update data successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Update data is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Update of data resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> update(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvUpdateDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            UserNotFoundException {
        log.debug("endpoint update data, containerId={}, databaseId={}, tableId={}, data={}, principal={}", containerId,
                databaseId, tableId, data, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "DATA_UPDATE", principal)) {
            log.error("Missing data update permission");
            throw new NotAllowedException("Missing data update permission");
        }
        queryService.update(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @DeleteMapping
    @Transactional
    @Timed(value = "data.delete", description = "Time needed to delete data into a table")
    @Operation(summary = "Delete data", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Delete data successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "400",
                    description = "Deletion query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Delete data is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Delete of data resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDeleteDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, NotAllowedException, ContainerNotFoundException,
            DatabaseConnectionException, QueryMalformedException, UserNotFoundException {
        log.debug("endpoint delete data, containerId={}, databaseId={}, tableId={}, data={}, principal={}", containerId,
                databaseId, tableId, data, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "DATA_DELETE", principal)) {
            log.error("Missing data delete permission");
            throw new NotAllowedException("Missing data delete permission");
        }
        queryService.delete(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @PostMapping("/import")
    @Transactional
    @Timed(value = "data.insertbulk", description = "Time needed to insert data from .csv into a table")
    @Operation(summary = "Insert data from csv", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Insert data from csv successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "400",
                    description = "Insert data from csv query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Insert data from csv is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Insert data from csv resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> importCsv(@NotNull @PathVariable("id") Long containerId,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @NotNull @Valid @RequestBody ImportDto data,
                                          @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, NotAllowedException, DatabaseConnectionException,
            QueryMalformedException, UserNotFoundException {
        log.debug("endpoint insert data from csv, containerId={}, databaseId={}, tableId={}, data={}, principal={}",
                containerId, databaseId, tableId, data, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "DATA_INSERT", principal)) {
            log.error("Missing data insert permission");
            throw new NotAllowedException("Missing data insert permission");
        }
        queryService.insert(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Timed(value = "data.all", description = "Time needed to find all data from a table")
    @Operation(summary = "Find data", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find data successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Pagination not within valid range or select query is malformed or sort is invalid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find data is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Query store failed to query result data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryResultDto> data(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("tableId") Long tableId,
                                               @NotNull Principal principal,
                                               @RequestParam(required = false) Instant timestamp,
                                               @RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size,
                                               @RequestParam(required = false) SortType sortDirection,
                                               @RequestParam(required = false) String sortColumn)
            throws TableNotFoundException, DatabaseNotFoundException, DatabaseConnectionException,
            ImageNotSupportedException, TableMalformedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {
        log.debug("endpoint find table data, containerId={}, databaseId={}, tableId={}, principal={}, timestamp={}, page={}, size={}, sortDirection={}, sortColumn={}",
                containerId, databaseId, tableId, principal, timestamp, page, size, sortDirection, sortColumn);
        /* check */
        if (!hasTablePermission(containerId, databaseId, tableId, "DATA_VIEW", principal)) {
            log.error("Missing data view permission");
            throw new NotAllowedException("Missing data view permission");
        }
        validateDataParams(page, size, sortDirection, sortColumn);
        /* find */
        final Long count = queryService.count(containerId, databaseId, tableId, timestamp, principal);
        log.debug("find table data has produced {} tuples", count);
        final HttpHeaders headers = new HttpHeaders();
        headers.set("FDA-COUNT", count.toString());
        final QueryResultDto response = queryService.findAll(containerId, databaseId, tableId, timestamp, page, size, principal);
        log.trace("find table data resulted in result {}", response);
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }


}
