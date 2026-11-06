package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierStatusTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database")
public class DatabaseEndpoint extends RestEndpoint {

    private final UserService userService;
    private final MetadataMapper metadataMapper;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final ContainerService containerService;
    private final DashboardService dashboardService;

    @Autowired
    public DatabaseEndpoint(UserService userService, MetadataMapper metadataMapper, StorageService storageService,
                            DatabaseService databaseService, ContainerService containerService,
                            DashboardService dashboardService) {
        this.userService = userService;
        this.metadataMapper = metadataMapper;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.containerService = containerService;
        this.dashboardService = dashboardService;
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
                if (AuthUtil.isSystem(principal)) {
                    log.debug("filter request to contain only databases that match internal name: {}", internalName);
                    databases = databaseService.findByInternalName(internalName);
                } else {
                    log.debug("filter request to contain only public databases or where user with username {} has at least read access that match internal name: {}", AuthUtil.getUsername(principal), internalName);
                    databases = databaseService.findAllPublicOrSchemaPublicOrReadAccessByInternalName(AuthUtil.getUsername(principal), internalName);
                }
            } else {
                if (AuthUtil.isSystem(principal)) {
                    databases = databaseService.findAll();
                } else {
                    log.debug("filter request to contain only databases where user with username {} has at least read access", AuthUtil.getUsername(principal));
                    databases = databaseService.findAllPublicOrSchemaPublicOrReadAccess(AuthUtil.getUsername(principal));
                }
            }
        } else {
            if (internalName != null) {
                log.debug("filter request to contain only public databases that match internal name: {}", internalName);
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
                        .map(database -> filterBrief(metadataMapper.databaseToDatabaseBriefDto(database), principal))
                        .toList());
    }

    @GetMapping("/batch")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_database_batch")
    @Operation(summary = "Find current database metadata in a batch",
            description = "Accepts 1 to 100 database IDs. Duplicate IDs are read once; missing or inaccessible databases are omitted.")
    public ResponseEntity<List<DatabaseDto>> findBatch(@RequestParam("id") List<UUID> ids, Principal principal) {
        if (ids == null || ids.isEmpty() || ids.size() > 100 || ids.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide 1 to 100 database IDs");
        }
        final List<DatabaseDto> result = new ArrayList<>();
        for (UUID id : new LinkedHashSet<>(ids)) {
            try {
                result.add(findById(id, principal).getBody());
            } catch (DatabaseNotFoundException | NotAllowedException ignored) {
                // Use the same authorization as a single read without revealing excluded databases.
            }
        }
        return ResponseEntity.ok(result);
    }

    private DatabaseBriefDto filterBrief(DatabaseBriefDto dto, Principal principal) {
        if (!AuthUtil.isSystem(principal) && dto.getIdentifiers() != null) {
            final String username = principal == null ? null : AuthUtil.getUsername(principal);
            dto.setIdentifiers(dto.getIdentifiers().stream().filter(identifier ->
                    IdentifierStatusTypeDto.PUBLISHED.equals(identifier.getStatus()) ||
                            (IdentifierStatusTypeDto.DRAFT.equals(identifier.getStatus()) && username != null &&
                                    username.equals(identifier.getOwnedBy()))).toList());
        }
        return dto;
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
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Database create permission is missing or grant permissions at broker service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to fin container/user/database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "Query store could not be created",
                    content = {@Content}),
            @ApiResponse(responseCode = "423",
                    description = "Database quota exceeded",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseBriefDto> create(@Valid @RequestBody CreateDatabaseDto data,
                                                   Principal principal) throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException, SearchServiceException, SearchServiceConnectionException,
            ContainerQuotaException, DashboardServiceException, DashboardServiceConnectionException, NotAllowedException {
        log.debug("endpoint create database, data.name={}", data.getName());
        final Container container = containerService.find(data.getCid());
        if (container.getQuota() != null && container.getDatabases().size() + 1 > container.getQuota()) {
            log.error("Failed to create database: quota of {} exceeded", container.getQuota());
            throw new ContainerQuotaException("Failed to create database: quota of " + container.getQuota() + " exceeded");
        }
        final UserDto owner = userService.findByUsername(AuthUtil.getUsername(principal));
        log.debug("found user: {}", owner);
        final Database database = databaseService.create(container, data, owner);
        /* find in dashboard service */
        final CreateDashboardResponseDto dashboard = dashboardService.create(database);
        database.setDashboardUid(dashboard.getUid());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(filterBrief(metadataMapper.databaseToDatabaseBriefDto(database), principal));
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
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to refresh table metadata",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseBriefDto> refreshTableMetadata(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                 Principal principal) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, NotAllowedException, MalformedException, TableNotFoundException {
        log.debug("endpoint refresh database metadata, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwnedBy().equals(AuthUtil.getUsername(principal))) {
            log.error("Failed to refresh database tables metadata: not owner");
            throw new NotAllowedException("Failed to refresh tables metadata: not owner");
        }
        return ResponseEntity.ok(filterBrief(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        databaseService.updateTableMetadata(database))), principal));
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
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseBriefDto> refreshViewMetadata(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                Principal principal) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, NotAllowedException, ViewNotFoundException {
        log.debug("endpoint refresh database metadata, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwnedBy().equals(AuthUtil.getUsername(principal))) {
            log.error("Failed to refresh database views metadata: not owner");
            throw new NotAllowedException("Failed to refresh database views metadata: not owner");
        }
        return ResponseEntity.ok(filterBrief(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        databaseService.updateViewMetadata(database))), principal));
    }

    @PutMapping("/{databaseId}/visibility")
    @Transactional(rollbackFor = {DashboardServiceException.class, DashboardServiceConnectionException.class})
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
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Visibility modification is not permitted",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseBriefDto> visibility(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                       @Valid @RequestBody DatabaseModifyVisibilityDto data,
                                                       Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, SearchServiceException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException {
        log.debug("endpoint modify database visibility, databaseId={}, data={}", databaseId, data);
        final Database database = databaseService.findForUpdateById(databaseId);
        if (!database.getOwnedBy().equals(AuthUtil.getUsername(principal))) {
            log.error("Failed to modify database visibility: not owner");
            throw new NotAllowedException("Failed to modify database visibility: not owner");
        }
        if (data.getIsDashboardEnabled() && !data.getIsPublic() && !data.getIsSchemaPublic()) {
            dashboardService.requireUser(database.getOwnedBy());
        }
        final Database database1 = databaseService.modifyVisibility(database, data);
        dashboardService.update(database1);
        return ResponseEntity.accepted()
                .body(filterBrief(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        database1)), principal));
    }

    @PutMapping("/{databaseId}/owner")
    @Transactional(rollbackFor = {DashboardServiceException.class, DashboardServiceConnectionException.class})
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
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Transfer of ownership is not permitted",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseBriefDto> transfer(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                     @Valid @RequestBody DatabaseTransferDto data,
                                                     Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {
        log.debug("endpoint transfer database, databaseId={}, transferDto.username={}", databaseId, data.getUsername());
        final Database database = databaseService.findForUpdateById(databaseId);
        if (!database.getOwnedBy().equals(AuthUtil.getUsername(principal))) {
            log.error("Failed to transfer database: not owner");
            throw new NotAllowedException("Failed to transfer database: not owner");
        }
        if (database.getIsDashboardEnabled() && !database.getIsPublic() && !database.getIsSchemaPublic()) {
            dashboardService.requireUser(data.getUsername());
        }
        final Database updated = databaseService.modifyOwner(database, data.getUsername());
        dashboardService.update(updated);
        return ResponseEntity.accepted()
                .body(filterBrief(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        updated)), principal));
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
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content}),
            @ApiResponse(responseCode = "410",
                    description = "File was not found in the Storage Service",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseBriefDto> modifyImage(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                        @Valid @RequestBody DatabaseModifyImageDto data,
                                                        Principal principal) throws NotAllowedException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException,
            StorageUnavailableException, StorageNotFoundException {
        log.debug("endpoint modify database image, databaseId={}, data.key={}", databaseId, data.getKey());
        final Database database = databaseService.findForUpdateById(databaseId);
        if (!database.getOwnedBy().equals(AuthUtil.getUsername(principal))) {
            log.error("Failed to update database image: not owner");
            throw new NotAllowedException("Failed to update database image: not owner");
        }
        byte[] image = null;
        if (data.getKey() != null) {
            storageService.requireUploadOwner(data.getKey(), UploadMetadata.ownerId(principal));
            image = storageService.getBytes(data.getKey());
        }
        return ResponseEntity.accepted()
                .body(filterBrief(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(
                        databaseService.modifyImage(database, image))), principal));
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
                    content = {@Content})
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
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database could not be found",
                    content = {@Content})
    })
    public ResponseEntity<DatabaseDto> findById(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                Principal principal) throws DatabaseNotFoundException,
            NotAllowedException {
        log.debug("endpoint find database, databaseId={}", databaseId);
        final Database database = filterDatabase(databaseService.findById(databaseId), principal);
        final DatabaseDto dto = metadataMapper.databaseToDatabaseDto(database);
        if (!AuthUtil.isSystem(principal)) {
            final var columns = visibleColumns(database.getTables());
            dto.getTables().forEach(table -> filterTableMetadata(table, columns, principal));
            dto.getViews().forEach(view -> filterViewMetadata(view, principal));
            dto.setIdentifiers(filterIdentifiers(dto.getIdentifiers(), principal));
            dto.setSubsets(filterIdentifiers(dto.getSubsets(), principal));
        }
        final HttpHeaders headers = new HttpHeaders();
        if (AuthUtil.isSystem(principal)) {
            log.trace("attach privileged credential information");
            headers.set("X-Host", database.getContainer().getHost());
            headers.set("X-Port", "" + database.getContainer().getPort());
            headers.set("X-Username", database.getContainer().getPrivilegedUsername());
            headers.set("X-Password", database.getContainer().getPrivilegedPassword());
            headers.set("X-Jdbc-Method", database.getContainer().getImage().getJdbcMethod());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password X-Jdbc-Method X-Host X-Port");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(dto);
    }

}
