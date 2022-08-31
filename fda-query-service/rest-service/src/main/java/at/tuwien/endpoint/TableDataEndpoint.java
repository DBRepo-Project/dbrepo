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
                             IdentifierService identifierService) {
        super(databaseService, identifierService);
        this.queryService = queryService;
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Insert data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> insert(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, NotAllowedException, DatabaseConnectionException {
        if (!hasDatabasePermission(containerId, databaseId, "DATA_INSERT", principal)) {
            log.error("Missing data insert permission");
            throw new NotAllowedException("Missing data insert permission");
        }
        queryService.insert(containerId, databaseId, tableId, data);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Update data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> update(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvUpdateDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, NotAllowedException, DatabaseConnectionException, QueryMalformedException {
        if (!hasDatabasePermission(containerId, databaseId, "DATA_UPDATE", principal)) {
            log.error("Missing data update permission");
            throw new NotAllowedException("Missing data update permission");
        }
        queryService.update(containerId, databaseId, tableId, data);
        return ResponseEntity.accepted()
                .build();
    }

    @DeleteMapping
    @Transactional
    @Operation(summary = "Delete data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@NotNull @PathVariable("id") Long containerId,
                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                       @NotNull @PathVariable("tableId") Long tableId,
                                       @NotNull @Valid @RequestBody TableCsvDeleteDto data,
                                       @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, TupleDeleteException, NotAllowedException, ContainerNotFoundException,
            DatabaseConnectionException, QueryMalformedException {
        if (!hasDatabasePermission(containerId, databaseId, "DATA_DELETE", principal)) {
            log.error("Missing data delete permission");
            throw new NotAllowedException("Missing data delete permission");
        }
        queryService.delete(containerId, databaseId, tableId, data);
        return ResponseEntity.accepted()
                .build();
    }

    @PostMapping("/import")
    @Transactional
    @Operation(summary = "Insert data from csv", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> importCsv(@NotNull @PathVariable("id") Long containerId,
                                          @NotNull @PathVariable("databaseId") Long databaseId,
                                          @NotNull @PathVariable("tableId") Long tableId,
                                          @NotNull @Valid @RequestBody ImportDto data,
                                          @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, NotAllowedException, DatabaseConnectionException,
            QueryMalformedException {
        if (!hasDatabasePermission(containerId, databaseId, "DATA_INSERT", principal)) {
            log.error("Missing data insert permission");
            throw new NotAllowedException("Missing data insert permission");
        }
        log.info("Insert data into database with id {}", databaseId);
        log.debug("insert data from location {} into database with id {}", data, databaseId);
        queryService.insert(containerId, databaseId, tableId, data);
        return ResponseEntity.accepted()
                .build();
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Operation(summary = "Find data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> getAll(@NotNull @PathVariable("id") Long containerId,
                                                 @NotNull @PathVariable("databaseId") Long databaseId,
                                                 @NotNull @PathVariable("tableId") Long tableId,
                                                 @RequestParam(required = false) Instant timestamp,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long size,
                                                 @NotNull Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, DatabaseConnectionException,
            ImageNotSupportedException, TableMalformedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException {
        if (!hasDatabasePermission(containerId, databaseId, "DATA_VIEW", principal)) {
            log.error("Missing data view permission");
            throw new NotAllowedException("Missing data view permission");
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
        final Long count = queryService.count(containerId, databaseId, tableId, timestamp);
        final HttpHeaders headers = new HttpHeaders();
        headers.set("FDA-COUNT", count.toString());
        final QueryResultDto response = queryService.findAll(containerId, databaseId, tableId, timestamp, page, size);
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }


}
