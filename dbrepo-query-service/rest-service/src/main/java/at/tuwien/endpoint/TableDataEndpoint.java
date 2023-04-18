package at.tuwien.endpoint;

import at.tuwien.SortType;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class TableDataEndpoint {

    private final QueryService queryService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public TableDataEndpoint(QueryService queryService, EndpointValidator endpointValidator) {
        this.queryService = queryService;
        this.endpointValidator = endpointValidator;
    }

    @PostMapping
    @Transactional
    @Timed(value = "data.insert", description = "Time needed to insert data into a table")
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Operation(summary = "Insert data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> insert(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, DatabaseConnectionException,
            UserNotFoundException {
        log.debug("endpoint insert data, containerId={}, databaseId={}, tableId={}, data={}, principal={}", containerId,
                databaseId, tableId, data, principal);
        queryService.insert(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping
    @Transactional
    @Deprecated
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Timed(value = "data.update", description = "Time needed to update data in a table")
    @Operation(summary = "Update data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> update(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvUpdateDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, DatabaseConnectionException, QueryMalformedException,
            UserNotFoundException {
        log.debug("endpoint update data, containerId={}, databaseId={}, tableId={}, data={}, principal={}", containerId,
                databaseId, tableId, data, principal);
        queryService.update(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @DeleteMapping
    @Transactional
    @PreAuthorize("hasAuthority('delete-table-data')")
    @Timed(value = "data.delete", description = "Time needed to delete data into a table")
    @Operation(summary = "Delete data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDeleteDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException,
            DatabaseConnectionException, QueryMalformedException, UserNotFoundException {
        log.debug("endpoint delete data, containerId={}, databaseId={}, tableId={}, data={}, principal={}", containerId,
                databaseId, tableId, data, principal);
        queryService.delete(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @PostMapping("/import")
    @Transactional
    @PreAuthorize("hasAuthority('insert-table-data')")
    @Timed(value = "data.insertbulk", description = "Time needed to insert data from .csv into a table")
    @Operation(summary = "Insert data from csv", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> importCsv(@NotNull @PathVariable("id") Long containerId,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @NotNull @Valid @RequestBody ImportDto data,
                                          @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, DatabaseConnectionException,
            QueryMalformedException, UserNotFoundException {
        log.debug("endpoint insert data from csv, containerId={}, databaseId={}, tableId={}, data={}, principal={}",
                containerId, databaseId, tableId, data, principal);
        queryService.insert(containerId, databaseId, tableId, data, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Timed(value = "data.all", description = "Time needed to find all data from a table")
    @Operation(summary = "Find data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> getAll(@NotNull @PathVariable("id") Long containerId,
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
            QueryMalformedException, UserNotFoundException, SortException {
        log.debug("endpoint find table data, containerId={}, databaseId={}, tableId={}, principal={}, timestamp={}, page={}, size={}, sortDirection={}, sortColumn={}",
                containerId, databaseId, tableId, principal, timestamp, page, size, sortDirection, sortColumn);
        /* check */
        endpointValidator.validateDataParams(page, size, sortDirection, sortColumn);
        final QueryResultDto response = queryService.tableFindAll(containerId, databaseId, tableId, timestamp, page, size, principal);
        log.trace("find table data resulted in result {}", response);
        return ResponseEntity.ok()
                .body(response);
    }

    @GetMapping("/count")
    @Timed(value = "data.all.count", description = "Time needed to get count of all data from a table")
    @Operation(summary = "Find data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Long> getCount(@NotNull @PathVariable("id") Long containerId,
                                                 @NotNull @PathVariable("databaseId") Long databaseId,
                                                 @NotNull @PathVariable("tableId") Long tableId,
                                                 @NotNull Principal principal,
                                                 @RequestParam(required = false) Instant timestamp)
            throws TableNotFoundException, DatabaseNotFoundException, DatabaseConnectionException,
            ImageNotSupportedException, TableMalformedException, ContainerNotFoundException,
            QueryStoreException, QueryMalformedException, UserNotFoundException {
        log.debug("endpoint find table data, containerId={}, databaseId={}, tableId={}, principal={}, timestamp={}",
                containerId, databaseId, tableId, principal, timestamp);
        /* find */
        final Long count = queryService.tableCount(containerId, databaseId, tableId, timestamp, principal);
        log.debug("table data count is {} tuples", count);
        return ResponseEntity.ok()
                .body(count);
    }

}
