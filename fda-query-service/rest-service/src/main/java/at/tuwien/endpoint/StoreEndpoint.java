package at.tuwien.endpoint;

import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.user.User;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.mapper.UserMapper;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/query")
public class StoreEndpoint {

    private final UserMapper userMapper;
    private final QueryMapper queryMapper;
    private final UserService userService;
    private final StoreService storeService;
    private final IdentifierMapper identifierMapper;
    private final IdentifierService identifierService;

    @Autowired
    public StoreEndpoint(UserMapper userMapper, QueryMapper queryMapper, UserService userService,
                         StoreService storeService, IdentifierMapper identifierMapper,
                         IdentifierService identifierService) {
        this.userMapper = userMapper;
        this.queryMapper = queryMapper;
        this.userService = userService;
        this.storeService = storeService;
        this.identifierMapper = identifierMapper;
        this.identifierService = identifierService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "store.list", description = "Time needed to list queries from the query store")
    @PreAuthorize("hasAuthority('find-queries')")
    @Operation(summary = "Find queries", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<QueryBriefDto>> findAll(@NotNull @PathVariable("id") Long containerId,
                                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                                       @RequestParam(value = "persisted", required = false) Boolean persisted,
                                                       Principal principal) throws QueryStoreException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException,
            DatabaseConnectionException, TableMalformedException, UserNotFoundException {
        log.debug("endpoint list queries, containerId={}, databaseId={}, persisted={}, principal={}", containerId,
                databaseId, persisted, principal);
        final List<Query> queries = storeService.findAll(containerId, databaseId, persisted, principal);
        final List<Identifier> identifiers = identifierService.findAll();
        final List<User> users = userService.findAll();
        final List<QueryBriefDto> dto = queries.stream()
                .map(q -> {
                    final QueryBriefDto brief = queryMapper.queryToQueryBriefDto(q);
                    final Optional<User> optional1 = users.stream().filter(u -> u.getUsername().equals(q.getCreatedBy()))
                            .findFirst();
                    optional1.ifPresent(user -> brief.setCreator(userMapper.userToUserDto(user)));
                    final Optional<Identifier> optional2 = identifiers.stream()
                            .filter(i -> i.getType().equals(IdentifierType.SUBSET))
                            .filter(i -> i.getDatabaseId().equals(databaseId) && i.getQueryId().equals(q.getId()))
                            .findFirst();
                    optional2.ifPresent(identifier -> brief.setIdentifier(identifierMapper.identifierToIdentifierBriefDto(identifier)));
                    return brief;
                })
                .collect(Collectors.toList());
        log.trace("find queries resulted in queries {}", dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{queryId}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('find-query')")
    @Timed(value = "store.find", description = "Time needed to find a query from the query store")
    @Operation(summary = "Find some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryDto> find(@NotNull @PathVariable("id") Long containerId,
                                         @NotNull @PathVariable("databaseId") Long databaseId,
                                         @NotNull @PathVariable Long queryId,
                                         Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            QueryStoreException, QueryNotFoundException, UserNotFoundException, NotAllowedException,
            DatabaseConnectionException {
        log.debug("endpoint find query, containerId={}, databaseId={}, queryId={}, principal={}", containerId, databaseId,
                queryId, principal);
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        final QueryDto dto = queryMapper.queryToQueryDto(query);
        final User creator = userService.findByUsername(query.getCreatedBy());
        dto.setCreator(userMapper.userToUserDto(creator));
        try {
            final Identifier identifier = identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
            dto.setIdentifier(identifierMapper.identifierToIdentifierDto(identifier));
        } catch (IdentifierNotFoundException e) {
            /* ignore */
        }
        log.trace("find query resulted in query {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{queryId}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('persist-query')")
    @Timed(value = "store.persist", description = "Time needed to persist a query in the query store")
    @Operation(summary = "Persist some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryDto> persist(@NotNull @PathVariable("id") Long containerId,
                                            @NotNull @PathVariable("databaseId") Long databaseId,
                                            @NotNull @PathVariable("queryId") Long queryId,
                                            @NotNull Principal principal)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseConnectionException, UserNotFoundException, QueryNotFoundException,
            QueryAlreadyPersistedException {
        log.debug("endpoint persist query, container, containerId={}, databaseId={}, queryId={}, principal={}",
                containerId, databaseId, queryId, principal);
        final Query check = storeService.findOne(containerId, databaseId, queryId, principal);
        if (check.getIsPersisted()) {
            log.error("Failed to persist, is already persisted");
            throw new QueryAlreadyPersistedException("Failed to persist");
        }
        final Query query = storeService.persist(containerId, databaseId, queryId, principal);
        final QueryDto dto = queryMapper.queryToQueryDto(query);
        final User creator = userService.findByUsername(query.getCreatedBy());
        dto.setCreator(userMapper.userToUserDto(creator));
        log.trace("persist query resulted in query {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }
}
