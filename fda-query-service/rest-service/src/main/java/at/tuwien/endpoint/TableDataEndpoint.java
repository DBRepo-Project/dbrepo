package at.tuwien.endpoint;

import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.exception.*;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
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
import java.time.Instant;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/data")
public class TableDataEndpoint {

    private final QueryService queryService;
    private final StoreService storeService;

    @Autowired
    public TableDataEndpoint(QueryService queryService, StoreService storeService) {
        this.queryService = queryService;
        this.storeService = storeService;
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Insert data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Integer> insert(@NotNull @PathVariable("id") Long id,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @Valid @RequestBody TableCsvDto data)
            throws TableNotFoundException, DatabaseNotFoundException, FileStorageException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException {
        return ResponseEntity.accepted()
                .body(queryService.insert(id, databaseId, tableId, data));
    }

    @PutMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Update data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Integer> update(@NotNull @PathVariable("id") Long id,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @Valid @RequestBody TableCsvUpdateDto data)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException {
        return ResponseEntity.accepted()
                .body(queryService.update(id, databaseId, tableId, data));
    }

    @DeleteMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Delete data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@NotNull @PathVariable("id") Long id,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @Valid @RequestBody TableCsvDeleteDto data)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, TupleDeleteException {
        queryService.delete(id, databaseId, tableId, data);
        return ResponseEntity.accepted()
                .build();
    }

    @PostMapping("/import")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Insert data from csv", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Integer> importCsv(@NotNull @PathVariable("id") Long id,
                                             @NotNull @PathVariable("databaseId") Long databaseId,
                                             @NotNull @PathVariable("tableId") Long tableId,
                                             @Valid @RequestBody ImportDto data)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException {
        log.info("Insert data from location {} into database id {}", data, databaseId);
        return ResponseEntity.accepted()
                .body(queryService.insert(id, databaseId, tableId, data));
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Operation(summary = "Find data")
    public ResponseEntity<QueryResultDto> getAll(@NotNull @PathVariable("id") Long id,
                                                 @NotNull @PathVariable("databaseId") Long databaseId,
                                                 @NotNull @PathVariable("tableId") Long tableId,
                                                 @RequestParam(required = false) Instant timestamp,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long size)
            throws TableNotFoundException, DatabaseNotFoundException, DatabaseConnectionException,
            ImageNotSupportedException, TableMalformedException, PaginationException, ContainerNotFoundException,
            QueryStoreException {
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
        storeService.findAll(id, databaseId);
        final BigInteger count = queryService.count(id, databaseId, tableId, timestamp);
        final HttpHeaders headers = new HttpHeaders();
        headers.set("FDA-COUNT", count.toString());
        final QueryResultDto response = queryService.findAll(id, databaseId, tableId, timestamp, page, size);
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }


}
