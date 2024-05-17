package at.tuwien.endpoints;

import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.database.*;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.config.RabbitConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.*;
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
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database")
public class DatabaseEndpoint {

    private final UserService userService;
    private final RabbitConfig rabbitConfig;
    private final AccessService accessService;
    private final BrokerService messageQueueService;
    private final DatabaseMapper databaseMapper;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final AuthenticationService authenticationService;

    @Autowired
    public DatabaseEndpoint(UserService userService, RabbitConfig rabbitConfig, AccessService accessService,
                            BrokerService messageQueueService, DatabaseMapper databaseMapper,
                            StorageService storageService, DatabaseService databaseService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.rabbitConfig = rabbitConfig;
        this.accessService = accessService;
        this.messageQueueService = messageQueueService;
        this.databaseMapper = databaseMapper;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.authenticationService = authenticationService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_metadata_database_findall")
    @Operation(summary = "List databases")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of databases",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DatabaseDto.class)))}),
    })
    public ResponseEntity<List<DatabaseDto>> list(@RequestParam(name = "internal_name", required = false) String internalName) {
        log.debug("endpoint list databases, internalName={}", internalName);
        List<DatabaseDto> dtos = new LinkedList<>();
        if (internalName != null) {
            try {
                dtos = List.of(databaseMapper.databaseToDatabaseDto(databaseService.findByInternalName(internalName)));
            } catch (DatabaseNotFoundException e) {
                /* ignore */
            }
        } else {
            dtos = databaseService.findAll()
                    .stream()
                    .map(databaseMapper::databaseToDatabaseDto)
                    .toList();
        }
        log.trace("list databases resulted in {} database(s)", dtos.size());
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Count", "" + dtos.size());
        headers.set("Access-Control-Expose-Headers", "X-Count");
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(dtos);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('create-database')")
    @Observed(name = "dbrepo_metadata_database_create")
    @Operation(summary = "Create database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
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
    public ResponseEntity<DatabaseDto> create(@Valid @RequestBody DatabaseCreateDto data,
                                              @NotNull Principal principal) throws ServiceException,
            ServiceConnectionException, UserNotFoundException, DatabaseNotFoundException, ContainerNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint create database, data.name={}", data.getName());
        final User user = userService.findByUsername(principal.getName());
        final Database database = databaseService.create(data, user);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping("/{databaseId}/visibility")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-visibility')")
    @Observed(name = "dbrepo_metadata_database_visibility")
    @Operation(summary = "Update database visibility", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<DatabaseDto> visibility(@NotNull @PathVariable("databaseId") Long databaseId,
                                                  @Valid @RequestBody DatabaseModifyVisibilityDto data,
                                                  @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint modify database visibility, databaseId={}, data={}", databaseId, data);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().equals(principal)) {
            log.error("Failed to modify database visibility: not owner");
            throw new NotAllowedException("Failed to modify database visibility: not owner");
        }
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(databaseService.modifyVisibility(database, data));
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{databaseId}/owner")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-owner')")
    @Observed(name = "dbrepo_metadata_database_transfer")
    @Operation(summary = "Update database owner", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<DatabaseDto> transfer(@NotNull @PathVariable("databaseId") Long databaseId,
                                                @Valid @RequestBody DatabaseTransferDto data,
                                                @NotNull Principal principal) throws NotAllowedException,
            ServiceException, ServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint transfer database, databaseId={}, transferDto.id={}", databaseId, data.getId());
        final Database database = databaseService.findById(databaseId);
        final User user = userService.findByUsername(principal.getName());
        final User newOwner = userService.findById(data.getId());
        if (!database.getOwner().equals(user)) {
            log.error("Failed to transfer database: not owner");
            throw new NotAllowedException("Failed to transfer database: not owner");
        }
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(databaseService.modifyOwner(database, newOwner));
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PutMapping("/{databaseId}/image")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-image')")
    @Observed(name = "dbrepo_metadata_database_image")
    @Operation(summary = "Update database image", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modify of image was successful",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Modify of image is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "File was not found in the Storage Service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseDto> modifyImage(@NotNull @PathVariable("databaseId") Long databaseId,
                                                   @Valid @RequestBody DatabaseModifyImageDto data,
                                                   @NotNull Principal principal) throws NotAllowedException,
            DatabaseNotFoundException, UserNotFoundException, SearchServiceException, SearchServiceConnectionException,
            StorageUnavailableException, StorageNotFoundException {
        log.debug("endpoint modify database image, databaseId={}, data.key={}", databaseId, data.getKey());
        final Database database = databaseService.findById(databaseId);
        final User user = userService.findByUsername(principal.getName());
        if (!database.getOwner().equals(user)) {
            log.error("Failed to update database image: not owner");
            throw new NotAllowedException("Failed to update database image: not owner");
        }
        final DatabaseDto dto;
        byte[] image = null;
        if (data.getKey() != null) {
            image = storageService.getBytes(data.getKey());
        }
        dto = databaseMapper.databaseToDatabaseDto(databaseService.modifyImage(database, image));
        return ResponseEntity.accepted()
                .body(dto);
    }

    @GetMapping("/{databaseId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_metadata_database_find")
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
    public ResponseEntity<DatabaseDto> findById(@NotNull @PathVariable("databaseId") Long databaseId,
                                                Principal principal) throws ServiceException,
            ServiceConnectionException, DatabaseNotFoundException, ExchangeNotFoundException {
        log.debug("endpoint find database, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        if (database.getOwner().equals(principal)) {
            log.debug("current logged-in user is also the owner: additionally load access list");
            /* only owner sees the access rights */
            final List<DatabaseAccess> accesses = accessService.list(database);
            dto.setAccesses(accesses.stream()
                    .map(databaseMapper::databaseAccessToDatabaseAccessDto)
                    .collect(Collectors.toList()));
            log.debug("found {} database accesses", accesses.size());
        }
        final HttpHeaders headers = new HttpHeaders();
        if (principal != null) {
            /* extra effort only when having access */
            final ExchangeDto exchange = messageQueueService.findExchange(rabbitConfig.getExchangeName());
            dto.setExchangeType(exchange.getType());
            final Authentication authentication = (Authentication) principal;
            if (authentication.isAuthenticated() && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"))) {
                headers.set("X-Username", database.getContainer().getPrivilegedUsername());
                headers.set("X-Password", database.getContainer().getPrivilegedPassword());
                headers.set("Access-Control-Expose-Headers", "X-Username X-Password");
            }
        }
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(dto);
    }

}
