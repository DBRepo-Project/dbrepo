package at.tuwien.endpoints;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.QueryService;
import at.tuwien.service.StorageService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/subset")
public class SubsetEndpoint {

    private final QueryService queryService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public SubsetEndpoint(QueryService queryService, MetadataServiceGateway metadataServiceGateway) {
        this.queryService = queryService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @GetMapping
    @Transactional(rollbackFor = Exception.class)
    @Observed(name = "dbr_database_create")
    @Operation(summary = "Find subsets", security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found subsets",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
    })
    public ResponseEntity<List<QueryDto>> findById(@NotNull @PathVariable("databaseId") Long databaseId,
                                                   @RequestParam(name = "persisted", required = false) Boolean filterPersisted)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException {
        log.debug("endpoint create view, databaseId={}, persisted={}", databaseId, filterPersisted);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final List<QueryDto> queries;
        try {
            queries = queryService.findAll(database, filterPersisted);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        log.info("Found {} queries in data database", queries.size());
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/{subsetId}")
    @Transactional(rollbackFor = Exception.class)
    @Observed(name = "dbr_database_create")
    @Operation(summary = "Find subset", security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
    })
    public ResponseEntity<?> findById(@NotNull @PathVariable("databaseId") Long databaseId,
                                      @NotNull @PathVariable("subsetId") Long subsetId,
                                      @RequestHeader(HttpHeaders.ACCEPT) String accept,
                                      @RequestParam(required = false) Instant timestamp)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, FormatNotAvailableException, StorageUnavailableException, QueryMalformedException,
            SidecarExportException, StorageNotFoundException {
        log.debug("endpoint create view, databaseId={}, subsetId={}", databaseId, subsetId);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final QueryDto query;
        try {
            query = queryService.findById(database, subsetId);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        if (accept != null) {
            log.trace("accept header present: {}", accept);
            switch (accept) {
                case "application/json":
                    log.trace("accept header matches json");
                    return ResponseEntity.ok(query);
                case "text/csv":
                    log.trace("accept header matches csv");
                    final String filename = RandomStringUtils.randomAlphabetic(20).toLowerCase();
                    try {
                        final ExportResourceDto resource = queryService.export(database, query, timestamp, filename);
                        return ResponseEntity.ok(resource);
                    } catch (SQLException e) {
                        log.error("Failed to establish connection to database: {}", e.getMessage());
                        throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
                    }
            }
        }
        throw new FormatNotAvailableException("Must provide either application/json or text/csv headers");
    }

}
