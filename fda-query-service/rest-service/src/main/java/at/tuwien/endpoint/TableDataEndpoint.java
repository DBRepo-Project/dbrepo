package at.tuwien.endpoint;

import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.security.Principal;
import java.time.Instant;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/data")
public class TableDataEndpoint extends AbstractEndpoint {

    private final QueryService queryService;
    private final StoreService storeService;

    @Autowired
    public TableDataEndpoint(TableService tableService, QueryService queryService, StoreService storeService,
                             DatabaseService databaseService,
                             IdentifierService identifierService) {
        super(tableService, databaseService, identifierService);
        this.queryService = queryService;
        this.storeService = storeService;
    }

    // FIXME non-trivial authentication for 1) direct JWT coming e.g from swagger 2) indirect service auth coming from
    //  table service 3) direct JWT coming from fda-public network =system
    //  @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @PostMapping
    @Transactional
    @Operation(summary = "Insert data")
    public ResponseEntity<Integer> insert(@NotNull @PathVariable("id") Long containerId,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @NotNull @Valid @RequestBody TableCsvDto data,
                                          @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, FileStorageException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, NotAllowedException {
        if (!hasDatabasePermission(databaseId, tableId, "DATA_INSERT", principal)) {
            throw new NotAllowedException("Insert data not allowed");
        }
        return ResponseEntity.accepted()
                .body(queryService.insert(containerId, databaseId, tableId, data));
    }

    @PutMapping
    @Transactional
    @PreAuthorize("hasPermission(#tableId, 'DATA_UPDATE')")
    @Operation(summary = "Update data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Integer> update(@NotNull @PathVariable("id") Long containerId,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @NotNull @Valid @RequestBody TableCsvUpdateDto data,
                                          @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, NotAllowedException {
        if (!hasDatabasePermission(databaseId, tableId, "DATA_UPDATE", principal)) {
            throw new NotAllowedException("Update data not allowed");
        }
        return ResponseEntity.accepted()
                .body(queryService.update(containerId, databaseId, tableId, data));
    }

    @DeleteMapping
    @Transactional
    @PreAuthorize("hasPermission(#tableId, 'DATA_DELETE')")
    @Operation(summary = "Delete data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDeleteDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, TupleDeleteException, NotAllowedException {
        if (!hasDatabasePermission(databaseId, tableId, "DATA_DELETE", principal)) {
            throw new NotAllowedException("Delete data not allowed");
        }
        queryService.delete(containerId, databaseId, tableId, data);
        return ResponseEntity.accepted()
                .build();
    }

    @PostMapping("/import")
    @Transactional
    @PreAuthorize("hasPermission(#tableId, 'DATA_INSERT')")
    @Operation(summary = "Insert data from csv", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Integer> importCsv(@NotNull @PathVariable("id") Long containerId,
                                             @NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId,
                                             @NotNull @Valid @RequestBody ImportDto data,
                                             @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, NotAllowedException {
        if (!hasDatabasePermission(databaseId, tableId, "DATA_INSERT", principal)) {
            throw new NotAllowedException("Insert data not allowed");
        }
        log.info("Insert data from location {} into database id {}", data, databaseId);
        return ResponseEntity.accepted()
                .body(queryService.insert(containerId, databaseId, tableId, data));
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#tableId, 'DATA_VIEW')")
    @Operation(summary = "Find data")
    public ResponseEntity<QueryResultDto> getAll(@NotNull @PathVariable("id") Long containerId,
                                                 @NotNull @PathVariable("databaseId") Long databaseId,
                                                 @NotNull @PathVariable("tableId") Long tableId,
                                                 @RequestParam(required = false) Instant timestamp,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long size,
                                                 @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, DatabaseConnectionException,
            ImageNotSupportedException, TableMalformedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException {
        if (!hasDatabasePermission(databaseId, tableId, "DATA_VIEW", principal)) {
            throw new NotAllowedException("View data not allowed");
        }
        if ((page == null && size != null) || (page != null && size == null)) {
            log.error("Cannot perform pagination with only one of page/size set.");
            log.debug(
                    "invalid pagination specification, one of page/size is null, either both should be null or none.");
            throw new PaginationException("Invalid pagination parameters");
        }
        if (page != null && page < 0) {
            throw new PaginationException("Page number cannot be lower than 0");
        }
        if (size != null && size <= 0) {
            throw new PaginationException("Page number cannot be lower or equal to 0");
        }
        /* fixme query store maybe not created, create it through running findAll() */
        storeService.findAll(containerId, databaseId);
        final BigInteger count = queryService.count(containerId, databaseId, tableId, timestamp);
        final HttpHeaders headers = new HttpHeaders();
        headers.set("FDA-COUNT", count.toString());
        final QueryResultDto response = queryService.findAll(containerId, databaseId, tableId, timestamp, page, size);
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }


}
