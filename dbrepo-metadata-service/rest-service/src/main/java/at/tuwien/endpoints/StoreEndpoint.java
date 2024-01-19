package at.tuwien.endpoints;

import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.UserMapper;
import at.tuwien.querystore.Query;
import at.tuwien.service.AccessService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.StoreService;
import at.tuwien.service.UserService;
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
import java.util.stream.Collectors;

import static org.apache.jena.sparql.vocabulary.VocabTestQuery.query;

@Log4j2
@RestController
@RequestMapping("/api/database/{databaseId}/query")
public class StoreEndpoint {

    private final UserMapper userMapper;
    private final QueryMapper queryMapper;
    private final UserService userService;
    private final StoreService storeService;
    private final AccessService accessService;
    private final IdentifierMapper identifierMapper;
    private final EndpointValidator endpointValidator;
    private final IdentifierService identifierService;

    @Autowired
    public StoreEndpoint(UserMapper userMapper, QueryMapper queryMapper, UserService userService, StoreService storeService,
                         AccessService accessService, IdentifierMapper identifierMapper,
                         EndpointValidator endpointValidator, IdentifierService identifierService) {
        this.userMapper = userMapper;
        this.queryMapper = queryMapper;
        this.userService = userService;
        this.storeService = storeService;
        this.accessService = accessService;
        this.identifierMapper = identifierMapper;
        this.endpointValidator = endpointValidator;
        this.identifierService = identifierService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_queries_findall")
    @Operation(summary = "Find queries", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List queries",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = QueryBriefDto.class)))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find all queries is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Selection of time-versioned query resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Query store failed to select query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<QueryBriefDto>> findAll(@NotNull @PathVariable("databaseId") Long databaseId,
                                                       @RequestParam(value = "persisted", required = false) Boolean persisted,
                                                       Principal principal) throws QueryStoreException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException,
            DatabaseConnectionException, TableMalformedException, UserNotFoundException, NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint list queries, databaseId={}, persisted={}, {}", databaseId, persisted, PrincipalUtil.formatForDebug(principal));
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        /* find all from data database */
        final List<Query> queries = storeService.findAll(databaseId, persisted, principal);
        /* add identifiers and creator from metadata database */
        final List<IdentifierBriefDto> identifiers = identifierService.findAllSubsetIdentifiers()
                .stream()
                .map(identifierMapper::identifierToIdentifierBriefDto)
                .toList();
        final List<UserDto> users = userService.findAll()
                .stream()
                .map(userMapper::userToUserDto)
                .toList();
        final List<QueryBriefDto> dto = queries.stream()
                .map(queryMapper::queryToQueryBriefDto)
                .peek(q -> {
                    q.setDatabaseId(databaseId);
                    users.stream()
                            .filter(u -> u.getId().equals(q.getCreatedBy()))
                            .findFirst()
                            .ifPresentOrElse(q::setCreator, () -> log.warn("Query creator with id {} not found in list of users", q.getCreatedBy()));
                    q.setIdentifiers(identifiers.stream()
                            .filter(i -> i.getDatabaseId().equals(databaseId) && i.getQueryId().equals(q.getId()))
                            .toList());
                })
                .collect(Collectors.toList());
        log.trace("find queries resulted in queries {}", dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{queryId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbr_queries_find")
    @Operation(summary = "Find some query", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List queries",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, query or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find query is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Query store failed to select query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryDto> find(@NotNull @PathVariable("databaseId") Long databaseId,
                                         @NotNull @PathVariable Long queryId,
                                         Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            QueryStoreException, QueryNotFoundException, UserNotFoundException, NotAllowedException,
            DatabaseConnectionException, KeycloakRemoteException, AccessDeniedException {
        log.debug("endpoint find query, databaseId={}, queryId={}, {}", databaseId, queryId, PrincipalUtil.formatForDebug(principal));
        /* check */
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        /* find */
        final Query query = storeService.findOne(databaseId, queryId, principal);
        final QueryDto dto = queryMapper.queryToQueryDto(query);
        dto.setDatabaseId(databaseId);
        dto.setCreator(userMapper.userToUserDto(userService.find(query.getCreatedBy())));
        final List<Identifier> identifiers = identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
        if (!identifiers.isEmpty()) {
            dto.setIdentifiers(identifiers.stream()
                    .map(identifierMapper::identifierToIdentifierDto)
                    .toList());
        }
        log.trace("find query resulted in query {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{queryId}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('persist-query')")
    @Observed(name = "dbr_query_persist")
    @Operation(summary = "Persist some query", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Persist query successful",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Image not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to persist query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, query or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Persist query is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "412",
                    description = "Query is already persisted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<QueryDto> persist(@NotNull @PathVariable("databaseId") Long databaseId,
                                            @NotNull @PathVariable("queryId") Long queryId,
                                            @NotNull @Valid @RequestBody QueryPersistDto data,
                                            @NotNull Principal principal)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException, UserNotFoundException,
            NotAllowedException, AccessDeniedException, IdentifierAlreadyPublishedException {
        log.debug("endpoint persist query, container, databaseId={}, queryId={}, data.persist={}, {}", databaseId, queryId, data.getPersist(), PrincipalUtil.formatForDebug(principal));
        /* check */
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        /* has access */
        accessService.find(databaseId, UserUtil.getId(principal));
        /* persist */
        final Query query = storeService.persist(databaseId, queryId, data);
        final QueryDto dto = queryMapper.queryToQueryDto(query);
        dto.setCreator(userMapper.userToUserDto(userService.find(query.getCreatedBy())));
        log.trace("persist query resulted in query {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }
}
