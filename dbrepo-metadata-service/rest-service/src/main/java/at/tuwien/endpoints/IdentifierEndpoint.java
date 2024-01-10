package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.querystore.Query;
import at.tuwien.service.*;
import at.tuwien.utils.PrincipalUtil;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/identifier")
public class IdentifierEndpoint {

    private final UserService userService;
    private final ViewService viewService;
    private final TableService tableService;
    private final StoreService storeService;
    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final MetadataService metadataService;
    private final IdentifierMapper identifierMapper;
    private final EndpointValidator endpointValidator;
    private final IdentifierService identifierService;

    @Autowired
    public IdentifierEndpoint(UserService userService, ViewService viewService, TableService tableService,
                              StoreService storeService, AccessService accessService, DatabaseService databaseService,
                              MetadataService metadataService, IdentifierMapper identifierMapper,
                              EndpointValidator endpointValidator, IdentifierService identifierService) {
        this.userService = userService;
        this.viewService = viewService;
        this.tableService = tableService;
        this.storeService = storeService;
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.metadataService = metadataService;
        this.identifierMapper = identifierMapper;
        this.endpointValidator = endpointValidator;
        this.identifierService = identifierService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_identifier_findall")
    @Operation(summary = "Find identifiers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List identifiers",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = IdentifierDto.class)))}),
    })
    public ResponseEntity<List<IdentifierDto>> list(@RequestParam(required = false) Long dbid,
                                                    @RequestParam(required = false) Long qid,
                                                    @RequestParam(required = false) Long vid,
                                                    @RequestParam(required = false) Long tid,
                                                    @RequestParam(required = false) IdentifierTypeDto type) {
        log.debug("endpoint find identifiers, dbid={}, qid={}, vid={}, tid={}, type={}", dbid, qid, vid, tid, type);
        final List<IdentifierDto> dto = identifierService.findAll(type, dbid, qid, vid, tid)
                .stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .collect(Collectors.toList());
        log.info("Find identifiers resulted in {} identifiers", dto.size());
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Transactional
    @Observed(name = "dbr_identifier_create")
    @PreAuthorize("hasAuthority('create-identifier') or hasAuthority('create-foreign-identifier')")
    @Operation(summary = "Create identifier", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created identifier",
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
            @ApiResponse(responseCode = "405",
                    description = "Creating identifier not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "Creating identifier not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Identifier for this resource already exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Query information could not be retrieved",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<IdentifierDto> create(@NotNull @Valid @RequestBody IdentifierSaveDto data,
                                                @NotNull Principal principal)
            throws IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException, IdentifierRequestException,
            NotAllowedException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException, IdentifierNotFoundException, TableNotFoundException, TableMalformedException,
            QueryMalformedException, FileStorageException, DataDbSidecarException {
        log.debug("endpoint create identifier, data={}, {}", data, PrincipalUtil.formatForDebug(principal));
        DatabaseAccess access = null;
        try {
            access = accessService.find(data.getDatabaseId(), UserUtil.getId(principal));
        } catch (AccessDeniedException e) {
            if (!UserUtil.hasRole(principal, "create-foreign-identifier")) {
                log.error("Failed to create identifier: insufficient role");
                throw new NotAllowedException("Failed to create identifier: insufficient role");
            }
        }
        final Database database = databaseService.find(data.getDatabaseId());
        switch (data.getType()) {
            case VIEW -> {
                if (data.getDatabaseId() == null || data.getQueryId() != null || data.getViewId() == null || data.getTableId() != null) {
                    log.error("Failed to create view identifier: only parameters database_id & view_id must be present");
                    throw new IdentifierRequestException("Failed to create view identifier: only parameters database_id & view_id must be present");
                }
                final View view = viewService.findById(data.getViewId());
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(view.getCreatedBy(), principal, access, "create-foreign-identifier")) {
                    log.error("Failed to create view identifier: insufficient access or role");
                    throw new IdentifierRequestException("Failed to create view identifier: insufficient access or role");
                }
            }
            case TABLE -> {
                if (data.getDatabaseId() == null || data.getQueryId() != null || data.getViewId() != null || data.getTableId() == null) {
                    log.error("Failed to create table identifier: only parameters database_id & table_id must be present");
                    throw new IdentifierRequestException("Failed to create table identifier: only parameters database_id & table_id must be present");
                }
                final Table table = tableService.find(data.getDatabaseId(), data.getTableId());
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(table.getOwnedBy(), principal, access, "create-foreign-identifier")) {
                    log.error("Failed to create table identifier: insufficient access or role");
                    throw new IdentifierRequestException("Failed to create table identifier: insufficient access or role");
                }
            }
            case SUBSET -> {
                if (data.getDatabaseId() == null || data.getQueryId() == null || data.getViewId() != null || data.getTableId() != null) {
                    log.error("Failed to create subset identifier: only parameters database_id & query_id must be present");
                    throw new IdentifierRequestException("Failed to create subset identifier: only parameters database_id & query_id must be present");
                }
                final Query query = storeService.findOne(data.getDatabaseId(), data.getQueryId(), principal);
                final User user = userService.findByUsername(query.getCreatedBy());
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(user.getId(), principal, access, "create-foreign-identifier")) {
                    log.error("Failed to create subset identifier: insufficient access or role");
                    throw new IdentifierRequestException("Failed to create subset identifier: insufficient access or role");
                }
            }
            case DATABASE -> {
                if (data.getDatabaseId() == null || data.getQueryId() != null || data.getViewId() != null || data.getTableId() != null) {
                    log.error("Failed to create database identifier: only parameters database_id must be present");
                    throw new IdentifierRequestException("Failed to create database identifier: only parameters database_id must be present");
                }
                if (!endpointValidator.validateOnlyMineOrReadAccessOrHasRole(database.getOwnedBy(), principal, access, "create-foreign-identifier")) {
                    log.error("Failed to create database identifier: insufficient access or role");
                    throw new IdentifierRequestException("Failed to create database identifier: insufficient access or role");
                }
            }
        }
        final Identifier identifier = identifierService.create(data, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(identifierMapper.identifierToIdentifierDto(identifier));
    }

    @GetMapping("/retrieve")
    @Observed(name = "dbr_identifier_retrieve")
    @Operation(summary = "Retrieve metadata from identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved metadata from identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
    })
    public ResponseEntity<ExternalMetadataDto> retrieve(@NotNull @Valid @RequestParam String url)
            throws OrcidNotFoundException, RorNotFoundException, RemoteUnavailableException, DoiNotFoundException {
        return ResponseEntity.ok(metadataService.findByUrl(url));
    }


}
