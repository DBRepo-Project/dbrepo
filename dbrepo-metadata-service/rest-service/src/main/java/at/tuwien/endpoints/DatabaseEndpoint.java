package at.tuwien.endpoints;

import at.tuwien.api.database.*;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.StorageService;
import at.tuwien.service.UserService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database")
public class DatabaseEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final MetadataMapper metadataMapper;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final ContainerService containerService;

    @Autowired
    public DatabaseEndpoint(UserService userService, MetadataMapper metadataMapper, StorageService storageService,
                            DatabaseService databaseService, ContainerService containerService) {
        this.userService = userService;
        this.metadataMapper = metadataMapper;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.containerService = containerService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_database_findall")
    @Operation(summary = "List databases",
            description = "Lists all databases in the metadata database. Requests with HTTP method **GET** return the list of databases, requests with HTTP method **HEAD** only the number in the `X-Count` header.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of databases",
                    headers = {@Header(name = "X-Count", description = "Number of databases", schema = @Schema(implementation = Long.class), required = true),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose `X-Count` custom header", schema = @Schema(implementation = String.class), required = true)},
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DatabaseBriefDto.class)))}),
    })
    public ResponseEntity<List<DatabaseBriefDto>> list(@RequestParam(name = "internal_name", required = false) String internalName,
                                                       Principal principal) {
        log.debug("endpoint list databases, internalName={}", internalName);
        final List<Database> databases;
        if (principal != null) {
            if (internalName != null) {
                log.debug("filter request to contain only public databases or where user with id {} has at least read access that match internal name {}", getId(principal), internalName);
                databases = databaseService.findAllPublicOrSchemaPublicOrReadAccessByInternalName(getId(principal), internalName);
            } else {
                log.debug("filter request to contain only databases where user with id {} has at least read access", getId(principal));
                databases = databaseService.findAllPublicOrSchemaPublicOrReadAccess(getId(principal));
            }
        } else {
            if (internalName != null) {
                log.debug("filter request to contain only public databases that match internal name {}", internalName);
                databases = databaseService.findAllPublicOrSchemaPublicByInternalName(internalName);
            } else {
                log.debug("filter request to contain only public databases");
                databases = databaseService.findAllPublicOrSchemaPublic();
            }
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Count", "" + databases.size());
        headers.set("Access-Control-Expose-Headers", "X-Count");
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(databases.stream()
                        .map(metadataMapper::databaseToDatabaseBriefDto)
                        .toList());
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('create-database')")
    @Observed(name = "dbrepo_database_create")
    @Operation(summary = "Create database",
            description = "Creates a database in the container with id. Requires roles `create-database`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
                    description = "Failed to fin container/user/database in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Query store could not be created",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Database quota exceeded",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseBriefDto> create(@Valid @RequestBody CreateDatabaseDto data,
                                                   @NotNull Principal principal) throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException, SearchServiceException, SearchServiceConnectionException,
            ContainerQuotaException {
        log.debug("endpoint create database, data.name={}", data.getName());
        final Container container = containerService.find(data.getCid());
        if (container.getDatabases().size() + 1 > container.getQuota()) {
            log.error("Failed to create database: quota of {} exceeded", container.getQuota());
            throw new ContainerQuotaException("Failed to create database: quota of " + container.getQuota() + " exceeded");
        }
        final User caller = userService.findById(getId(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        databaseService.create(container, data, caller, userService.findAllInternalUsers()))));
    }

    @PutMapping("/{databaseId}/metadata/table")
    @Transactional(rollbackFor = {Exception.class})
    @PreAuthorize("hasAuthority('find-database')")
    @Observed(name = "dbrepo_tables_refresh")
    @Operation(summary = "Update database table schemas",
            description = "Updates the database with id with generated metadata from tables that are not yet known to the database. Only the database owner can perform this operation. Requires role `find-database`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Refreshed database tables metadata",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Failed to parse payload at search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to refresh table metadata",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseBriefDto> refreshTableMetadata(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                 @NotNull Principal principal) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, NotAllowedException, QueryNotFoundException, MalformedException,
            TableNotFoundException {
        log.debug("endpoint refresh database metadata, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to refresh database tables metadata: not owner");
            throw new NotAllowedException("Failed to refresh tables metadata: not owner");
        }
        return ResponseEntity.ok(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                databaseService.updateTableMetadata(database))));
    }

    @PutMapping("/{databaseId}/metadata/view")
    @Transactional(rollbackFor = {SearchServiceException.class, SearchServiceConnectionException.class, DatabaseNotFoundException.class})
    @PreAuthorize("hasAuthority('find-database')")
    @Observed(name = "dbrepo_views_refresh")
    @Operation(summary = "Update database view schemas",
            description = "Updates the database with id with generated metadata from view that are not yet known to the database. Only the database owner can perform this operation. Requires role `find-database`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Refreshed database views metadata",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Refresh view metadata is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseBriefDto> refreshViewMetadata(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                @NotNull Principal principal) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, NotAllowedException, QueryNotFoundException, ViewNotFoundException {
        log.debug("endpoint refresh database metadata, databaseId={}, principal.name={}", databaseId, principal.getName());
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to refresh database views metadata: not owner");
            throw new NotAllowedException("Failed to refresh database views metadata: not owner");
        }
        return ResponseEntity.ok(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                databaseService.updateViewMetadata(database))));
    }

    @PutMapping("/{databaseId}/visibility")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-visibility')")
    @Observed(name = "dbrepo_database_visibility")
    @Operation(summary = "Update database visibility",
            description = "Updates the database with id on the visibility. Only the database owner can perform this operation. Requires role `modify-database-visibility`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Visibility modified successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "The visibility payload is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Visibility modification is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseBriefDto> visibility(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                       @Valid @RequestBody DatabaseModifyVisibilityDto data,
                                                       @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, SearchServiceException, SearchServiceConnectionException, UserNotFoundException {
        log.debug("endpoint modify database visibility, databaseId={}, data={}", databaseId, data);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to modify database visibility: not owner");
            throw new NotAllowedException("Failed to modify database visibility: not owner");
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        databaseService.modifyVisibility(database, data))));
    }

    @PutMapping("/{databaseId}/owner")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-owner')")
    @Observed(name = "dbrepo_database_transfer")
    @Operation(summary = "Update database owner",
            description = "Updates the database with id on the owner. Only the database owner can perform this operation. Requires role `modify-database-owner`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Transfer of ownership was successful",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Owner payload is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
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
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseBriefDto> transfer(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                     @Valid @RequestBody DatabaseTransferDto data,
                                                     @NotNull Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint transfer database, databaseId={}, transferDto.id={}", databaseId, data.getId());
        final Database database = databaseService.findById(databaseId);
        final User newOwner = userService.findById(data.getId());
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to transfer database: not owner");
            throw new NotAllowedException("Failed to transfer database: not owner");
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        databaseService.modifyOwner(database, newOwner))));
    }

    @PutMapping("/{databaseId}/image")
    @Transactional
    @PreAuthorize("hasAuthority('modify-database-image')")
    @Observed(name = "dbrepo_database_image")
    @Operation(summary = "Update database preview image",
            description = "Updates the database with id on the preview image. Only the database owner can perform this operation. Requires role `modify-database-image`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modify of image was successful",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Modify of image is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "File was not found in the Storage Service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseBriefDto> modifyImage(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                        @Valid @RequestBody DatabaseModifyImageDto data,
                                                        @NotNull Principal principal) throws NotAllowedException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException,
            StorageUnavailableException, StorageNotFoundException {
        log.debug("endpoint modify database image, databaseId={}, data.key={}", databaseId, data.getKey());
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to update database image: not owner");
            throw new NotAllowedException("Failed to update database image: not owner");
        }
        byte[] image = null;
        if (data.getKey() != null) {
            image = storageService.getBytes(data.getKey());
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        databaseService.modifyImage(database, image))));
    }

    @GetMapping("/{databaseId}/image")
    @Transactional
    @Observed(name = "dbrepo_database_image_view")
    @Operation(summary = "Get database preview image",
            description = "Gets the database with id on the preview image.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "View of image was successful"),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<byte[]> findPreviewImage(@NotNull @PathVariable("databaseId") UUID databaseId)
            throws DatabaseNotFoundException {
        log.debug("endpoint get database preview image, databaseId={}", databaseId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/webp"))
                .body(databaseService.findById(databaseId).getImage());
    }

    @GetMapping("/{databaseId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_database_find")
    @Operation(summary = "Find database",
            description = "Finds a database with id.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Database found successfully",
                    headers = {@Header(name = "X-Username", description = "The authentication username", schema = @Schema(implementation = String.class)),
                            @Header(name = "X-Password", description = "The authentication password", schema = @Schema(implementation = String.class)),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose custom headers", schema = @Schema(implementation = String.class))},
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to view database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<DatabaseDto> findById(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                Principal principal) throws DatabaseNotFoundException,
            NotAllowedException {
        log.debug("endpoint find database, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        if (principal != null) {
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUser().getId().equals(getId(principal)))
                    .findFirst();
            optional.ifPresentOrElse(access -> log.trace("user has access: {}", access), () -> log.trace("user has no access"));
            if (!database.getIsPublic() && !database.getIsSchemaPublic() && optional.isEmpty() && !isSystem(principal)) {
                log.error("Failed to find database: not public and no access found");
                throw new NotAllowedException("Failed to find database: not public and no access found");
            }
            /* reduce metadata */
            final int tables = database.getTables()
                    .size();
            database.setTables(database.getTables()
                    .stream()
                    .filter(t -> t.getIsPublic() || t.getIsSchemaPublic() || optional.isPresent())
                    .toList());
            log.trace("filtered database tables from {} to {}", tables, database.getTables().size());
            final int views = database.getViews()
                    .size();
            database.setViews(database.getViews()
                    .stream()
                    .filter(v -> v.getIsPublic() || v.getIsSchemaPublic() || optional.isPresent())
                    .toList());
            log.trace("filtered database views from {} to {}", views, database.getViews().size());
            if (!isSystem(principal) && !database.getOwner().getId().equals(getId(principal))) {
                log.trace("authenticated user {} is not owner: remove access list", principal.getName());
                database.setAccesses(List.of());
            }
        } else {
            if (!database.getIsPublic() && !database.getIsSchemaPublic()) {
                log.error("Failed to find database: not public and not authenticated");
                throw new NotAllowedException("Failed to find database: not public and not authenticated");
            }
            /* reduce metadata */
            database.getTables()
                    .removeAll(database.getTables()
                            .stream()
                            .filter(t -> !t.getIsPublic() && !t.getIsSchemaPublic())
                            .toList());
            database.getViews()
                    .removeAll(database.getViews()
                            .stream()
                            .filter(v -> !v.getIsPublic() && !v.getIsSchemaPublic())
                            .toList());
            database.setAccesses(List.of());
        }
        final DatabaseDto dto = metadataMapper.databaseToDatabaseDto(database);
        final HttpHeaders headers = new HttpHeaders();
        if (isSystem(principal)) {
            headers.set("X-Username", database.getContainer().getPrivilegedUsername());
            headers.set("X-Password", database.getContainer().getPrivilegedPassword());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password");
        } else {
            removeInternalData(dto.getContainer());
        }
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(dto);
    }

}
