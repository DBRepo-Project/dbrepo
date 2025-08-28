package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TuplesWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.UUID;

import java.util.List;
@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database")
public class DatabaseEndpoint extends RestEndpoint {

    private final DataMapper dataMapper;
    private final CacheService cacheService;
    private final AccessService accessService;
    private final AnalyseService analyseService;
    private final DatabaseService databaseService;
    private final ContainerService containerService;
    private final TableService tableService;

    @Autowired
    public DatabaseEndpoint(DataMapper dataMapper, CacheService cacheService, AccessService accessService,
                            AnalyseService analyseService, DatabaseService databaseService,
                            ContainerService containerService, TableService tableService) {
        this.dataMapper = dataMapper;
        this.cacheService = cacheService;
        this.accessService = accessService;
        this.analyseService = analyseService;
        this.databaseService = databaseService;
        this.containerService = containerService;
        this.tableService = tableService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Create database",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Database create query is malformed or readonly password is hashed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find container in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to create query store in database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseDto> create(@Valid @RequestBody CreateDatabaseDto data)
            throws DatabaseUnavailableException, RemoteUnavailableException, ContainerNotFoundException,
            DatabaseMalformedException, QueryStoreCreateException, MetadataServiceException, MalformedException {
        log.debug("endpoint create database, data.containerId={}, data.internalName={}, data.username={}",
                data.getContainerId(), data.getInternalName(), data.getUsername());
        final ContainerDto container = cacheService.getContainer(data.getContainerId());
        try {
            final DatabaseDto database = containerService.createDatabase(container, data);
            containerService.createQueryStore(container, data.getInternalName());
            accessService.create(database, dataMapper.createDatabaseDtoToUserDto(data), AccessTypeDto.WRITE_ALL);
            accessService.create(database, dataMapper.createDatabaseDtoToPrivilegedUserDto(data), AccessTypeDto.WRITE_ALL);
            if (data.getReadonlyPassword().startsWith("*")) {
                log.error("Failed to give readonly user read-access: password is hashed");
                throw new MalformedException("Failed to give readonly user read-access: password is hashed");
            }
            accessService.create(database, dataMapper.createDatabaseDtoToReadonlyUserDto(data), AccessTypeDto.READ);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(database);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{databaseId}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Update user password",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated user password in database"),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to update user password in database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<Void> update(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @Valid @RequestBody UpdateUserPasswordDto data)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            DatabaseMalformedException, MetadataServiceException {
        log.debug("endpoint update user password in database, databaseId={}, data.username={}", databaseId,
                data.getUsername());
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        try {
            databaseService.update(database, data);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{databaseId}/analyse/schema/{key}")
    @PreAuthorize("hasAuthority('analyse-datatypes')")
    @Operation(summary = "Analyse datatypes of a dataset",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Analysed dataset"),
            @ApiResponse(responseCode = "400",
                    description = "Image datatypes malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Dataset not found",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<SchemaAnalysisResultDto> analyseDatatypes(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                    @PathVariable("key") String key)
            throws AnalyseDataTypesException, DatabaseUnavailableException, StorageNotFoundException,
            RemoteUnavailableException, MetadataServiceException, ImageInvalidException, DatabaseNotFoundException,
            ColumnNotFoundException {
        log.debug("endpoint analyse datatypes, databaseId={}, key={}", databaseId, key);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        return ResponseEntity.ok()
                .body(analyseService.determineDataTypes(database.getContainer().getImage(), key));
    }

    @PostMapping("/{databaseId}/check-tuples-after-timestamp")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Check for tuples inserted after timestamp",
            description = "Check if there are tuples inserted after a given timestamp in the specified database",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Check completed successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request parameters",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection to database",
                    content = {@Content}),
    })
    public ResponseEntity<java.util.Map<String, Object>> checkTuplesAfterTimestamp(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                                  @RequestBody java.util.Map<String, Object> request) {

        try {
            // Extract parameters from request
            String timestampStr = (String) request.get("timestamp");
            String replicaDatabaseId = (String) request.get("replicaDatabaseId");
            
            if (timestampStr == null || replicaDatabaseId == null) {
                log.error("Missing required parameters. timestamp: {}, replicaDatabaseId: {}", timestampStr, replicaDatabaseId);
                throw new IllegalArgumentException("timestamp and replicaDatabaseId are required");
            }
            
            // Parse timestamp
            java.time.Instant timestamp = java.time.Instant.parse(timestampStr);
            log.info("Parsed timestamp: {}", timestamp);
            
            // Get database from cache service
            final DatabaseDto database = cacheService.getDatabase(databaseId);
            
            // Delegate to table service
            boolean hasNewTuples = tableService.checkTuplesAfterTimestamp(database, timestamp, replicaDatabaseId);

            if (hasNewTuples) {
                List<TuplesWithTimestampsDto.TupleWithTimestampsDto> tuplesWithTimestamps = tableService.loadNewTuplesAfterTimestamp(database, timestamp);
                log.info("Found {} new tuples after timestamp {}", tuplesWithTimestamps.size(), timestamp);
                for (TuplesWithTimestampsDto.TupleWithTimestampsDto tuple : tuplesWithTimestamps) {
                    log.info("New tuple data: {} | inserted_at: {} | deleted_at: {} | replication_key: {}", 
                        tuple.getData(), 
                        tuple.getInsertedAt(), 
                        tuple.getDeletedAt(), 
                        tuple.getReplicationKey());
                }
            }
            
            return ResponseEntity.ok(null);
            
        } catch (Exception e) {
            log.error("❌ Error checking tuples after timestamp: {}", e.getMessage(), e);
            
            java.util.Map<String, Object> response = java.util.Map.of(
                "status", "error",
                "message", "Failed to check tuples after timestamp: " + e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }

}
