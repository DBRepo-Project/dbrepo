package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.config.EndpointConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld.LdDatasetDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalMetadataDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierStatusType;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@Validated
@RequestMapping(path = "/api/identifier")
public class IdentifierEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final AccessService accessService;
    private final EndpointConfig endpointConfig;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final MetadataService metadataService;
    private final EndpointValidator endpointValidator;
    private final IdentifierService identifierService;

    private static final String CREATE_FOREIGN_IDENTIFIER_ROLE = "create-foreign-identifier";

    @Autowired
    public IdentifierEndpoint(UserService userService, AccessService accessService, EndpointConfig endpointConfig,
                              MetadataMapper metadataMapper, DatabaseService databaseService,
                              MetadataService metadataService, EndpointValidator endpointValidator,
                              IdentifierService identifierService) {
        this.userService = userService;
        this.accessService = accessService;
        this.endpointConfig = endpointConfig;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.metadataService = metadataService;
        this.endpointValidator = endpointValidator;
        this.identifierService = identifierService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_identifier_list")
    @Operation(summary = "List identifiers",
            description = "Lists all identifiers known to the metadata database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found identifiers successfully",
                    content = {
                            @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = IdentifierBriefDto[].class))),
                            @Content(mediaType = "application/ld+json",
                                    array = @ArraySchema(schema = @Schema(implementation = LdDatasetDto.class)))
                    }),
    })
    public ResponseEntity<?> findAll(@Valid @RequestParam(value = "type", required = false) IdentifierTypeDto type,
                                     @Valid @RequestParam(value = "status", required = false) IdentifierStatusTypeDto status,
                                     @Valid @RequestParam(value = "dbid", required = false) UUID dbid,
                                     @Valid @RequestParam(value = "qid", required = false) UUID qid,
                                     @Valid @RequestParam(value = "vid", required = false) UUID vid,
                                     @Valid @RequestParam(value = "tid", required = false) UUID tid,
                                     @RequestHeader(HttpHeaders.ACCEPT) String accept,
                                     Principal principal) {
        log.debug("endpoint find identifiers, type={}, status={}, dbid={}, qid={}, vid={}, tid={}, accept={}", type,
                status, dbid, qid, vid, tid, accept);
        final List<Identifier> identifiers = identifierService.findAll()
                .stream()
                .filter(i -> !Objects.nonNull(type) || metadataMapper.identifierTypeDtoToIdentifierType(type).equals(i.getType()))
                .filter(i -> !Objects.nonNull(status) || metadataMapper.identifierStatusTypeDtoToIdentifierStatusType(status).equals(i.getStatus()))
                .filter(i -> !Objects.nonNull(dbid) || dbid.equals(i.getDatabase().getId()))
                .filter(i -> !Objects.nonNull(qid) || qid.equals(i.getQueryId()))
                .filter(i -> !Objects.nonNull(vid) || vid.equals(i.getViewId()))
                .filter(i -> !Objects.nonNull(tid) || tid.equals(i.getTableId()))
                .filter(i -> principal != null && i.getStatus().equals(IdentifierStatusType.DRAFT) ? i.getOwnedBy().equals(getId(principal)) : i.getStatus().equals(IdentifierStatusType.PUBLISHED))
                .toList();
        if (identifiers.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        log.trace("found persistent identifiers {}", identifiers);
        if (accept.equals("application/ld+json")) {
            log.trace("accept header matches json-ld");
            return ResponseEntity.ok(identifiers.stream()
                    .map(i -> metadataMapper.identifierToLdDatasetDto(i, endpointConfig.getWebsiteUrl()))
                    .toList());
        }
        log.trace("default to json");
        return ResponseEntity.ok(identifiers.stream()
                .map(metadataMapper::identifierToIdentifierBriefDto)
                .toList());
    }

    @GetMapping(value = "/{identifierId}", produces = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json",
            MediaType.TEXT_XML_VALUE, "text/bibliography", "text/bibliography; style=apa",
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
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to view identifier",
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
    public ResponseEntity<?> find(@Valid @PathVariable("identifierId") UUID identifierId,
                                  @RequestHeader(HttpHeaders.ACCEPT) String accept,
                                  Principal principal) throws IdentifierNotFoundException,
            DataServiceException, DataServiceConnectionException, MalformedException, FormatNotAvailableException,
            QueryNotFoundException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        log.debug("endpoint find identifier, identifierId={}, accept={}", identifierId, accept);
        if (accept == null) {
            accept = "";
        }
        final Identifier identifier = identifierService.find(identifierId);
        if (identifier.getStatus().equals(IdentifierStatusType.DRAFT)) {
            if (principal == null) {
                throw new NotAllowedException("Draft identifier: authentication required");
            }
            if (!identifier.getOwnedBy().equals(getId(principal))) {
                throw new NotAllowedException("Draft identifier: not authorized");
            }
        }
        log.info("Found persistent identifier with id: {}", identifier.getId());
        switch (accept) {
            case "application/json":
                log.trace("accept header matches json");
                return ResponseEntity.ok(metadataMapper.identifierToIdentifierDto(identifier));
            case "application/ld+json":
                log.trace("accept header matches json-ld");
                return ResponseEntity.ok(metadataMapper.identifierToLdDatasetDto(identifier, endpointConfig.getWebsiteUrl()));
            case "text/xml":
                log.trace("accept header matches xml");
                return ResponseEntity.ok(identifierService.exportMetadata(identifier));
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
    public ResponseEntity<Void> delete(@NotNull @PathVariable("identifierId") UUID identifierId)
            throws IdentifierNotFoundException, NotAllowedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
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
    public ResponseEntity<IdentifierDto> publish(@Valid @PathVariable("identifierId") UUID identifierId)
            throws SearchServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            MalformedException, DataServiceConnectionException, IdentifierNotFoundException, ExternalServiceException {
        log.debug("endpoint publish identifier, identifierId={}", identifierId);
        return ResponseEntity.accepted()
                .body(metadataMapper.identifierToIdentifierDto(
                        identifierService.publish(identifierService.find(identifierId))));
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
    public ResponseEntity<IdentifierDto> save(@NotNull @PathVariable("identifierId") UUID identifierId,
                                              @NotNull @Valid @RequestBody IdentifierSaveDto data,
                                              Principal principal) throws UserNotFoundException,
            DatabaseNotFoundException, MalformedException, NotAllowedException, DataServiceException,
            DataServiceConnectionException, SearchServiceException, QueryNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException,
            TableNotFoundException, ExternalServiceException {
        log.debug("endpoint save identifier, identifierId={}, data.id={}", identifierId,
                data.getId());
        final Database database = databaseService.findById(data.getDatabaseId());
        final User caller = userService.findById(getId(principal));
        final Identifier identifier = identifierService.find(identifierId);
        /* check owner */
        if (!identifier.getOwner().getId().equals(getId(principal)) && !hasRole(principal, CREATE_FOREIGN_IDENTIFIER_ROLE)) {
            log.error("Failed to save identifier: foreign user");
            throw new NotAllowedException("Failed to save identifier: foreign user");
        }
        /* check data */
        if (!endpointValidator.validatePublicationDate(data)) {
            log.error("Failed to save identifier: publication date is invalid");
            throw new MalformedException("Failed to save identifier: publication date is invalid");
        }
        /* check access */
        try {
            final DatabaseAccess access = accessService.find(database, caller);
            log.trace("found access: {}", access);
        } catch (AccessNotFoundException e) {
            if (!hasRole(principal, CREATE_FOREIGN_IDENTIFIER_ROLE)) {
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
            }
            case TABLE -> {
                if (data.getQueryId() != null || data.getViewId() != null || data.getTableId() == null) {
                    log.error("Failed to save table identifier: only parameters database_id & table_id must be present");
                    throw new MalformedException("Failed to save table identifier: only parameters database_id & table_id must be present");
                }
            }
            case SUBSET -> {
                if (data.getQueryId() == null || data.getViewId() != null || data.getTableId() != null) {
                    log.error("Failed to save subset identifier: only parameters database_id & query_id must be present");
                    throw new MalformedException("Failed to save subset identifier: only parameters database_id & query_id must be present");
                }
                log.debug("retrieving subset from data service: data.database_id={}, data.query_id={}", data.getDatabaseId(), data.getQueryId());
            }
            case DATABASE -> {
                if (data.getQueryId() != null || data.getViewId() != null || data.getTableId() != null) {
                    log.error("Failed to save database identifier: only parameters database_id must be present");
                    throw new MalformedException("Failed to save database identifier: only parameters database_id must be present");
                }
            }
        }

        return ResponseEntity.accepted()
                .body(metadataMapper.identifierToIdentifierDto(identifierService.save(database, caller, data)));
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
    public ResponseEntity<IdentifierDto> create(@NotNull @Valid @RequestBody CreateIdentifierDto data,
                                                Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, MalformedException, DataServiceConnectionException,
            SearchServiceException, DataServiceException, QueryNotFoundException, SearchServiceConnectionException,
            IdentifierNotFoundException, ViewNotFoundException, ExternalServiceException {
        log.debug("endpoint create identifier, data.databaseId={}", data.getDatabaseId());
        final Database database = databaseService.findById(data.getDatabaseId());
        final User caller = userService.findById(getId(principal));
        /* check access */
        try {
            accessService.find(database, caller);
        } catch (AccessNotFoundException e) {
            if (!hasRole(principal, CREATE_FOREIGN_IDENTIFIER_ROLE)) {
                log.error("Failed to create identifier: insufficient role");
                throw new NotAllowedException("Failed to create identifier: insufficient role");
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.identifierToIdentifierDto(identifierService.create(database, caller, data)));
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
    public ResponseEntity<ExternalMetadataDto> retrieve(@NotNull @Valid @RequestParam("url") String url)
            throws OrcidNotFoundException, RorNotFoundException, DoiNotFoundException, IdentifierNotSupportedException {
        log.debug("endpoint retrieve identifier, url={}", url);
        return ResponseEntity.ok(metadataService.findByUrl(url));
    }


}
