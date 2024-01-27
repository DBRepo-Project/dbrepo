package at.tuwien.endpoints;

import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.database.*;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.config.RabbitConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.*;
import at.tuwien.utils.PrincipalUtil;
import at.tuwien.utils.UserUtil;
import io.micrometer.observation.annotation.Observed;
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
import org.springframework.http.HttpHeaders;
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
    private final RabbitConfig rabbitConfig;
    private final AccessService accessService;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;
    private final QueryStoreService queryStoreService;
    private final MessageQueueService messageQueueService;

    @Autowired
    public DatabaseEndpoint(DatabaseMapper databaseMapper, UserService userService, RabbitConfig rabbitConfig,
                            DatabaseService databaseService, QueryStoreService queryStoreService,
                            AccessService accessService, MessageQueueService messageQueueService) {
        this.userService = userService;
        this.rabbitConfig = rabbitConfig;
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.queryStoreService = queryStoreService;
        this.messageQueueService = messageQueueService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_database_findall")
    @Operation(summary = "List databases")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of databases",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DatabaseBriefDto.class)))}),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<DatabaseDto>> list(@NotNull Principal principal,
                                                  @RequestParam(required = false) String filter)
            throws UserNotFoundException {
        log.debug("endpoint list databases, filter={}, {}", filter, PrincipalUtil.formatForDebug(principal));
        final List<DatabaseDto> dtos;
        if (principal != null && filter != null) {
            final User user = userService.findByUsername(principal.getName());
            dtos = databaseService.findAccess(user.getId())
                    .stream()
                    .map(databaseMapper::databaseToDatabaseDto)
                    .collect(Collectors.toList());
        } else {
            dtos = databaseService.findAll()
                    .stream()
                    .map(databaseMapper::databaseToDatabaseDto)
                    .collect(Collectors.toList());
        }
        log.trace("list databases resulted in databases {}", dtos);
        return ResponseEntity.ok(dtos);
    }

    @RequestMapping(method = RequestMethod.HEAD)
    @Transactional(readOnly = true)
    @Observed(name = "dbr_database_count")
    @Operation(summary = "Count databases")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Count databases"),
            @ApiResponse(responseCode = "404",
                    description = "User not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<DatabaseDto>> count(@NotNull Principal principal,
                                                   @RequestParam(required = false) String filter)
            throws UserNotFoundException {
        log.debug("endpoint list databases, filter={}, {}", filter, PrincipalUtil.formatForDebug(principal));
        final List<DatabaseDto> dtos;
        if (principal != null && filter != null) {
            final User user = userService.findByUsername(principal.getName());
            dtos = databaseService.findAccess(user.getId())
                    .stream()
                    .map(databaseMapper::databaseToDatabaseDto)
                    .collect(Collectors.toList());
        } else {
            dtos = databaseService.findAll()
                    .stream()
                    .map(databaseMapper::databaseToDatabaseDto)
                    .collect(Collectors.toList());
        }
        log.trace("list databases resulted in databases {}", dtos);
        final HttpHeaders headers = new HttpHeaders();
        headers.set("x-count", "" + dtos.size());
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .build();
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('create-database')")
    @Observed(name = "dbr_database_create")
    @Operation(summary = "Create database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Database create query is malformed or image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Database create permission is missing or grant permissions at broker service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container, user or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Query store could not be created",
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
            throws ContainerNotFoundException, DatabaseMalformedException, UserNotFoundException,
            DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException, NotAllowedException,
            QueryStoreException {
        log.debug("endpoint create database, createDto={}, {}", createDto, PrincipalUtil.formatForDebug(principal));
        final User user = userService.findByUsername(principal.getName());
        final Database database = databaseService.create(createDto, principal);
        queryStoreService.create(database.getId(), principal);
        accessService.create(database.getId(), user.getId(), DatabaseGiveAccessDto.builder()
                .type(AccessTypeDto.WRITE_ALL)
                .build());
        final DatabaseBriefDto dto = databaseMapper.databaseToDatabaseBriefDto(database);
        log.trace("create database resulted in database {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping("/{id}/visibility")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-visibility')")
    @Observed(name = "dbr_database_visibility")
    @Operation(summary = "Update database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Visibility modified successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Visibility modification is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseDto> visibility(@NotNull @PathVariable Long id,
                                                  @Valid @RequestBody DatabaseModifyVisibilityDto data,
                                                  @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException {
        log.debug("endpoint update database, id={}, data={}, {}", id, data, PrincipalUtil.formatForDebug(principal));
        final Database database = databaseService.findById(id);
        if (!database.getOwnedBy().equals(UserUtil.getId(principal))) {
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
    @Observed(name = "dbr_database_transfer")
    @Operation(summary = "Transfer database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
            @ApiResponse(responseCode = "403",
                    description = "Transfer of ownership is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseDto> transfer(@NotNull @PathVariable Long id,
                                                @Valid @RequestBody DatabaseTransferDto transferDto,
                                                @NotNull Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException {
        log.debug("endpoint update database, id={}, transferDto={}, {}", id, transferDto, PrincipalUtil.formatForDebug(principal));
        final Database database = databaseService.findById(id);
        final User user = userService.findByUsername(principal.getName());
        if (!database.getOwnedBy().equals(user.getId())) {
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
    @Observed(name = "dbr_database_find")
    @Operation(summary = "Find some database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Database found successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or exchange could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the broker service could not be established",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseDto> findById(@NotNull @PathVariable Long id, Principal principal)
            throws DatabaseNotFoundException, ExchangeNotFoundException, BrokerRemoteException {
        log.debug("endpoint find database, id={}, {}", id, PrincipalUtil.formatForDebug(principal));
        final Database database = databaseService.findById(id);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        if (principal != null && database.getOwnedBy().equals(UserUtil.getId(principal))) {
            log.debug("current logged-in user is also the owner: additionally load access list");
            /* only owner sees the access rights */
            final List<DatabaseAccess> accesses = accessService.list(id);
            dto.setAccesses(accesses.stream()
                    .map(databaseMapper::databaseAccessToDatabaseAccessDto)
                    .collect(Collectors.toList()));
            log.debug("found {} database accesses", accesses.size());
        }
        if (principal != null) {
            /* extra effort only when logged-in */
            final ExchangeDto exchange = messageQueueService.findExchange(rabbitConfig.getExchangeName());
            dto.setExchangeType(exchange.getType());
        }
        log.trace("find database resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

}
