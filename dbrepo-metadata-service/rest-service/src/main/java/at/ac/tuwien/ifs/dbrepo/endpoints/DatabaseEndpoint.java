package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.*;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database")
public class DatabaseEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final MetadataMapper metadataMapper;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final ContainerService containerService;
    private final DashboardService dashboardService;
    private final ReplicationService replicationService;

    @Autowired
    public DatabaseEndpoint(UserService userService, MetadataMapper metadataMapper, StorageService storageService,
                            DatabaseService databaseService, ContainerService containerService,
                            DashboardService dashboardService, ReplicationService replicationService) {
        this.userService = userService;
        this.metadataMapper = metadataMapper;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.containerService = containerService;
        this.dashboardService = dashboardService;
        this.replicationService = replicationService;
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
                if (isSystem(principal)) {
                    log.debug("filter request to contain only databases that match internal name: {}", internalName);
                    databases = databaseService.findByInternalName(internalName);
                } else {
                    log.debug("filter request to contain only public databases or where user with id {} has at least read access that match internal name: {}", getUsername(principal), internalName);
                    databases = databaseService.findAllPublicOrSchemaPublicOrReadAccessByInternalName(getUsername(principal), internalName);
                }
            } else {
                if (isSystem(principal)) {
                    databases = databaseService.findAll();
                } else {
                    log.debug("filter request to contain only databases where user with id {} has at least read access", getUsername(principal));
                    databases = databaseService.findAllPublicOrSchemaPublicOrReadAccess(getUsername(principal));
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
            ContainerQuotaException, DashboardServiceException, DashboardServiceConnectionException {
        log.debug("endpoint create database, data.name={}", data.getName());
        final Container container = containerService.find(data.getCid());
        if (container.getQuota() != null && container.getDatabases().size() + 1 > container.getQuota()) {
            log.error("Failed to create database: quota of {} exceeded", container.getQuota());
            throw new ContainerQuotaException("Failed to create database: quota of " + container.getQuota() + " exceeded");
        }
        final User caller = userService.findByUsername(getUsername(principal));
        final Database database = databaseService.create(container, data, caller, userService.findAllInternalUsers());
        /* find in dashboard service */
        final CreateDashboardResponseDto dashboard = dashboardService.create(database);
        database.setDashboardUid(dashboard.getUid());

        // Handle replication after the transaction is committed
        if (data.getCreationLocation() == null && data.getReplicaUrls() != null && data.getReplicaUrls().size() > 0) {
            log.debug("Triggering replication for database - id: {}, creationLocation: null, replicaUrls: {}", 
                    database.getId(), data.getReplicaUrls());
            try {
                replicationService.replicateDatabase(data, database.getId());
            } catch (Exception e) {
                log.error("Failed to trigger replication for database {}: {}", database.getId(), e.getMessage());
                // Don't fail the database creation if replication fails
            }
        } else {
            log.debug("Skipping replication - creationLocation: {}, replicaUrls size: {}", 
                    data.getCreationLocation(), data.getReplicaUrls() != null ? data.getReplicaUrls().size() : 0);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.databaseToDatabaseBriefDto(database));
    }

    @PostMapping("/replicate")
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_database_replicate")
    @Operation(summary = "Replicate database creation",
            description = "Creates a database from replication notification. Requires system authority.",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Database created successfully from replication",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Database create query is malformed or image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Database create permission is missing",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find container/user/database in metadata database",
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
    public ResponseEntity<Map<String, Object>> replicateDatabase(@Valid @RequestBody DatabaseNotificationDto databaseNotificationDto,
                                                                 Principal principal)
            throws DataServiceException, DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException, SearchServiceException, SearchServiceConnectionException,
            ContainerQuotaException, DashboardServiceException, DashboardServiceConnectionException {
        
        log.debug("endpoint replicate database, data.name={}", databaseNotificationDto.getCreateDatabaseDto().getName());
        
        final CreateDatabaseDto data = databaseNotificationDto.getCreateDatabaseDto();
        final Container container = containerService.find(data.getCid());
        
        if (container.getQuota() != null && container.getDatabases().size() + 1 > container.getQuota()) {
            log.error("Failed to create database: quota of {} exceeded", container.getQuota());
            throw new ContainerQuotaException("Failed to create database: quota of " + container.getQuota() + " exceeded");
        }


        final User caller = userService.findByUsername(getUsername(principal));
        final Database database = databaseService.create(container, data, caller, userService.findAllInternalUsers(), databaseNotificationDto.getCreationId());

        /* find in dashboard service */
        final CreateDashboardResponseDto dashboard = dashboardService.create(database);
        database.setDashboardUid(dashboard.getUid());

        Map<String, Object> response = Map.of(
            "status", "success",
            "message", "Database created successfully from replication",
            "databaseId", database.getId().toString(),
            "databaseName", database.getName(),
            "creationId", databaseNotificationDto.getCreationId().toString()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
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
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
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
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
            log.error("Failed to modify database visibility: not owner");
            throw new NotAllowedException("Failed to modify database visibility: not owner");
        }
        final Database database1 = databaseService.modifyVisibility(database, data);
        dashboardService.update(database1);
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
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
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
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint transfer database, databaseId={}, transferDto.username={}", databaseId, data.getUsername());
        final Database database = databaseService.findById(databaseId);
        final User newOwner = userService.findByUsername(data.getUsername());
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
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
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
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
        final HttpHeaders headers = new HttpHeaders();
        if (isSystem(principal)) {
            log.trace("attach privileged credential information");
            headers.set("X-Host", database.getContainer().getHost());
            headers.set("X-Port", "" + database.getContainer().getPort());
            headers.set("X-Username", database.getContainer().getPrivilegedUsername());
            headers.set("X-Password", database.getContainer().getPrivilegedPassword());
            headers.set("X-Jdbc-Method", database.getContainer().getImage().getJdbcMethod());
            headers.set("Access-Control-Expose-Headers", "X-Username X-Password X-Jdbc-Method X-Host X-Port");
        } else {
            removeInternalData(dto.getContainer());
        }
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(dto);
    }

    @PutMapping("/{databaseId}/replication-url")
    @Transactional
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_database_replication_url_update")
    @Operation(summary = "Update database replication URL",
            description = "Updates the replication URL with the remote database ID for a given database. Only the database owner can perform this operation. Requires role `modify-database-replication`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Replication URL updated successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "The replication URL update payload is malformed or replication URL not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Replication URL update is not permitted",
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
    public ResponseEntity<DatabaseBriefDto> updateReplicationUrl(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                                 @Valid @RequestBody DatabaseUpdateReplicationUrlDto data,
                                                                 Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint update replication URL, databaseId={}, replicaUrl={}, replicaDatabaseId={}", 
                databaseId, data.getReplicaUrl(), data.getReplicaDatabaseId());
        
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
            log.error("Failed to update replication URL: not owner");
            throw new NotAllowedException("Failed to update replication URL: not owner");
        }
        
        final Database updatedDatabase = databaseService.updateReplicationUrl(databaseId, data);
        return ResponseEntity.accepted()
                .body(metadataMapper.databaseDtoToDatabaseBriefDto(metadataMapper.databaseToDatabaseDto(updatedDatabase)));
    }

}
