package at.tuwien.endpoints;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.*;
import at.tuwien.api.identifier.ld.LdDatasetDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierStatusType;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.*;
import at.tuwien.utils.UserUtil;
import at.tuwien.validation.EndpointValidator;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/identifier")
public class IdentifierEndpoint {

    private final UserService userService;
    private final ViewService viewService;
    private final TableService tableService;
    private final AccessService accessService;
    private final EndpointConfig endpointConfig;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final MetadataService metadataService;
    private final EndpointValidator endpointValidator;
    private final IdentifierService identifierService;
    private final DataServiceGateway dataServiceGateway;

    @Autowired
    public IdentifierEndpoint(UserService userService, ViewService viewService, TableService tableService,
                              AccessService accessService, EndpointConfig endpointConfig, MetadataMapper metadataMapper,
                              DatabaseService databaseService, MetadataService metadataService,
                              EndpointValidator endpointValidator, IdentifierService identifierService,
                              DataServiceGateway dataServiceGateway) {
        this.userService = userService;
        this.viewService = viewService;
        this.tableService = tableService;
        this.accessService = accessService;
        this.endpointConfig = endpointConfig;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.metadataService = metadataService;
        this.endpointValidator = endpointValidator;
        this.identifierService = identifierService;
        this.dataServiceGateway = dataServiceGateway;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"})
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_identifier_list")
    @Operation(summary = "List identifiers",
            description = "Lists all identifiers known to the metadata database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found identifiers successfully",
                    content = {
                            @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ConceptDto.class))),
                            @Content(mediaType = "application/ld+json",
                                    array = @ArraySchema(schema = @Schema(implementation = LdDatasetDto.class)))
                    }),
            @ApiResponse(responseCode = "406",
                    description = "Identifier could not be exported, the requested style is not known",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> findAll(@Valid @RequestParam(value = "dbid", required = false) Long dbid,
                                     @Valid @RequestParam(value = "qid", required = false) Long qid,
                                     @Valid @RequestParam(value = "vid", required = false) Long vid,
                                     @Valid @RequestParam(value = "tid", required = false) Long tid,
                                     @RequestHeader(HttpHeaders.ACCEPT) String accept)
            throws FormatNotAvailableException {
        log.debug("endpoint find identifiers, dbid={}, qid={}, vid={}, tid={}, accept={}", dbid, qid, vid, tid, accept);
        final List<Identifier> identifiers = identifierService.findAll()
                .stream()
                .filter(i -> !Objects.nonNull(dbid) || dbid.equals(i.getDatabase().getId()))
                .filter(i -> !Objects.nonNull(qid) || qid.equals(i.getQueryId()))
                .filter(i -> !Objects.nonNull(vid) || vid.equals(i.getViewId()))
                .filter(i -> !Objects.nonNull(tid) || tid.equals(i.getTableId()))
                .toList();
        if (identifiers.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        log.trace("found persistent identifiers {}", identifiers);
        switch (accept) {
            case "application/json":
                log.trace("accept header matches json");
                final List<IdentifierDto> resource1 = identifiers.stream()
                        .map(metadataMapper::identifierToIdentifierDto)
                        .toList();
                log.debug("find identifier resulted in identifiers {}", resource1);
                return ResponseEntity.ok(resource1);
            case "application/ld+json":
                log.trace("accept header matches json-ld");
                final List<LdDatasetDto> resource2 = identifiers.stream()
                        .map(i -> metadataMapper.identifierToLdDatasetDto(i, endpointConfig.getWebsiteUrl()))
                        .toList();
                log.debug("find identifier resulted in identifiers {}", resource2);
                return ResponseEntity.ok(resource2);
        }
        throw new FormatNotAvailableException("Must provide either application/json or application/ld+json headers");
    }

    @GetMapping(value = "/{identifierId}", produces = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json",
            MediaType.TEXT_XML_VALUE, "text/csv", "text/bibliography", "text/bibliography; style=apa",
            "text/bibliography; style=ieee", "text/bibliography; style=bibtex"})
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_identifier_find")
    @Operation(summary = "Find identifier",
            description = "Finds an identifier with id. The response format depends on the HTTP `Accept` header set on the request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found identifier successfully",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = IdentifierDto.class)),
                            @Content(mediaType = "application/ld+json", schema = @Schema(implementation = LdDatasetDto.class)),
                            @Content(mediaType = "text/csv"),
                            @Content(mediaType = "text/xml"),
                            @Content(mediaType = "text/bibliography"),
                            @Content(mediaType = "text/bibliography; style=apa"),
                            @Content(mediaType = "text/bibliography; style=ieee"),
                            @Content(mediaType = "text/bibliography; style=bibtex"),
                    }),
            @ApiResponse(responseCode = "400",
                    description = "Identifier could not be exported, the requested style is not known",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Identifier could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to find acceptable representation",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Exported resource was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "Failed to retrieve from S3 endpoint",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Failed to retrieve from database sidecar",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to data service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to find in data service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> find(@Valid @PathVariable("identifierId") Long identifierId,
                                  @RequestHeader(HttpHeaders.ACCEPT) String accept) throws IdentifierNotFoundException,
            ServiceException, ServiceConnectionException, MalformedException, FormatNotAvailableException,
            QueryNotFoundException {
        log.debug("endpoint find identifier, identifierId={}, accept={}", identifierId, accept);
        final Identifier identifier = identifierService.find(identifierId);
        log.info("Found persistent identifier with id {}", identifier.getId());
        log.trace("found persistent identifier {}", identifier);
        if (accept != null) {
            log.trace("accept header present: {}", accept);
            switch (accept) {
                case "application/json":
                    log.trace("accept header matches json");
                    final IdentifierDto resource1 = metadataMapper.identifierToIdentifierDto(identifier);
                    log.debug("find identifier resulted in identifier {}", resource1);
                    return ResponseEntity.ok(resource1);
                case "application/ld+json":
                    log.trace("accept header matches json-ld");
                    final LdDatasetDto resource2 = metadataMapper.identifierToLdDatasetDto(identifier, endpointConfig.getWebsiteUrl());
                    log.debug("find identifier resulted in identifier {}", resource2);
                    return ResponseEntity.ok(resource2);
                case "text/csv":
                    log.trace("accept header matches csv");
                    if (identifier.getType().equals(IdentifierType.DATABASE)) {
                        log.error("Failed to export dataset: identifier type is database");
                        throw new FormatNotAvailableException("Failed to export dataset: identifier type is database");
                    }
                    final InputStreamResource resource3;
                    resource3 = identifierService.exportResource(identifier);
                    log.debug("find identifier resulted in resource {}", resource3);
                    return ResponseEntity.ok(resource3);
                case "text/xml":
                    log.trace("accept header matches xml");
                    final InputStreamResource resource4 = identifierService.exportMetadata(identifier);
                    log.debug("find identifier resulted in resource {}", resource4);
                    return ResponseEntity.ok(resource4);
            }
            final Pattern regex = Pattern.compile("text\\/bibliography(; ?style=(apa|ieee|bibtex))?");
            final Matcher matcher = regex.matcher(accept);
            if (matcher.find()) {
                log.trace("accept header matches bibliography");
                final BibliographyTypeDto style;
                if (matcher.group(2) != null) {
                    style = BibliographyTypeDto.valueOf(matcher.group(2).toUpperCase());
                    log.trace("bibliography style matches {}", style);
                } else {
                    style = BibliographyTypeDto.APA;
                    log.trace("no bibliography style provided, default: {}", style);
                }
                final String resource = identifierService.exportBibliography(identifier, style);
                log.debug("find identifier resulted in resource {}", resource);
                return ResponseEntity.ok(resource);
            }
        } else {
            log.trace("no accept header present");
        }
        final HttpHeaders headers = new HttpHeaders();
        final String url = metadataMapper.identifierToLocationUrl(endpointConfig.getWebsiteUrl(), identifier);
        headers.add("Location", url);
        log.debug("find identifier resulted in http redirect, headers={}, url={}", headers, url);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .headers(headers)
                .build();
    }

    @DeleteMapping("/{identifierId}")
    @Transactional
    @Observed(name = "dbrepo_identifier_delete")
    @PreAuthorize("hasAuthority('delete-identifier')")
    @Operation(summary = "Delete identifier",
            description = "Deletes an identifier with id. Requires role `delete-identifier`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted identifier"),
            @ApiResponse(responseCode = "403",
                    description = "Deleting identifier not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Identifier or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to delete in search service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("identifierId") Long identifierId)
            throws IdentifierNotFoundException, NotAllowedException, ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint delete identifier, identifierId={}", identifierId);
        final Identifier identifier = identifierService.find(identifierId);
        if (identifier.getStatus().equals(IdentifierStatusType.PUBLISHED)) {
            log.error("Failed to delete identifier: already published");
            throw new NotAllowedException("Failed to delete identifier: already published");
        }
        identifierService.delete(identifier);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{identifierId}/publish")
    @Transactional
    @Observed(name = "dbrepo_identifier_publish")
    @PreAuthorize("hasAuthority('publish-identifier')")
    @Operation(summary = "Publish identifier",
            description = "Publishes an identifier with id. A published identifier cannot be changed anymore. Requires role `publish-identifier`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Published identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Identifier form contains invalid request data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient access rights or authorities",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database, table or view",
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
    public ResponseEntity<IdentifierDto> publish(@Valid @PathVariable("identifierId") Long identifierId)
            throws SearchServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            MalformedException, ServiceConnectionException, IdentifierNotFoundException {
        log.debug("endpoint publish identifier, identifierId={}", identifierId);
        identifierService.find(identifierId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.identifierToIdentifierDto(identifierService.publish(identifierId)));
    }

    @PutMapping("/{identifierId}")
    @Transactional(rollbackFor = {Exception.class})
    @Observed(name = "dbrepo_identifier_save")
    @PreAuthorize("hasAuthority('create-identifier') or hasAuthority('create-foreign-identifier')")
    @Operation(summary = "Save identifier",
            description = "Saves an identifier with id as a draft identifier. Identifiers can only be created for objects the user has at least *READ* access in the associated database (requires role `create-identifier`) or for any object in any database (requires role `create-foreign-identifier`).",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Saved identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Identifier form contains invalid request data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient access rights or authorities",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database, table or view",
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
    public ResponseEntity<IdentifierDto> save(@NotNull @PathVariable("identifierId") Long identifierId,
                                              @NotNull @Valid @RequestBody IdentifierSaveDto data,
                                              @NotNull Principal principal) throws UserNotFoundException,
            DatabaseNotFoundException, MalformedException, NotAllowedException, ServiceException,
            ServiceConnectionException, SearchServiceException, QueryNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException, TableNotFoundException {
        log.debug("endpoint save identifier, identifierId={}, data.id={}, principal.name={}", identifierId,
                data.getId(), principal.getName());
        final Database database = databaseService.findById(data.getDatabaseId());
        final User user = userService.findByUsername(principal.getName());
        final Identifier identifier = identifierService.find(identifierId);
        /* check owner */
        if (!identifier.getCreator().equals(user) && !UserUtil.hasRole(principal, "create-foreign-identifier")) {
            log.error("Failed to save identifier: foreign user");
            throw new NotAllowedException("Failed to save identifier: foreign user");
        }
        /* check data */
        if (!endpointValidator.validatePublicationDate(data)) {
            log.error("Failed to save identifier: publication date is invalid");
            throw new MalformedException("Failed to save identifier: publication date is invalid");
        }
        /* check access */
        DatabaseAccess access = null;
        try {
            access = accessService.find(database, user);
            log.trace("found access: {}", access);
        } catch (AccessNotFoundException e) {
            if (!UserUtil.hasRole(principal, "create-foreign-identifier")) {
                log.error("Failed to save identifier: insufficient role");
                throw new NotAllowedException("Failed to save identifier: insufficient role");
            }
        }
        switch (data.getType()) {
            case VIEW -> {
                if (data.getQueryId() != null || data.getViewId() == null || data.getTableId() != null) {
                    log.error("Failed to save view identifier: only parameters database_id & view_id must be present");
                    throw new MalformedException("Failed to save view identifier: only parameters database_id & view_id must be present");
                }
                final View view = viewService.findById(database, data.getViewId());
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(view.getCreator(), principal, access, "create-foreign-identifier")) {
                    log.error("Failed to save view identifier: insufficient access or role");
                    throw new MalformedException("Failed to save view identifier: insufficient access or role");
                }
            }
            case TABLE -> {
                if (data.getQueryId() != null || data.getViewId() != null || data.getTableId() == null) {
                    log.error("Failed to save table identifier: only parameters database_id & table_id must be present");
                    throw new MalformedException("Failed to save table identifier: only parameters database_id & table_id must be present");
                }
                final Table table = tableService.findById(data.getDatabaseId(), data.getTableId());
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(table.getOwner(), principal, access, "create-foreign-identifier")) {
                    log.error("Failed to save table identifier: insufficient access or role");
                    throw new MalformedException("Failed to save table identifier: insufficient access or role");
                }
            }
            case SUBSET -> {
                if (data.getQueryId() == null || data.getViewId() != null || data.getTableId() != null) {
                    log.error("Failed to save subset identifier: only parameters database_id & query_id must be present");
                    throw new MalformedException("Failed to save subset identifier: only parameters database_id & query_id must be present");
                }
                log.debug("retrieving subset from data service: data.database_id={}, data.query_id={}", data.getDatabaseId(), data.getQueryId());
                final QueryDto query = dataServiceGateway.findQuery(data.getDatabaseId(), data.getQueryId());
                final User queryCreator = userService.findById(query.getCreator().getId());
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(queryCreator, principal, access, "create-foreign-identifier")) {
                    log.error("Failed to create subset identifier: insufficient access or role");
                    throw new MalformedException("Failed to create subset identifier: insufficient access or role");
                }
            }
            case DATABASE -> {
                if (data.getQueryId() != null || data.getViewId() != null || data.getTableId() != null) {
                    log.error("Failed to save database identifier: only parameters database_id must be present");
                    throw new MalformedException("Failed to save database identifier: only parameters database_id must be present");
                }
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(database.getOwner(), principal, access, "create-foreign-identifier")) {
                    log.error("Failed to save database identifier: insufficient access or role");
                    throw new MalformedException("Failed to save database identifier: insufficient access or role");
                }
            }
        }
        return ResponseEntity.accepted()
                .body(metadataMapper.identifierToIdentifierDto(identifierService.save(database, user, data)));
    }

    @PostMapping
    @Transactional(rollbackFor = {Exception.class})
    @Observed(name = "dbrepo_identifier_create")
    @PreAuthorize("hasAuthority('create-identifier') or hasAuthority('create-foreign-identifier')")
    @Operation(summary = "Create identifier",
            description = "Create an identifier with id to create a draft identifier. Identifiers can only be created for objects the user has at least *READ* access in the associated database (requires role `create-identifier`) or for any object in any database (requires role `create-foreign-identifier`).",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Drafted identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Identifier form contains invalid request data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient access rights or authorities",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database, table or view",
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
    public ResponseEntity<IdentifierDto> create(@NotNull @Valid @RequestBody IdentifierCreateDto data,
                                                @NotNull Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, MalformedException, ServiceConnectionException,
            SearchServiceException, ServiceException, QueryNotFoundException, SearchServiceConnectionException,
            IdentifierNotFoundException, ViewNotFoundException {
        log.debug("endpoint create identifier, data.databaseId={}", data.getDatabaseId());
        final Database database = databaseService.findById(data.getDatabaseId());
        final User user = userService.findByUsername(principal.getName());
        /* check access */
        try {
            final DatabaseAccess access = accessService.find(database, user);
            log.trace("found access: {}", access.getType());
        } catch (AccessNotFoundException e) {
            if (!UserUtil.hasRole(principal, "create-foreign-identifier")) {
                log.error("Failed to create identifier: insufficient role");
                throw new NotAllowedException("Failed to create identifier: insufficient role");
            }
        }
        final Identifier identifier = identifierService.create(database, user, data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.identifierToIdentifierDto(identifier));
    }

    @GetMapping("/retrieve")
    @Observed(name = "dbrepo_identifier_retrieve")
    @Operation(summary = "Retrieve PID metadata",
            description = "Retrieves Persistent Identifier (PID) metadata from external endpoints. Supported PIDs are: ORCID, ROR, DOI.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved metadata from identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find metadata for identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ExternalMetadataDto> retrieve(@NotNull @Valid @RequestParam String url)
            throws OrcidNotFoundException, RorNotFoundException, DoiNotFoundException, IdentifierNotSupportedException {
        return ResponseEntity.ok(metadataService.findByUrl(url));
    }


}
