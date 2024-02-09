package at.tuwien.endpoints;

import at.tuwien.SortType;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.utils.PrincipalUtil;
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
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/database/{databaseId}/table/{tableId}/data")
public class TableDataEndpoint {

    private final QueryService queryService;
    private final DatabaseService databaseService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public TableDataEndpoint(QueryService queryService, DatabaseService databaseService,
                             EndpointValidator endpointValidator) {
        this.queryService = queryService;
        this.databaseService = databaseService;
        this.endpointValidator = endpointValidator;
    }

    @PostMapping
    @Transactional
    @Observed(name = "dbr_table_data_insert")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Operation(summary = "Insert data", description = "Insert data directly as key-value map tuple",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Inserted data successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Insert table data is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> insert(@NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("tableId") Long tableId,
                                    @NotNull @Valid @RequestBody TableCsvDto data,
                                    @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException, NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint insert data, databaseId={}, tableId={}, data={}, {}", databaseId, tableId, data, PrincipalUtil.formatForDebug(principal));
        /* check */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(databaseId, tableId, principal);
        /* insert */
        queryService.insert(databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @DeleteMapping
    @Transactional
    @PreAuthorize("hasAuthority('delete-table-data')")
    @Observed(name = "dbr_table_data_delete")
    @Operation(summary = "Delete data", description = "Delete a tuples that match a key-value map",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted table data successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Table data or query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDeleteDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, QueryMalformedException, NotAllowedException, AccessDeniedException {
        log.debug("endpoint delete data, databaseId={}, tableId={}, data={}, {}", databaseId, tableId, data, PrincipalUtil.formatForDebug(principal));
        /* check */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(databaseId, tableId, principal);
        /* delete */
        queryService.delete(databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @PostMapping("/import")
    @Transactional
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Observed(name = "dbr_table_data_import")
    @Operation(summary = "Insert data from csv", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Import table data successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Table data is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Import failed in sidecar",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Could not import csv via sidecar",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> importCsv(@NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @NotNull @Valid @RequestBody ImportDto data,
                                          @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            NotAllowedException, AccessDeniedException, DataDbSidecarException, DataProcessingException {
        log.debug("endpoint insert data from csv, databaseId={}, tableId={}, data={}, {}", databaseId, tableId, data, PrincipalUtil.formatForDebug(principal));
        /* check */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(databaseId, tableId, principal);
        /* insert */
        queryService.insert(databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Observed(name = "dbr_table_data_findall")
    @Operation(summary = "Find data", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Get table data successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Table data is malformed or image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Could not import csv via sidecar",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryResultDto> getAll(@NotNull @PathVariable("databaseId") Long databaseId,
                                                 @NotNull @PathVariable("tableId") Long tableId,
                                                 @NotNull Principal principal,
                                                 @RequestParam(required = false) Instant timestamp,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long size,
                                                 @RequestParam(required = false) SortType sortDirection,
                                                 @RequestParam(required = false) String sortColumn)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, PaginationException, QueryMalformedException, SortException, NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint find table data, databaseId={}, tableId={}, timestamp={}, page={}, size={}, sortDirection={}, sortColumn={}, {}",
                databaseId, tableId, timestamp, page, size, sortDirection, sortColumn, PrincipalUtil.formatForDebug(principal));
        /* check */
        endpointValidator.validateDataParams(page, size, sortDirection, sortColumn);
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        final Database database = databaseService.find(databaseId);
        if (!database.getIsPublic() && !UserUtil.hasRole(principal, "view-table-data")) {
            log.error("Failed to view table data: database with id {} is private and user has no authority", databaseId);
            throw new NotAllowedException("Failed to view table data: database with id " + databaseId + " is private and user has no authority");
        }
        /* default */
        if (page == null) {
            log.trace("page is null: default to 0");
            page = 0L;
        }
        if (size == null) {
            log.trace("size is null: default to 10");
            size = 10L;
        }
        /* find */
        final QueryResultDto response = queryService.tableFindAll(databaseId, tableId, timestamp, page, size, principal);
        log.trace("find table data resulted in result {}", response);
        return ResponseEntity.ok()
                .body(response);
    }

    @GetMapping("/count")
    @Transactional(readOnly = true)
    @Observed(name = "dbr_table_data_countall")
    @Operation(summary = "Find data", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Get table data count successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Table data is malformed or image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Could not import csv via sidecar",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Long> getCount(@NotNull @PathVariable("databaseId") Long databaseId,
                                         @NotNull @PathVariable("tableId") Long tableId,
                                         @NotNull Principal principal,
                                         @RequestParam(required = false) Instant timestamp)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, QueryStoreException, QueryMalformedException, NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint find table data, databaseId={}, tableId={}, timestamp={}, {}", databaseId, tableId, timestamp, PrincipalUtil.formatForDebug(principal));
        /* check */
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        final Database database = databaseService.find(databaseId);
        if (!database.getIsPublic() && !UserUtil.hasRole(principal, "view-table-data")) {
            log.error("Failed to view table data: database with id {} is private and user has no authority", databaseId);
            throw new NotAllowedException("Failed to view table data: database with id " + databaseId + " is private and user has no authority");
        }
        /* find */
        final Long count = queryService.tableCount(databaseId, tableId, timestamp, principal);
        log.debug("find table data count resulted in {} tuple(s)", count);
        return ResponseEntity.ok()
                .body(count);
    }

}
