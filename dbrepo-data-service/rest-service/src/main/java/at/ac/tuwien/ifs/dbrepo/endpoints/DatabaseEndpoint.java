package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database")
public class DatabaseEndpoint {

    private final AccessService accessService;
    private final AnalyseService analyseService;
    private final DatabaseService databaseService;
    private final MetadataService metadataService;

    @Autowired
    public DatabaseEndpoint(AccessService accessService, AnalyseService analyseService, DatabaseService databaseService,
                            MetadataService metadataService) {
        this.accessService = accessService;
        this.analyseService = analyseService;
        this.databaseService = databaseService;
        this.metadataService = metadataService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Create database",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a database"),
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
    public ResponseEntity<Void> create(@Valid @RequestBody CreateDatabaseDto data)
            throws DatabaseUnavailableException, RemoteUnavailableException, ContainerNotFoundException,
            DatabaseMalformedException, QueryStoreCreateException, MetadataServiceException, MalformedException {
        log.debug("endpoint create database, data.containerId={}, data.internalName={}", data.getContainerId(),
                data.getInternalName());
        final Container container = metadataService.getContainer(data.getContainerId());
        try {
            final Database database = databaseService.create(container, data);
            databaseService.createExtensions(container, data.getInternalName());
            accessService.create(database, AccessTypeDto.WRITE_ALL, data.getUsername(), data.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .build();
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
        final Database database = metadataService.getDatabase(databaseId);
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
        final Database database = metadataService.getDatabase(databaseId);
        return ResponseEntity.ok()
                .body(analyseService.determineS3CsvDataTypes(database.getContainer().getImage(), key));
    }

}
