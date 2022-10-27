package at.tuwien.endpoint;

import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.entities.user.User;
import at.tuwien.mapper.UserMapper;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class StoreEndpoint extends AbstractEndpoint {

    private final UserMapper userMapper;
    private final QueryMapper queryMapper;
    private final UserService userService;
    private final StoreService storeService;

    @Autowired
    public StoreEndpoint(UserMapper userMapper, QueryMapper queryMapper,
                         UserService userService, StoreService storeService, DatabaseService databaseService,
                         IdentifierService identifierService, TableService tableService,
                         DatabaseAccessRepository databaseAccessRepository) {
        super(tableService, databaseService, identifierService, databaseAccessRepository);
        this.userMapper = userMapper;
        this.queryMapper = queryMapper;
        this.userService = userService;
        this.storeService = storeService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Find queries", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<QueryBriefDto>> findAll(@NotNull @PathVariable("id") Long containerId,
                                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                                       @RequestParam(value = "persisted", required = false) Boolean persisted,
                                                       Principal principal)
            throws QueryStoreException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, NotAllowedException,
            DatabaseConnectionException, TableMalformedException, UserNotFoundException {
        if (!hasDatabasePermission(containerId, databaseId, "QUERY_VIEW_ALL", principal)) {
            log.error("Missing view all queries permission");
            throw new NotAllowedException("Missing view all queries permission");
        }
        final List<Query> queries = storeService.findAll(containerId, databaseId, persisted, principal);
        final List<User> users = userService.findAll();
        final List<QueryBriefDto> out = queries.stream()
                .map(q -> {
                    final QueryBriefDto dto = queryMapper.queryToQueryBriefDto(q);
                    final Optional<User> optional = users.stream().filter(u -> u.getId().equals(q.getCreatedBy())).findFirst();
                    optional.ifPresent(user -> dto.setCreator(userMapper.userToUserDto(user)));
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{queryId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryDto> find(@NotNull @PathVariable("id") Long containerId,
                                         @NotNull @PathVariable("databaseId") Long databaseId,
                                         @NotNull @PathVariable Long queryId,
                                         Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            QueryStoreException, QueryNotFoundException, UserNotFoundException, NotAllowedException,
            DatabaseConnectionException {
        if (!hasQueryPermission(containerId, databaseId, queryId, "QUERY_VIEW", principal)) {
            log.error("Missing view query permission");
            throw new NotAllowedException("Missing view query permission");
        }
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        final QueryDto dto = queryMapper.queryToQueryDto(query);
        final User creator = userService.find(query.getCreatedBy());
        dto.setCreator(userMapper.userToUserDto(creator));
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{queryId}")
    @Operation(summary = "Persist some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryDto> persist(@NotNull @PathVariable("id") Long containerId,
                                            @NotNull @PathVariable("databaseId") Long databaseId,
                                            @NotNull @PathVariable("queryId") Long queryId,
                                            @NotNull Principal principal)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            NotAllowedException, DatabaseConnectionException, UserNotFoundException, QueryNotFoundException,
            QueryAlreadyPersistedException {
        if (!hasQueryPermission(containerId, databaseId, queryId, "QUERY_PERSIST", principal)) {
            log.error("Missing query persist permission");
            throw new NotAllowedException("Missing query persist permission");
        }
        final Query check = storeService.findOne(containerId, databaseId, queryId, principal);
        if (check.getIsPersisted()) {
            log.error("Failed to persist, is already persisted");
            throw new QueryAlreadyPersistedException("Failed to persist");
        }
        final Query query = storeService.persist(containerId, databaseId, queryId, principal);
        final QueryDto dto = queryMapper.queryToQueryDto(query);
        final User creator = userService.find(query.getCreatedBy());
        dto.setCreator(userMapper.userToUserDto(creator));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }
}
