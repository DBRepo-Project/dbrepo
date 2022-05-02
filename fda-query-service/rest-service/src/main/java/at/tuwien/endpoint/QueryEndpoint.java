package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.api.database.query.*;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.SQLException;

@Log4j2
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/query")
public class QueryEndpoint {

    private final QueryMapper queryMapper;
    private final QueryService queryService;
    private final StoreService storeService;

    @Autowired
    public QueryEndpoint(QueryMapper queryMapper, QueryService queryService, StoreService storeService) {
        this.queryMapper = queryMapper;
        this.queryService = queryService;
        this.storeService = storeService;
    }

    @PutMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Execute query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> execute(@NotNull @PathVariable("id") Long id,
                                                  @NotNull @PathVariable("databaseId") Long databaseId,
                                                  @Valid @RequestBody ExecuteStatementDto data,
                                                  @RequestParam(value = "page", required = false) Long page,
                                                  @RequestParam(value = "size", required = false) Long size)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException, QueryMalformedException,
            TableNotFoundException, ContainerNotFoundException, SQLException, JSQLParserException, TableMalformedException {
        /* validation */
        if (data.getStatement() == null || data.getStatement().isBlank()) {
            log.error("Query is empty");
            throw new QueryMalformedException("Invalid query");
        }
        log.debug("Data for execution: {}", data);
        final QueryResultDto result = queryService.execute(id, databaseId, data, page, size);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @PutMapping("/{queryId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Re-execute some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> reExecute(@NotNull @PathVariable("id") Long id,
                                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                                    @NotNull @PathVariable("queryId") Long queryId,
                                                    @RequestParam(value = "page", required = false) Long page, @RequestParam(value = "size", required = false) Long size)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableNotFoundException, QueryMalformedException, ContainerNotFoundException, SQLException, JSQLParserException, TableMalformedException {
        final Query query = storeService.findOne(id, databaseId, queryId);
        final QueryResultDto result = queryService.reExecute(id, databaseId, query, page, size);
        result.setId(queryId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/export")
    @Transactional(readOnly = true)
    @Operation(summary = "Exports some query")
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long id,
                                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                                    @NotNull @PathVariable("queryId") Long queryId)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, TableMalformedException, FileStorageException {
        final Query query = storeService.findOne(id, databaseId, queryId);
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = queryService.findOne(id, databaseId, queryId);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }

}
