package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.ContainerService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
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

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database")
public class DatabaseEndpoint extends RestEndpoint {

    private final DataMapper dataMapper;
    private final CacheService cacheService;
    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final ContainerService containerService;

    @Autowired
    public DatabaseEndpoint(DataMapper dataMapper, CacheService cacheService, AccessService accessService,
                            DatabaseService databaseService, ContainerService containerService) {
        this.dataMapper = dataMapper;
        this.cacheService = cacheService;
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.containerService = containerService;
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find container in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to create query store in database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to update user password in database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
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

}
