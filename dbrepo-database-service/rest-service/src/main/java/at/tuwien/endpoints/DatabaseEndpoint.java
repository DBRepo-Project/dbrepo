package at.tuwien.endpoints;

import at.tuwien.api.database.*;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.mdb.DatabaseAccessRepository;
import at.tuwien.service.*;
import at.tuwien.service.impl.MariaDbServiceImpl;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/database")
public class DatabaseEndpoint {

    private final UserService userService;
    private final AccessService accessService;
    private final DatabaseMapper databaseMapper;
    private final MariaDbServiceImpl databaseService;
    private final QueryStoreService queryStoreService;
    private final MessageQueueService messageQueueService;
    private final DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    public DatabaseEndpoint(DatabaseMapper databaseMapper, UserService userService,
                            MariaDbServiceImpl databaseService, QueryStoreService queryStoreService,
                            MessageQueueService messageQueueService, AccessService accessService,
                            DatabaseAccessRepository databaseAccessRepository) {
        this.userService = userService;
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.queryStoreService = queryStoreService;
        this.messageQueueService = messageQueueService;
        this.databaseAccessRepository = databaseAccessRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "database.list", description = "Time needed to list the databases")
    @Operation(summary = "List databases")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of databases",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DatabaseBriefDto.class)))}),
    })
    public ResponseEntity<List<DatabaseDto>> list(@NotNull Principal principal) {
        log.debug("endpoint list databases, principal={}", principal);
        List<DatabaseDto> databases;
        databases = databaseService.findAll()
                .stream()
                .map(databaseMapper::databaseToDatabaseDto)
                .collect(Collectors.toList());
        log.trace("list databases resulted in databases {}", databases);
        return ResponseEntity.ok(databases);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('create-database')")
    @Timed(value = "database.create", description = "Time needed to create a database")
    @Operation(summary = "Create database", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Database create query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container, user or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Database create permission is missing or grant permissions at broker service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to create user at broker service or virtual host could not be reached at broker service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Database name already exist or query store could not be created",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Container image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to the container failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseBriefDto> create(@Valid @RequestBody DatabaseCreateDto createDto,
                                                   @NotNull Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException, DatabaseMalformedException,
            AmqpException, ContainerConnectionException, UserNotFoundException,
            DatabaseNotFoundException, DatabaseNameExistsException, DatabaseConnectionException,
            QueryMalformedException, NotAllowedException, BrokerVirtualHostCreationException, QueryStoreException,
            BrokerVirtualHostGrantException {
        log.debug("endpoint create database, createDto={}, principal={}", createDto,
                principal);
        final User user = userService.findByUsername(principal.getName());
        final Database database = databaseService.create(createDto, principal);
        messageQueueService.createUser(user);
        messageQueueService.createExchange(database, principal);
        messageQueueService.updatePermissions(user);
        queryStoreService.create(database.getId(), principal);
        databaseAccessRepository.save(databaseMapper.defaultCreatorAccess(database, user));
        final DatabaseBriefDto dto = databaseMapper.databaseToDatabaseBriefDto(database);
        log.trace("create database resulted in database {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping("/{id}/visibility")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-visibility')")
    @Timed(value = "database.visibility", description = "Time needed to modify a database visibility")
    @Operation(summary = "Update database", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Visibility modified successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Visibility modification is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseDto> visibility(@NotNull @PathVariable Long id,
                                                  @Valid @RequestBody DatabaseModifyVisibilityDto data,
                                                  @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint update database, id={}, data={}, principal={}", id, data, principal);
        final Database database = databaseService.findById(id);
        final User user = userService.findByUsername(principal.getName());
        if (!database.getOwner().equals(user)) {
            log.error("Failed to create database: not owner");
            throw new NotAllowedException(("Failed to create database: not owner"));
        }
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(databaseService.visibility(id, data));
        log.trace("update database resulted in database {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{id}/transfer")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-owner')")
    @Timed(value = "database.transfer", description = "Time needed to transfer a database ownership")
    @Operation(summary = "Transfer database", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Transfer of ownership was successful",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Transfer of ownership is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseDto> transfer(@NotNull @PathVariable Long id,
                                                @Valid @RequestBody DatabaseTransferDto transferDto,
                                                @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint update database, id={}, transferDto={}, principal={}", id, transferDto, principal);
        final Database database = databaseService.findById(id);
        final User user = userService.findByUsername(principal.getName());
        if (!database.getOwner().equals(user)) {
            log.error("Failed to create database: not owner");
            throw new NotAllowedException(("Failed to create database: not owner"));
        }
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(databaseService.transfer(id, transferDto));
        log.trace("update database resulted in database {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Timed(value = "database.find", description = "Time needed to find a database")
    @Operation(summary = "Find some database", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Database found successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or container could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Database information is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseDto> findById(@NotNull @PathVariable Long id, Principal principal)
            throws DatabaseNotFoundException, AccessDeniedException {
        log.debug("endpoint find database, id={}", id);
        final Database database = databaseService.findById(id);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        if (principal != null && database.getOwner().equalsPrincipal(principal)) {
            /* only owner sees the access rights */ // TODO improve this by proper mapping
            final List<DatabaseAccess> accesses = accessService.list(id);
            dto.setAccesses(accesses.stream()
                    .map(databaseMapper::databaseAccessToDatabaseAccessDto)
                    .collect(Collectors.toList()));
        }
        log.trace("find database resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('delete-database')")
    @Timed(value = "database.delete", description = "Time needed to delete a database")
    @Operation(summary = "Delete some database", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Deleted a database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Database delete query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Database delete permission is missing or revoke permissions at broker service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to delete user at broker service or virtual host could not be reached at broker service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Container image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to the container failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable Long id, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, DatabaseMalformedException, AmqpException,
            QueryMalformedException, UserNotFoundException, BrokerVirtualHostGrantException,
            DatabaseConnectionException {
        log.debug("endpoint delete database, id={}, principal={}", id,
                principal);
        final Database database = databaseService.findById(id);
        final User user = userService.findByUsername(principal.getName());
        messageQueueService.deleteExchange(database);
        databaseService.delete(id, user.getId());
        messageQueueService.updatePermissions(user);
        return ResponseEntity.accepted()
                .build();
    }

}
